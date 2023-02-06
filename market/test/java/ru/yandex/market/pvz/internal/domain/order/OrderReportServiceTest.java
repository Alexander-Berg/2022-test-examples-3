package ru.yandex.market.pvz.internal.domain.order;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.order.OrderPersonalCommandService;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentStatus;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.order.model.OrderType;
import ru.yandex.market.pvz.core.domain.order.model.personal.OrderPersonalRepository;
import ru.yandex.market.pvz.core.domain.order_delivery_result.PartialDeliveryStatus;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultCommandService;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRequestData;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.IncompleteOrderDto;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderReportDto;
import ru.yandex.market.pvz.internal.controller.pi.report.dto.OrderReportParamsDto;
import ru.yandex.market.tpl.common.personal.client.model.DataType;
import ru.yandex.market.tpl.common.personal.client.model.PersonalFindResponse;
import ru.yandex.market.tpl.common.personal.client.tpl.PersonalExternalService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.DISABLE_ORDER_SEARCH_BY_PERSONAL_DATA;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.INCOMPLETE_FASHION_ORDER_THRESHOLD;
import static ru.yandex.market.pvz.core.domain.dbqueue.get_recipient_phone_tail_batch.GetRecipientPhoneTailBatchService.PHONE_TAIL_LENGTH;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderReportServiceTest {

    private static final String DEFAULT_RECIPIENT_PHONE = "71234560099";
    private static final String DEFAULT_RECIPIENT_PHONE_ID = "11111111";

    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory orderFactory;

    private final TestableClock clock;
    private final EntityManager entityManager;

    private final OrderReportService orderReportService;
    private final OrderDeliveryResultQueryService orderDeliveryResultQueryService;
    private final OrderDeliveryResultCommandService orderDeliveryResultCommandService;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;
    private final OrderPersonalRepository orderPersonalRepository;
    private final OrderPersonalCommandService orderPersonalCommandService;

    @MockBean
    private PersonalExternalService personalExternalService;

    private PickupPoint pickupPoint;
    private PickupPointRequestData requestData;
    private Order cardPaidOrder;
    private Order onDemandOrder;
    private Order fashionOrder;
    private Order fashionOrder2;

    @BeforeEach
    void setUp() {
        pickupPoint = pickupPointFactory.createPickupPoint();
        requestData = new PickupPointRequestData(pickupPoint.getId(),
                pickupPoint.getPvzMarketId(), pickupPoint.getName(),
                1L, pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod());
        cardPaidOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(OrderPaymentType.CARD)
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .externalId("1")
                        .recipientPhone(DEFAULT_RECIPIENT_PHONE)
                        .personal(
                                TestOrderFactory.OrderPersonalParams.builder()
                                        .recipientPhoneId(DEFAULT_RECIPIENT_PHONE_ID)
                                        .build()
                        )
                        .build())
                .pickupPoint(pickupPoint)
                .build());

        Order cashPaidOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(OrderPaymentType.CASH)
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .externalId("2")
                        .build())
                .pickupPoint(pickupPoint)
                .build());

        Order arrivedOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(OrderPaymentType.PREPAID)
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .externalId("3")
                        .build())
                .pickupPoint(pickupPoint)
                .build());
        orderFactory.receiveOrder(arrivedOrder.getId());

        Order deliveredOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(OrderPaymentType.PREPAID)
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .externalId("4")
                        .build())
                .pickupPoint(pickupPoint)
                .build());
        orderFactory.receiveOrder(deliveredOrder.getId());
        orderFactory.deliverOrder(
                deliveredOrder.getId(),
                OrderDeliveryType.UNKNOWN,
                OrderPaymentType.PREPAID
        );

        onDemandOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .type(OrderType.ON_DEMAND)
                        .externalId("5")
                        .build())
                .pickupPoint(pickupPoint)
                .build());

        fashionOrder = orderFactory.createSimpleFashionOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(OrderPaymentType.CARD)
                        .externalId("6")
                        .fbs(false)
                        .build())
                .pickupPoint(pickupPoint)
                .build());

        orderFactory.receiveOrder(fashionOrder.getId());

        fashionOrder2 = orderFactory.createSimpleFashionOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(OrderPaymentType.CARD)
                        .externalId("7")
                        .fbs(false)
                        .build())
                .pickupPoint(pickupPoint)
                .build());
        orderFactory.receiveOrder(fashionOrder2.getId());
    }

    private Page<OrderReportDto> getOrders(OrderPaymentType paymentType) {
        return orderReportService.getOrders(
                requestData,
                OrderReportParamsDto.builder()
                        .resultPaymentType(paymentType.name())
                        .build(),
                Pageable.unpaged(), null
        );
    }

    @Test
    void filterByArrived() {
        LocalDate date = LocalDate.now(clock);
        OrderReportParamsDto paramsDto = OrderReportParamsDto.builder()
                .arrivedDateFrom(date)
                .arrivedDateTo(date)
                .resultPaymentType("CARD")
                .build();
        Page<OrderReportDto> orders = orderReportService.getOrders(requestData, paramsDto, Pageable.unpaged(), null);
        assertThat(orders.getContent()).hasSize(2);

        orderFactory.receiveOrder(cardPaidOrder.getId());

        orders = orderReportService.getOrders(requestData, paramsDto, Pageable.unpaged(), null);
        assertThat(orders.getContent()).hasSize(3);

        orders = orderReportService.getOrders(
                requestData,
                OrderReportParamsDto.builder()
                        .arrivedDateFrom(date.plusDays(1))
                        .arrivedDateTo(date.plusDays(1))
                        .resultPaymentType("CARD")
                        .build(),
                Pageable.unpaged(), null
        );
        assertThat(orders.getContent()).hasSize(0);
    }

    @Test
    void filterByDelivered() {
        LocalDate date = LocalDate.now(clock);
        OrderReportParamsDto paramsDto = OrderReportParamsDto.builder()
                .arrivedDateFrom(date)
                .arrivedDateTo(date)
                .deliveredDateFrom(date)
                .deliveredDateTo(date)
                .build();
        Page<OrderReportDto> orders = orderReportService.getOrders(requestData, paramsDto, Pageable.unpaged(), null);
        int size = orders.getContent().size();

        orderFactory.receiveOrder(cardPaidOrder.getId());
        orderFactory.deliverOrder(cardPaidOrder.getId(), OrderDeliveryType.UNKNOWN, OrderPaymentType.CARD);

        orders = orderReportService.getOrders(requestData, paramsDto, Pageable.unpaged(), null);
        assertThat(orders.getContent()).hasSize(size + 1);
    }

    @Test
    void filterByOnDemand() {
        OrderReportParamsDto paramsDto = OrderReportParamsDto.builder()
                .types("ON_DEMAND")
                .build();
        Page<OrderReportDto> orders = orderReportService.getOrders(requestData, paramsDto, Pageable.unpaged(), null);

        assertThat(orders.getContent()).hasSize(1);
        var order = orders.getContent().get(0);
        assertThat(order.getType()).isEqualTo(OrderType.ON_DEMAND);
        assertThat(order.getExternalId()).isEqualTo(onDemandOrder.getExternalId());
    }

    @Test
    void filterByNotStarted() {
        OrderReportParamsDto paramsDto = OrderReportParamsDto.builder()
                .partialDeliveryStatuses("NOT_STARTED")
                .build();

        orderDeliveryResultCommandService.startFitting(fashionOrder2.getId());

        Page<OrderReportDto> orders = orderReportService.getOrders(requestData, paramsDto, Pageable.unpaged(), null);

        assertThat(orders.getContent()).hasSize(1);
        var order = orders.getContent().get(0);
        assertThat(order.getExternalId()).isEqualTo(fashionOrder.getExternalId());
        assertThat(orderDeliveryResultQueryService.get(order.getId()).getStatus())
                .isEqualTo(PartialDeliveryStatus.NOT_STARTED);
    }

    @Test
    void filterByNotStartedAndCreated() {
        OrderReportParamsDto paramsDto = OrderReportParamsDto.builder()
                .partialDeliveryStatuses("NOT_STARTED,CREATED")
                .build();

        orderDeliveryResultCommandService.startFitting(fashionOrder2.getId());

        var orders = orderReportService.getOrders(requestData, paramsDto, Pageable.unpaged(), null).getContent();

        assertThat(orders).hasSize(2);
        assertThat(orders.stream().map(OrderReportDto::getExternalId)).containsExactlyInAnyOrderElementsOf(
                List.of(fashionOrder.getExternalId(), fashionOrder2.getExternalId()));
        assertThat(List.of(orderDeliveryResultQueryService.get(fashionOrder.getId()).getStatus(),
                orderDeliveryResultQueryService.get(fashionOrder2.getId()).getStatus()))
                .containsExactlyInAnyOrderElementsOf(
                        List.of(PartialDeliveryStatus.NOT_STARTED, PartialDeliveryStatus.CREATED));
    }

    @Test
    void filterByCreated() {
        OrderReportParamsDto paramsDto = OrderReportParamsDto.builder()
                .partialDeliveryStatuses("CREATED")
                .build();

        orderDeliveryResultCommandService.startFitting(fashionOrder2.getId());

        Page<OrderReportDto> orders = orderReportService.getOrders(requestData, paramsDto, Pageable.unpaged(), null);

        assertThat(orders.getContent()).hasSize(1);
        var order = orders.getContent().get(0);
        assertThat(order.getExternalId()).isEqualTo(fashionOrder2.getExternalId());
        assertThat(orderDeliveryResultQueryService.get(order.getId()).getStatus())
                .isEqualTo(PartialDeliveryStatus.CREATED);
    }

    @Test
    @SneakyThrows
    void downloadOrders() {
        byte[] actual = orderReportService.downloadOrders(
                requestData,
                OrderReportParamsDto.builder().build(),
                Sort.by("id")
        ).readAllBytes();
        assertThat(actual.length).isGreaterThan(0);
    }

    @Test
    void testSortIncompleteOrdersFirst() {
        orderDeliveryResultCommandService.startFitting(fashionOrder.getId());

        OrderReportParamsDto paramsDto = OrderReportParamsDto.builder().build();

        PageRequest pageRequest = PageRequest.of(0, 8, Sort.by("id"));
        Page<OrderReportDto> orders = orderReportService.getOrders(requestData, paramsDto, pageRequest, null);

        assertThat(orders.getContent().size()).isGreaterThan(1);
        assertThat(orders.getContent().get(0).getExternalId()).isEqualTo(fashionOrder.getExternalId());
    }

    @Test
    void testFilterIncompleteOrders() {
        clock.clearFixed();
        orderDeliveryResultCommandService.startFitting(fashionOrder.getId());
        Instant fittingStarted = Instant.now(clock);
        configurationGlobalCommandService.setValue(INCOMPLETE_FASHION_ORDER_THRESHOLD, 15);

        clock.setFixed(fittingStarted.plus(14, ChronoUnit.MINUTES), ZoneOffset.ofHours(pickupPoint.getTimeOffset()));
        List<IncompleteOrderDto> orderIds = orderReportService.getIncompleteFashionOrders(requestData);
        assertThat(orderIds).isEmpty();

        clock.setFixed(fittingStarted.plus(16, ChronoUnit.MINUTES), ZoneOffset.ofHours(pickupPoint.getTimeOffset()));
        orderIds = orderReportService.getIncompleteFashionOrders(requestData);

        assertThat(orderIds.size()).isEqualTo(1);
        assertThat(orderIds.get(0).getExternalId()).isEqualTo(fashionOrder.getExternalId());
        assertThat(orderIds.get(0).getId()).isEqualTo(fashionOrder.getId());
    }

    @Test
    void filterByPlaceCodes() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .places(List.of(
                                TestOrderFactory.OrderPlaceParams.builder()
                                        .barcode("SOME_PLACE_ID_1")
                                        .build(),
                                TestOrderFactory.OrderPlaceParams.builder()
                                        .barcode("SOME_PLACE_ID_2")
                                        .build()
                        ))
                        .build())
                .build());

        OrderReportParamsDto paramsDto = OrderReportParamsDto.builder()
                .commonQuery("some_place")
                .build();

        Page<OrderReportDto> orders = orderReportService.getOrders(requestData, paramsDto, Pageable.unpaged(), null);

        assertThat(orders.getContent()).hasSize(1);
        var orderFound = orders.getContent().get(0);
        assertThat(orderFound.getId()).isEqualTo(order.getId());
    }

    @Test
    void filterByRecipientPhoneWithoutPersonal() {
        configurationGlobalCommandService.setValue(DISABLE_ORDER_SEARCH_BY_PERSONAL_DATA, false);

        OrderReportParamsDto paramsDto = OrderReportParamsDto.builder().commonQuery(DEFAULT_RECIPIENT_PHONE).build();
        Page<OrderReportDto> orders = orderReportService.getOrders(requestData, paramsDto, Pageable.unpaged(), false);
        assertThat(orders.getContent()).hasSize(1);
        assertThat(orders.getContent().get(0).getExternalId()).isEqualTo(cardPaidOrder.getExternalId());

        configurationGlobalCommandService.setValue(DISABLE_ORDER_SEARCH_BY_PERSONAL_DATA, true);

        orders = orderReportService.getOrders(requestData, paramsDto, Pageable.unpaged(), false);
        assertThat(orders.getContent()).hasSize(0);
    }

    @Test
    void filterByExternalIdWithPersonal() {
        configurationGlobalCommandService.setValue(DISABLE_ORDER_SEARCH_BY_PERSONAL_DATA, true);

        OrderReportParamsDto paramsDto = OrderReportParamsDto.builder().commonQuery("1").build();
        Page<OrderReportDto> orders = orderReportService.getOrders(requestData, paramsDto, Pageable.unpaged(), true);
        assertThat(orders.getContent()).hasSize(1);
        assertThat(orders.getContent().get(0).getExternalId()).isEqualTo(cardPaidOrder.getExternalId());
    }

    @Test
    void filterByPhoneTailWithPersonal() {
        configurationGlobalCommandService.setValue(DISABLE_ORDER_SEARCH_BY_PERSONAL_DATA, true);

        var createdPersonal = orderPersonalRepository.findActiveByOrderId(cardPaidOrder.getId());
        String phoneTail = DEFAULT_RECIPIENT_PHONE.substring(DEFAULT_RECIPIENT_PHONE.length() - PHONE_TAIL_LENGTH);
        orderPersonalCommandService.updatePhoneTail(createdPersonal.get().getId(), phoneTail);

        checkPhoneTail(3);
        checkPhoneTail(4);
    }

    private void checkPhoneTail(int length) {
        OrderReportParamsDto paramsDto = OrderReportParamsDto.builder().commonQuery(
                DEFAULT_RECIPIENT_PHONE.substring(DEFAULT_RECIPIENT_PHONE.length() - length)
        ).build();
        Page<OrderReportDto> orders = orderReportService.getOrders(requestData, paramsDto, Pageable.unpaged(), true);
        assertThat(orders.getContent()).hasSize(1);
        assertThat(orders.getContent().get(0).getExternalId()).isEqualTo(cardPaidOrder.getExternalId());
    }

    @Test
    void filterByPhoneWithPersonal() {
        configurationGlobalCommandService.setValue(DISABLE_ORDER_SEARCH_BY_PERSONAL_DATA, true);

        var createdPersonal = orderPersonalRepository.findActiveByOrderId(cardPaidOrder.getId());
        when(personalExternalService.getIdByPersonal(DataType.PHONES, ("+" + DEFAULT_RECIPIENT_PHONE)))
                .thenReturn(Optional.of(new PersonalFindResponse().id(createdPersonal.get().getRecipientPhoneId())));

        OrderReportParamsDto paramsDto = OrderReportParamsDto.builder().commonQuery(DEFAULT_RECIPIENT_PHONE).build();
        Page<OrderReportDto> orders = orderReportService.getOrders(requestData, paramsDto, Pageable.unpaged(), true);
        assertThat(orders.getContent()).hasSize(1);
        assertThat(orders.getContent().get(0).getExternalId()).isEqualTo(cardPaidOrder.getExternalId());
    }

    @Test
    void filterByInvalidDataWithPersonal() {
        configurationGlobalCommandService.setValue(DISABLE_ORDER_SEARCH_BY_PERSONAL_DATA, true);

        OrderReportParamsDto paramsDto = OrderReportParamsDto.builder().commonQuery("aaaaa").build();
        Page<OrderReportDto> orders = orderReportService.getOrders(requestData, paramsDto, Pageable.unpaged(), true);
        assertThat(orders.getContent()).hasSize(0);
    }

    @Test
    void filterWithoutCommonQueryWithPersonal() {
        configurationGlobalCommandService.setValue(DISABLE_ORDER_SEARCH_BY_PERSONAL_DATA, true);

        OrderReportParamsDto paramsDto = OrderReportParamsDto.builder().statuses("ARRIVED_TO_PICKUP_POINT").build();
        Page<OrderReportDto> orders = orderReportService.getOrders(requestData, paramsDto, Pageable.unpaged(), true);
        assertThat(orders.getContent()).hasSize(3);
    }

}
