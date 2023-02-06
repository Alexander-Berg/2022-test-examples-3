package ru.yandex.market.pvz.internal.controller.pi.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistic.api.model.delivery.CargoType;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.logs.pickup_point_scan.LogPickupPointScanRepository;
import ru.yandex.market.pvz.core.domain.logs.pickup_point_scan.model.LogPickupPointScan;
import ru.yandex.market.pvz.core.domain.logs.pickup_point_scan.model.LogPickupPointScanDetails;
import ru.yandex.market.pvz.core.domain.logs.pickup_point_scan.model.LogPickupPointScanType;
import ru.yandex.market.pvz.core.domain.order.OrderQueryService;
import ru.yandex.market.pvz.core.domain.order.OrderRepository;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderLabel;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.order.model.OrderType;
import ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus;
import ru.yandex.market.pvz.core.domain.order.model.VatType;
import ru.yandex.market.pvz.core.domain.order.model.history.OrderHistoryRecord;
import ru.yandex.market.pvz.core.domain.order.model.history.OrderHistoryRepository;
import ru.yandex.market.pvz.core.domain.order.model.params.OrderSimpleParams;
import ru.yandex.market.pvz.core.domain.order_delivery_result.ItemDeliveryFlow;
import ru.yandex.market.pvz.core.domain.order_delivery_result.PartialDeliveryStatus;
import ru.yandex.market.pvz.core.domain.order_delivery_result.params.OrderDeliveryResultParams;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultCommandService;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRequestData;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.CreatePickupPointBuilder;
import ru.yandex.market.pvz.internal.PvzIntTest;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.ItemPrintFormDto;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.ItemProductType;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionDto;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderDeliverDto;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderDto;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderHistoryRecordDto;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderItemDto;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderPageDto;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderPrintFormDto;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderSenderDto;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderSource;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.Recipient;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.SmsDto;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.VerificationCodeDto;
import ru.yandex.market.pvz.internal.domain.order.OrderPartnerService;
import ru.yandex.market.sc.internal.client.ScLogisticsClient;
import ru.yandex.market.sc.internal.model.InventoryItemDto;
import ru.yandex.market.sc.internal.model.InventoryItemPlaceDto;
import ru.yandex.market.sc.internal.model.InventoryItemPlaceStatus;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.personal.client.model.CommonType;
import ru.yandex.market.tpl.common.personal.client.model.CommonTypeEnum;
import ru.yandex.market.tpl.common.personal.client.model.FullName;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeRetrieveResponseItem;
import ru.yandex.market.tpl.common.personal.client.tpl.PersonalExternalService;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.DISABLE_ORDER_SEARCH_BY_PERSONAL_DATA;
import static ru.yandex.market.pvz.core.domain.order.model.OrderBarcodeFilterType.EXTERNAL_ID;
import static ru.yandex.market.pvz.core.domain.order.model.OrderBarcodeFilterType.PLACE_BARCODE;
import static ru.yandex.market.pvz.core.domain.order.model.OrderVerification.DEFAULT_ORDER_VERIFICATION_CODE_LIMIT;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.ARRIVED_TO_PICKUP_POINT;
import static ru.yandex.market.pvz.core.domain.yandex.YandexMigrationManager.YANDEX_COURIER;
import static ru.yandex.market.pvz.core.domain.yandex.YandexMigrationManager.YANDEX_SENDER_ID;
import static ru.yandex.market.pvz.core.test.TestExternalConfiguration.DEFAULT_UID;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderItemParams.DEFAULT_PRICE;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderItemParams.DEFAULT_SUPPLIER_TAXPAYER_NUMBER;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderItemParams.DEFAULT_VAT_TYPE;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_DIMENSIONS;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_PAYMENT_STATUS;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_PAYMENT_TYPE;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_PLACES;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_RECIPIENT_EMAIL;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_RECIPIENT_NAME;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_RECIPIENT_NOTES;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_RECIPIENT_PHONE;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_VERIFICATION_CODE;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_1_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.UIT_2_1;
import static ru.yandex.market.pvz.core.test.factory.TestOrderSenderFactory.OrderSenderParams.DEFAULT_INCORPORATION;
import static ru.yandex.market.pvz.core.test.factory.TestOrderSenderFactory.OrderSenderParams.DEFAULT_PHONE;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.CASHBOX_PAYMENT;
import static ru.yandex.market.pvz.internal.controller.pi.order.mapper.OrderDtoMapper.DEFAULT_SMS_VERIFICATION_CODE_DELAY;
import static ru.yandex.market.pvz.internal.controller.pi.order.mapper.OrderDtoMapper.SINGLE_PLACE_ORDERS_COUNT;

@PvzIntTest
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderPartnerServiceTest {

    private static final int SQL_DECIMAL_PLACES_COUNT = 2;

    private static final String CIS_1_1 = "CIS_1_1";
    private static final String CIS_2_1 = "CIS_2_1";
    private static final String CIS_2_2 = "CIS_2_2";

    private static final String CIS_FULL_POSTFIX = "-full";

    private final TestableClock clock;
    private final TransactionTemplate transactionTemplate;

    private final TestOrderFactory orderFactory;
    private final TestPickupPointFactory pickupPointFactory;

    private final OrderRepository orderRepository;
    private final OrderQueryService orderQueryService;
    private final OrderHistoryRepository orderHistoryRepository;

    private final OrderPartnerService orderPartnerService;
    private final OrderDeliveryResultQueryService orderDeliveryResultQueryService;
    private final OrderDeliveryResultCommandService orderDeliveryResultCommandService;
    private final LogPickupPointScanRepository logPickupPointScanRepository;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    @MockBean
    private ScLogisticsClient scLogisticsClientImpl;

    @MockBean
    private PersonalExternalService personalExternalService;

    @Test
    void successDeliver() {
        configurationGlobalCommandService.setValue(DISABLE_ORDER_SEARCH_BY_PERSONAL_DATA, true);

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(
                        TestOrderFactory.OrderParams.builder()
                                .recipientPhone("12345")
                                .recipientEmail("email")
                                .recipientName("Иванов Иван Иванович")
                                .build()
                )
                .build());
        orderFactory.receiveOrder(order.getId());

        String phone = "+71112223344";
        String email = "some@mail.ru";
        String forename = "Василий";
        String surname = "Пупкин";
        mockPersonal(phone, email, forename, surname);

        OrderDto orderDto = orderPartnerService.deliver(order.getId(),
                new OrderDeliverDto(null, null),
                new PickupPointRequestData(order.getPickupPoint().getId(), 1L, "Пункт выдачи Беру", 1L, 3, 7));

        Recipient recipient = orderDto.getRecipient();
        assertThat(recipient.getPhone()).isEqualTo(phone);
        assertThat(recipient.getEmail()).isEqualTo(email);
        assertThat(recipient.getName()).isEqualTo(surname + " " + forename);

        Order delivered = orderRepository.findByIdOrThrow(order.getId());

        assertThat(delivered.getStatus()).isEqualTo(PvzOrderStatus.TRANSMITTED_TO_RECIPIENT);
    }

    private void mockPersonal(String phone, String email, String forename, String surname) {
        List<MultiTypeRetrieveResponseItem> responseItems = List.of(
                new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.PHONE).id("1234")
                        .value(new CommonType().phone(phone)),
                new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.EMAIL).id("4321")
                        .value(new CommonType().email(email)),
                new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.FULL_NAME).id("5678")
                        .value(new CommonType().fullName(new FullName().forename(forename).surname(surname)))
        );
        when(personalExternalService.getMultiTypePersonalByIds(any())).thenReturn(responseItems);
    }

    @Test
    void whenDeliverOrderWithFittingThenError() {
        configurationGlobalCommandService.setValue(DISABLE_ORDER_SEARCH_BY_PERSONAL_DATA, true);
        mockPersonal("+71112223344", "some@mail.ru", "Василий", "Пупкин");

        Order order = orderFactory.createSimpleFashionOrder(false);
        orderFactory.receiveOrder(order.getId());
        orderDeliveryResultCommandService.startFitting(order.getId());

        PickupPointRequestData pickupPointRequestData = new PickupPointRequestData(
                order.getPickupPoint().getId(), 1L, "Пункт выдачи Беру", 1L, 3, 7
        );
        assertThatThrownBy(() -> orderPartnerService.deliver(order.getId(),
                new OrderDeliverDto(null, null), pickupPointRequestData))
                .isExactlyInstanceOf(TplIllegalStateException.class);

        OrderSimpleParams orderSimpleParams = orderQueryService.getSimple(order.getId());
        assertThat(orderSimpleParams.getStatus()).isEqualTo(ARRIVED_TO_PICKUP_POINT);
    }

    @Test
    void invalidPickupPoint() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());
        assertThatThrownBy(() -> orderPartnerService.deliver(order.getId(),
                new OrderDeliverDto(OrderDeliveryType.VERIFICATION_CODE, null),
                new PickupPointRequestData(99L, 1L, "Пункт выдачи Беру", 1L, 3, 7)))
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void tryToDeliverOrderWithUnknownPaymentType() {
        assertThatThrownBy(() -> orderPartnerService.deliver(1L,
                new OrderDeliverDto(OrderDeliveryType.VERIFICATION_CODE, OrderPaymentType.UNKNOWN),
                new PickupPointRequestData(99L, 1L, "Пункт выдачи Беру", 1L, 3, 7)))
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void getOrderPageById() {
        configurationGlobalCommandService.setValue(DISABLE_ORDER_SEARCH_BY_PERSONAL_DATA, true);

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());
        orderFactory.cancelOrder(order.getId());

        OrderPageDto page = orderPartnerService.getOrderPage(
                order.getId(),
                new PickupPointRequestData(
                        order.getPickupPoint().getId(),
                        order.getPickupPoint().getPvzMarketId(),
                        order.getPickupPoint().getName(),
                        1L,
                        order.getPickupPoint().getTimeOffset(),
                        order.getPickupPoint().getStoragePeriod()
                ));

        assertThat(page.getOrder().getExternalId()).isEqualTo(order.getExternalId());
        assertThat(page.getHistory().size()).isEqualTo(2);
        List<PvzOrderStatus> statuses = getOrderStatuses(page);
        assertThat(statuses).containsExactlyInAnyOrder(PvzOrderStatus.CREATED, PvzOrderStatus.CANCELLED);
    }

    @Test
    void getOrderPageByIdWithPersonal() {
        configurationGlobalCommandService.setValue(DISABLE_ORDER_SEARCH_BY_PERSONAL_DATA, true);

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(
                        TestOrderFactory.OrderParams.builder()
                                .recipientPhone("12345")
                                .recipientEmail("email")
                                .recipientName("Иванов Иван Иванович")
                                .build()
                )
                .build());
        orderFactory.receiveOrder(order.getId());

        String phoneNew = "+71112223344";
        String emailNew = "some@mail.ru";
        String forename = "Василий";
        String surname = "Пупкин";
        List<MultiTypeRetrieveResponseItem> responseItems = List.of(
                new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.PHONE).id("1234")
                        .value(new CommonType().phone(phoneNew)),
                new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.EMAIL).id("4321")
                        .value(new CommonType().email(emailNew)),
                new MultiTypeRetrieveResponseItem().type(CommonTypeEnum.FULL_NAME).id("5678")
                        .value(new CommonType().fullName(new FullName().forename(forename).surname(surname)))
        );
        when(personalExternalService.getMultiTypePersonalByIds(any())).thenReturn(responseItems);

        OrderPageDto page = orderPartnerService.getOrderPage(
                order.getId(),
                new PickupPointRequestData(
                        order.getPickupPoint().getId(),
                        order.getPickupPoint().getPvzMarketId(),
                        order.getPickupPoint().getName(),
                        1L,
                        order.getPickupPoint().getTimeOffset(),
                        order.getPickupPoint().getStoragePeriod()
                ));

        OrderDto orderDto = page.getOrder();
        assertThat(orderDto.getExternalId()).isEqualTo(order.getExternalId());

        Recipient recipient = orderDto.getRecipient();
        assertThat(recipient.getPhone()).isEqualTo(phoneNew);
        assertThat(recipient.getEmail()).isEqualTo(emailNew);
        assertThat(recipient.getName()).isEqualTo(surname + " " + forename);

        assertThat(page.getHistory().size()).isEqualTo(2);
        List<PvzOrderStatus> statuses = getOrderStatuses(page);
        assertThat(statuses).containsExactlyInAnyOrder(PvzOrderStatus.CREATED, ARRIVED_TO_PICKUP_POINT);
    }

    @Test
    void getOnDemandOrderPageById() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .type(OrderType.ON_DEMAND)
                        .build())
                .build());
        orderFactory.setStatusAndCheckpoint(order.getId(), PvzOrderStatus.CANCELLED);

        OrderPageDto page = orderPartnerService.getOrderPage(
                order.getId(),
                new PickupPointRequestData(
                        order.getPickupPoint().getId(),
                        order.getPickupPoint().getPvzMarketId(),
                        order.getPickupPoint().getName(),
                        1L,
                        order.getPickupPoint().getTimeOffset(),
                        order.getPickupPoint().getStoragePeriod()
                ));

        assertThat(page.getOrder().getExternalId()).isEqualTo(order.getExternalId());
        assertThat(page.getOrder().getRecipient().getName()).isEqualTo("");
        assertThat(page.getOrder().getRecipient().getPhone()).isEqualTo("");
        assertThat(page.getOrder().getRecipient().getEmail()).isEqualTo("");
        assertThat(page.getOrder().getRecipient().getNote()).isEqualTo("");
        assertThat(page.getHistory().size()).isEqualTo(2);

        List<PvzOrderStatus> statuses = getOrderStatuses(page);
        assertThat(statuses).containsExactlyInAnyOrder(PvzOrderStatus.CREATED, PvzOrderStatus.CANCELLED);
    }

    private List<PvzOrderStatus> getOrderStatuses(OrderPageDto page) {
        return page.getHistory().stream()
                .map(OrderHistoryRecordDto::getStatus)
                .collect(Collectors.toList());
    }

    @Test
    void notFoundByOrderId() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());

        long id = order.getId();
        PickupPoint pickupPoint = order.getPickupPoint();

        assertThatThrownBy(() -> orderPartnerService.getOrderPage(
                id + 1,
                new PickupPointRequestData(pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), 1L,
                        pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod())
        )).isExactlyInstanceOf(TplEntityNotFoundException.class);
    }

    @Test
    void invalidPickupPointByOrderId() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());

        long id = order.getId();
        PickupPoint pickupPoint = order.getPickupPoint();

        assertThatThrownBy(() -> orderPartnerService.getOrderPage(
                id,
                new PickupPointRequestData(pickupPoint.getId() + 1, pickupPoint.getPvzMarketId(), pickupPoint.getName(),
                        1L, pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod())
        )).isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void getOrderByExternalId() {
        clock.setFixed(Instant.EPOCH, ZoneId.systemDefault());

        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .deliveryDate(LocalDate.ofInstant(clock.instant(), DateTimeUtils.MOSCOW_ZONE))
                        .items(List.of(
                                TestOrderFactory.OrderItemParams.builder()
                                        .name("Чипсы")
                                        .count(1)
                                        .cisFullValues(List.of(CIS_1_1 + CIS_FULL_POSTFIX))
                                        .build(),
                                TestOrderFactory.OrderItemParams.builder()
                                        .name("Пиво")
                                        .count(1)
                                        .cisFullValues(List.of(CIS_2_1 + CIS_FULL_POSTFIX))
                                        .build()
                        ))
                        .build())
                .build());

        final long orderId = order.getId();
        order = transactionTemplate.execute(s -> {
            Order o = orderFactory.receiveOrder(orderId);

            // preload hibernate entities
            o.getPickupPoint().getPvzMarketId();
            o.getOrderAdditionalInfo().getDeliveryServiceType();
            o.getBoughtTotalPrice();

            return o;
        });

        List<OrderHistoryRecord> history = orderHistoryRepository.getOrderHistory(order.getId());
        assertThat(history).hasSize(2);

        OrderPageDto actual = orderPartnerService.getOrderPage(
                order.getExternalId(),
                new PickupPointRequestData(
                        order.getPickupPoint().getId(),
                        order.getPickupPoint().getPvzMarketId(),
                        order.getPickupPoint().getName(),
                        1L,
                        order.getPickupPoint().getTimeOffset(),
                        order.getPickupPoint().getStoragePeriod()
                ));

        var expected = OrderPageDto.builder()
                .order(OrderDto.builder()
                        .id(order.getId())
                        .externalId(order.getExternalId())
                        .type(OrderType.CLIENT)
                        .pickupPointId(order.getPickupPoint().getId())
                        .pvzMarketId(order.getPickupPoint().getPvzMarketId())
                        .recipient(Recipient.builder()
                                .name(DEFAULT_RECIPIENT_NAME)
                                .phone(DEFAULT_RECIPIENT_PHONE)
                                .email(DEFAULT_RECIPIENT_EMAIL)
                                .note(DEFAULT_RECIPIENT_NOTES)
                                .buyerYandexUid(order.getBuyerYandexUid().toString())
                                .build())
                        .deliveryDate(LocalDate.ofInstant(clock.instant(), clock.getZone()))
                        .creationDateTime(order.getCreatedAt())
                        .arrivalDateTime(OffsetDateTime.ofInstant(clock.instant(), ZoneId.systemDefault()))
                        .expirationDate(LocalDate.ofInstant(
                                clock.instant(), clock.getZone()).plusDays(order.getPickupPoint().getStoragePeriod()))
                        .expirationDateTime(DateTimeUtil.atStartOfDay(LocalDate.ofInstant(
                                clock.instant(), clock.getZone()).plusDays(order.getPickupPoint().getStoragePeriod())))
                        .status(ARRIVED_TO_PICKUP_POINT)
                        .dsApiCheckpoint(ARRIVED_TO_PICKUP_POINT.getCode())
                        .totalPrice(order.getTotalPrice())
                        .paymentType(DEFAULT_PAYMENT_TYPE)
                        .prepaid(DEFAULT_PAYMENT_TYPE.isPrepaid())
                        .paymentStatus(DEFAULT_PAYMENT_STATUS)
                        .dimensions(DEFAULT_DIMENSIONS)
                        .items(List.of(
                                buildItemDto("Пиво", CIS_2_1 + CIS_FULL_POSTFIX),
                                buildItemDto("Чипсы", CIS_1_1 + CIS_FULL_POSTFIX)
                        ))
                        .placesCount(DEFAULT_PLACES.size())
                        .placeBarcodes(DEFAULT_PLACES.stream()
                                .map(TestOrderFactory.OrderPlaceParams::getBarcode).collect(Collectors.toList()))
                        .maxStorageExtensionDate(order.getExpirationDate()
                                .plusDays(Order.DEFAULT_EXTEND_STORAGE_PERIOD_MAX_DAYS))
                        .actions(List.of(
                                new OrderActionDto(OrderActionType.DELIVER),
                                new OrderActionDto(OrderActionType.EXTEND_STORAGE_PERIOD),
                                new OrderActionDto(OrderActionType.DO_NOT_USE_VERIFICATION_CODE)))
                        .senderName(DEFAULT_INCORPORATION)
                        .senderPhone(DEFAULT_PHONE)
                        .verificationCode(VerificationCodeDto.builder()
                                .accepted(false)
                                .attemptsLeftToVerify(DEFAULT_ORDER_VERIFICATION_CODE_LIMIT)
                                .sms(SmsDto.builder()
                                        .delayInSeconds(DEFAULT_SMS_VERIFICATION_CODE_DELAY)
                                        .build())
                                .build())
                        .assessedCost(order.getAssessedCost())
                        .boughtItemsCost(order.getBoughtTotalPrice())
                        .labels(List.of())
                        .siblings(List.of())
                        .deliveryServiceType(order.getOrderAdditionalInfo().getDeliveryServiceType())
                        .sender(OrderSenderDto.builder()
                                .id(YANDEX_SENDER_ID)
                                .name(YANDEX_COURIER)
                                .build())
                        .build())
                .history(List.of(
                        OrderHistoryRecordDto.builder()
                                .id(history.get(0).getId())
                                .orderId(order.getId())
                                .externalOrderId(order.getExternalId())
                                .status(PvzOrderStatus.CREATED)
                                .dsApiCheckpoint(PvzOrderStatus.CREATED.getCode())
                                .eventDateTime(clock.instant())
                                .build(),
                        OrderHistoryRecordDto.builder()
                                .id(history.get(1).getId())
                                .orderId(order.getId())
                                .externalOrderId(order.getExternalId())
                                .status(ARRIVED_TO_PICKUP_POINT)
                                .dsApiCheckpoint(ARRIVED_TO_PICKUP_POINT.getCode())
                                .eventDateTime(clock.instant())
                                .build()
                ))
                .build();
        assertThat(actual).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected);
    }

    private OrderItemDto buildItemDto(String name, String cis) {
        return OrderItemDto.builder()
                .name(name)
                .count(1)
                .price(DEFAULT_PRICE.setScale(SQL_DECIMAL_PLACES_COUNT, RoundingMode.HALF_UP))
                .sumPrice(DEFAULT_PRICE.setScale(SQL_DECIMAL_PLACES_COUNT, RoundingMode.HALF_UP))
                .vatType(DEFAULT_VAT_TYPE)
                .cisValues(List.of(cis))
                .supplierTaxpayerNumber(DEFAULT_SUPPLIER_TAXPAYER_NUMBER)
                .supplierName(TestOrderFactory.OrderItemParams.DEFAULT_SUPPLIER_NAME)
                .supplierPhone(TestOrderFactory.OrderItemParams.DEFAULT_SUPPLIER_PHONE)
                .build();
    }

    @Test
    void testGetOrderWithoutPlaces() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .places(List.of())
                        .build())
                .build());
        orderFactory.setStatusAndCheckpoint(order.getId(), PvzOrderStatus.TRANSMITTED_TO_RECIPIENT);

        OrderPageDto page = orderPartnerService.getOrderPage(
                order.getExternalId(),
                new PickupPointRequestData(
                        order.getPickupPoint().getId(),
                        order.getPickupPoint().getPvzMarketId(),
                        order.getPickupPoint().getName(),
                        1L,
                        order.getPickupPoint().getTimeOffset(),
                        order.getPickupPoint().getStoragePeriod()
                ));

        assertThat(page.getOrder().getPlacesCount()).isEqualTo(SINGLE_PLACE_ORDERS_COUNT);
        assertThat(page.getOrder().getPlaceBarcodes()).isEmpty();
    }

    @Test
    void notFoundByOrderExternalId() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());

        String externalId = order.getExternalId();
        PickupPoint pickupPoint = order.getPickupPoint();

        assertThatThrownBy(() -> orderPartnerService.getOrderPage(
                externalId + "-fake",
                new PickupPointRequestData(pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(),
                        1L, pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod())
        )).isExactlyInstanceOf(TplEntityNotFoundException.class);
    }

    @Test
    void invalidPickupPointByOrderExternalId() {
        Order order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().build());

        String externalId = order.getExternalId();
        PickupPoint pickupPoint = order.getPickupPoint();

        assertThatThrownBy(() -> orderPartnerService.getOrderPage(
                externalId,
                new PickupPointRequestData(pickupPoint.getId() + 1, pickupPoint.getPvzMarketId(), pickupPoint.getName(),
                        1L, pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod())
        )).isExactlyInstanceOf(TplEntityNotFoundException.class);
    }

    @ParameterizedTest(name = "orderItemsWithCargoType_{index}")
    @MethodSource("orderItemsWithCargoTypeMethodSource")
    void orderItemsWithCargoTypeTest(List<Integer> cargoTypes, boolean expectedPassportRequired) {
        var order = createOrderWithCargoTypes(cargoTypes);
        var page = getPageByOrder(order);

        assertThat(page.getOrder().getExternalId()).isEqualTo(order.getExternalId());
        assertThat(page.getOrder().isPassportRequired()).isEqualTo(expectedPassportRequired);
    }

    private Order createOrderWithCargoTypes(List<Integer> cargoTypes) {
        return orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder().params(
                TestOrderFactory.OrderParams.builder().items(
                        List.of(TestOrderFactory.OrderItemParams.builder().cargoTypeCodes(cargoTypes).build())
                ).build()).build());
    }

    private OrderPageDto getPageByOrder(Order order) {
        return orderPartnerService.getOrderPage(order.getId(),
                new PickupPointRequestData(order.getPickupPoint().getId(), order.getPickupPoint().getPvzMarketId(),
                        order.getPickupPoint().getName(), 1L, order.getPickupPoint().getTimeOffset(),
                        order.getPickupPoint().getStoragePeriod())
        );
    }

    static Stream<Arguments> orderItemsWithCargoTypeMethodSource() {
        return Stream.of(
                Arguments.of(List.of(CargoType.R18.getCode(), CargoType.CIS_REQUIRED.getCode()), true),
                Arguments.of(List.of(CargoType.BULKY_CARGO.getCode(), CargoType.CIS_REQUIRED.getCode()), false),
                Arguments.of(List.of(), false)
        );
    }

    @Test
    void testRegularOrderForm() {
        Order order = createOrder(false);
        OrderPrintFormDto form = orderPartnerService.form(order.getId(),
                new PickupPointRequestData(order.getPickupPoint().getId(), null, null, null, 3, 7));

        assertThat(form).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(OrderPrintFormDto.builder()
                .externalId(order.getExternalId())
                .deliveryDate(null)
                .amount(3)
                .orderSender(order.getSender().getIncorporation())
                .recipientName(order.getRecipientName())
                .totalVat(toRub(1818))
                .totalSum(toRub(10100))
                .totalSumWithoutSale(toRub(10100))
                .items(List.of(
                        ItemPrintFormDto.builder()
                                .number(1)
                                .name("Футболка")
                                .amount(1)
                                .vatType(VatType.VAT_18)
                                .price(toRub(2000))
                                .sum(toRub(2000))
                                .sumWithoutSale(toRub(2000))
                                .cisValues(List.of(CIS_1_1))
                                .supplierTaxpayerNumber("item_1_sup")
                                .supplierName(TestOrderFactory.OrderItemParams.DEFAULT_SUPPLIER_NAME)
                                .supplierPhone(TestOrderFactory.OrderItemParams.DEFAULT_SUPPLIER_PHONE)
                                .productType(ItemProductType.GOOD)
                                .build(),

                        ItemPrintFormDto.builder()
                                .number(2)
                                .name("Штаны")
                                .amount(2)
                                .vatType(VatType.VAT_18)
                                .price(toRub(4000))
                                .sum(toRub(8000))
                                .sumWithoutSale(toRub(8000))
                                .cisValues(List.of(CIS_2_1, CIS_2_2))
                                .supplierTaxpayerNumber("item_2_sup")
                                .supplierName(TestOrderFactory.OrderItemParams.DEFAULT_SUPPLIER_NAME)
                                .supplierPhone(TestOrderFactory.OrderItemParams.DEFAULT_SUPPLIER_PHONE)
                                .productType(ItemProductType.GOOD)
                                .build(),

                        ItemPrintFormDto.builder()
                                .number(3)
                                .name("Доставка")
                                .amount(1)
                                .vatType(VatType.VAT_18)
                                .price(toRub(100))
                                .sum(toRub(100))
                                .sumWithoutSale(toRub(100))
                                .supplierTaxpayerNumber("delivery_sup")
                                .supplierName(TestOrderFactory.OrderItemParams.DEFAULT_SUPPLIER_NAME)
                                .supplierPhone(TestOrderFactory.OrderItemParams.DEFAULT_SUPPLIER_PHONE)
                                .productType(ItemProductType.SERVICE)
                                .build()
                ))
                .build());
    }

    @Test
    void testOrderFormWithFullCis() {
        Order order = createOrder(true);
        OrderPrintFormDto form = orderPartnerService.form(order.getId(),
                new PickupPointRequestData(order.getPickupPoint().getId(), null, null, null, 3, 7));

        assertThat(form).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(OrderPrintFormDto.builder()
                .externalId(order.getExternalId())
                .deliveryDate(null)
                .amount(3)
                .orderSender(order.getSender().getIncorporation())
                .recipientName(order.getRecipientName())
                .totalVat(toRub(1818))
                .totalSum(toRub(10100))
                .totalSumWithoutSale(toRub(10100))
                .items(List.of(
                        ItemPrintFormDto.builder()
                                .number(1)
                                .name("Футболка")
                                .amount(1)
                                .vatType(VatType.VAT_18)
                                .price(toRub(2000))
                                .sum(toRub(2000))
                                .sumWithoutSale(toRub(2000))
                                .cisValues(List.of(CIS_1_1 + CIS_FULL_POSTFIX))
                                .supplierTaxpayerNumber("item_1_sup")
                                .supplierName(TestOrderFactory.OrderItemParams.DEFAULT_SUPPLIER_NAME)
                                .supplierPhone(TestOrderFactory.OrderItemParams.DEFAULT_SUPPLIER_PHONE)
                                .productType(ItemProductType.GOOD)
                                .build(),

                        ItemPrintFormDto.builder()
                                .number(2)
                                .name("Штаны")
                                .amount(2)
                                .vatType(VatType.VAT_18)
                                .price(toRub(4000))
                                .sum(toRub(8000))
                                .sumWithoutSale(toRub(8000))
                                .cisValues(List.of(CIS_2_1 + CIS_FULL_POSTFIX, CIS_2_2 + CIS_FULL_POSTFIX))
                                .supplierTaxpayerNumber("item_2_sup")
                                .supplierName(TestOrderFactory.OrderItemParams.DEFAULT_SUPPLIER_NAME)
                                .supplierPhone(TestOrderFactory.OrderItemParams.DEFAULT_SUPPLIER_PHONE)
                                .productType(ItemProductType.GOOD)
                                .build(),

                        ItemPrintFormDto.builder()
                                .number(3)
                                .name("Доставка")
                                .amount(1)
                                .vatType(VatType.VAT_18)
                                .price(toRub(100))
                                .sum(toRub(100))
                                .sumWithoutSale(toRub(100))
                                .supplierTaxpayerNumber("delivery_sup")
                                .supplierName(TestOrderFactory.OrderItemParams.DEFAULT_SUPPLIER_NAME)
                                .supplierPhone(TestOrderFactory.OrderItemParams.DEFAULT_SUPPLIER_PHONE)
                                .productType(ItemProductType.SERVICE)
                                .build()
                ))
                .build());
    }

    @Test
    void successDeliverFashionOrder() {
        Order order = orderFactory.createSimpleFashionOrder();
        order = orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.finishFitting(order.getId());
        orderPartnerService.deliver(order.getId(),
                new OrderDeliverDto(null, null),
                new PickupPointRequestData(order.getPickupPoint().getId(), 1L, "Пункт выдачи Беру", 1L, 3, 7));

        Order delivered = orderRepository.findByIdOrThrow(order.getId());
        OrderDeliveryResultParams deliveryResult = orderDeliveryResultQueryService.get(order.getId());
        assertThat(delivered.getStatus()).isEqualTo(PvzOrderStatus.TRANSMITTED_TO_RECIPIENT);
        assertThat(delivered.getDsApiCheckpoint()).isEqualTo(ARRIVED_TO_PICKUP_POINT.getCode());
        assertThat(deliveryResult.getStatus()).isEqualTo(PartialDeliveryStatus.PAYED);
    }

    @Test
    void testFashionOrderForm() {
        Order order = orderFactory.createSimpleFashionOrder();
        orderFactory.receiveOrder(order.getId());

        orderDeliveryResultCommandService.startFitting(order.getId());
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_2_1, ItemDeliveryFlow.RETURN);
        orderDeliveryResultCommandService.updateItemFlow(order.getId(), UIT_1_1, ItemDeliveryFlow.RETURN);

        OrderPrintFormDto form = orderPartnerService.form(order.getId(),
                new PickupPointRequestData(order.getPickupPoint().getId(), null, null, null, 3, 7));

        assertThat(form).isEqualTo(OrderPrintFormDto.builder()
                .externalId(order.getExternalId())
                .deliveryDate(null)
                .amount(1)
                .orderSender(order.getSender().getIncorporation())
                .recipientName(order.getRecipientName())
                .totalVat(toRub(360))
                .totalSum(toRub(2000))
                .totalSumWithoutSale(toRub(2000))
                .items(List.of(
                        ItemPrintFormDto.builder()
                                .number(1)
                                .name("Штаны")
                                .amount(1)
                                .vatType(VatType.VAT_18)
                                .price(toRub(2000))
                                .sum(toRub(2000))
                                .sumWithoutSale(toRub(2000))
                                .cisValues(List.of(CIS_2_2))
                                .supplierTaxpayerNumber("item_2_sup")
                                .supplierName(TestOrderFactory.OrderItemParams.DEFAULT_SUPPLIER_NAME)
                                .supplierPhone(TestOrderFactory.OrderItemParams.DEFAULT_SUPPLIER_PHONE)
                                .productType(ItemProductType.GOOD)
                                .build()
                ))
                .build());
    }

    @Test
    void cantStartFittingAfterDeliver() {
        Order order = orderFactory.createSimpleFashionOrder();
        Long orderId = order.getId();
        order = orderFactory.receiveOrder(orderId);
        orderPartnerService.deliver(orderId,
                new OrderDeliverDto(null, null),
                new PickupPointRequestData(order.getPickupPoint().getId(), 1L, "Пункт выдачи Беру", 1L, 3, 7));
        assertThatThrownBy(() -> orderDeliveryResultCommandService.startFitting(orderId))
                .isExactlyInstanceOf(TplInvalidActionException.class);
    }

    @Test
    void verifyInvalidBarcode() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        Order order = createAndReceiveOrder(pickupPoint);
        String barcode = order.getExternalId() + "-" + "10";
        PickupPointRequestData pickupPointRequestData = buildRequestData(pickupPoint);
        orderPartnerService.verifyBarcode(barcode, pickupPointRequestData);

        checkVerifyBarcodeLog(pickupPoint, order, false, barcode);
    }

    @Test
    void verifyValidCode() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        Order order = createAndReceiveOrder(pickupPoint);
        String barcode = order.getExternalId() + "-" + DEFAULT_VERIFICATION_CODE;
        PickupPointRequestData pickupPointRequestData = buildRequestData(pickupPoint);
        orderPartnerService.verifyBarcode(barcode, pickupPointRequestData);
        checkVerifyBarcodeLog(pickupPoint, order, true, barcode);
    }

    @Test
    void verifyBarcodeWithBadExternalId() {
        var pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        Order order = createAndReceiveOrder(pickupPoint);
        String barcode = "12345" + order.getExternalId() + "-" + DEFAULT_VERIFICATION_CODE;
        PickupPointRequestData pickupPointRequestData = buildRequestData(pickupPoint);
        orderPartnerService.verifyBarcode(barcode, pickupPointRequestData);
        checkVerifyBarcodeLog(pickupPoint, order, true, barcode);
    }

    @Test
    void verifyBarcodeWithWrongExternalId() {
        var pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        createAndReceiveOrder(pickupPoint);
        String barcode = "12345-" + DEFAULT_VERIFICATION_CODE;
        PickupPointRequestData pickupPointRequestData = buildRequestData(pickupPoint);
        assertThatThrownBy(() -> orderPartnerService.verifyBarcode(barcode, pickupPointRequestData))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
    }

    @Test
    void findByBarcodeExternalIdOnly() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        Order order = createAndReceiveOrder(pickupPoint);
        PickupPointRequestData requestData = buildRequestData(pickupPoint);

        assertThat(orderPartnerService.findByBarcode(
                requestData,
                List.of(EXTERNAL_ID),
                List.of(order.getExternalId())
        )).hasSize(1);

        assertThat(orderPartnerService.findByBarcode(
                requestData,
                List.of(EXTERNAL_ID),
                List.of(getPlaces(order.getId()).get(0))
        )).hasSize(0);
    }

    @Test
    void findByBarcodePlacesCodesOnly() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        Order order = createAndReceiveOrder(pickupPoint);
        PickupPointRequestData requestData = buildRequestData(pickupPoint);


        assertThat(orderPartnerService.findByBarcode(
                requestData,
                List.of(PLACE_BARCODE),
                List.of(order.getExternalId())
        )).hasSize(0);

        assertThat(orderPartnerService.findByBarcode(
                requestData,
                List.of(PLACE_BARCODE),
                List.of(getPlaces(order.getId()).get(0))
        )).hasSize(1);
    }

    @Test
    void findByBarcodeBoth() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        Order order = createAndReceiveOrder(pickupPoint);
        PickupPointRequestData requestData = buildRequestData(pickupPoint);

        assertThat(orderPartnerService.findByBarcode(
                requestData,
                List.of(EXTERNAL_ID, PLACE_BARCODE),
                List.of(order.getExternalId())
        )).hasSize(1);

        assertThat(orderPartnerService.findByBarcode(
                requestData,
                List.of(EXTERNAL_ID, PLACE_BARCODE),
                List.of(getPlaces(order.getId()).get(0))
        )).hasSize(1);
    }

    @Test
    void findByMultipleBarcodes() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        Order order = createAndReceiveOrder(pickupPoint);
        PickupPointRequestData requestData = buildRequestData(pickupPoint);

        assertThat(orderPartnerService.findByBarcode(
                requestData,
                List.of(PLACE_BARCODE),
                getPlaces(order.getId())
        )).hasSize(1);
    }

    @Test
    void findByBarcodeForReceptionFromPvz() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        Order order = createAndReceiveOrder(pickupPoint);
        PickupPointRequestData requestData = buildRequestData(pickupPoint);

        var result = orderPartnerService.findByBarcodeForReception(
                requestData,
                getPlaces(order.getId()));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSource()).isEqualTo(OrderSource.PVZ);
    }

    @Test
    void findByBarcodeForReceptionFromSc() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        PickupPointRequestData requestData = buildRequestData(pickupPoint);
        String barcode = "AAA";

        Mockito.when(scLogisticsClientImpl.findInventoryItemByBarcode(anyLong(), eq(List.of(barcode))))
                .thenReturn(InventoryItemDto.builder()
                        .externalId("111")
                        .declaredCost(BigDecimal.ONE)
                        .courierId(222L)
                        .placeCodes(List.of(barcode))
                        .places(
                                List.of(new InventoryItemPlaceDto(barcode, InventoryItemPlaceStatus.CAN_ACCEPT))
                        )
                        .build());

        var result = orderPartnerService.findByBarcodeForReception(
                requestData, List.of(barcode));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSource()).isEqualTo(OrderSource.SC);
    }

    @Test
    void findByBarcodeForReceptionFromScAllAccepted() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        PickupPointRequestData requestData = buildRequestData(pickupPoint);
        String barcode = "AAA";

        Mockito.when(scLogisticsClientImpl.findInventoryItemByBarcode(anyLong(), eq(List.of(barcode))))
                .thenReturn(InventoryItemDto.builder()
                        .externalId("111")
                        .declaredCost(BigDecimal.ONE)
                        .courierId(222L)
                        .placeCodes(List.of(barcode))
                        .places(
                                List.of(new InventoryItemPlaceDto(barcode, InventoryItemPlaceStatus.ACCEPTED))
                        )
                        .build());

        var result = orderPartnerService.findByBarcodeForReception(
                requestData, List.of(barcode));

        assertThat(result).hasSize(0);
    }

    @Test
    void findByBarcodeForReceptionFromScShowsOnlyAcceptable() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        PickupPointRequestData requestData = buildRequestData(pickupPoint);
        String barcode1 = "AAA";
        String barcode2 = "BBB";

        Mockito.when(scLogisticsClientImpl.findInventoryItemByBarcode(anyLong(), eq(List.of(barcode1, barcode2))))
                .thenReturn(InventoryItemDto.builder()
                        .externalId("111")
                        .declaredCost(BigDecimal.ONE)
                        .courierId(222L)
                        .placeCodes(List.of(barcode1, barcode2))
                        .places(
                                List.of(new InventoryItemPlaceDto(barcode1, InventoryItemPlaceStatus.ACCEPTED),
                                        new InventoryItemPlaceDto(barcode2, InventoryItemPlaceStatus.CAN_ACCEPT))
                        )
                        .build());

        var result = orderPartnerService.findByBarcodeForReception(
                requestData, List.of(barcode1, barcode2));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSource()).isEqualTo(OrderSource.SC);
        assertThat(result.get(0).getLabels()).contains(OrderLabel.BIKE_COURIER);
    }


    private PickupPointRequestData buildRequestData(PickupPoint pickupPoint) {
        return new PickupPointRequestData(
                pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(), 1L,
                pickupPoint.getTimeOffset(), pickupPoint.getStoragePeriod()
        );
    }

    private List<String> getPlaces(long orderId) {
        return orderQueryService.getOrderAdditionalParams(orderId).getPlaceCodes();
    }

    private void checkVerifyBarcodeLog(PickupPoint pickupPoint, Order order, boolean accepted, String barcode) {
        LogPickupPointScan actual = logPickupPointScanRepository.findAll().get(0);
        LogPickupPointScan expected = LogPickupPointScan.builder()
                .pickupPointId(pickupPoint.getId())
                .logPickupPointScanType(LogPickupPointScanType.VERIFY_ORDER_BARCODE)
                .scannedAt(Instant.now(clock))
                .uid("1")
                .details(LogPickupPointScanDetails.builder()
                        .orderId(order.getId())
                        .accepted(accepted)
                        .barcode(barcode)
                        .build())
                .build();
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(expected);
    }

    private Order createAndReceiveOrder(PickupPoint pickupPoint) {
        String externalId = RandomStringUtils.randomAlphanumeric(6);
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());

        OffsetDateTime creationTime = OffsetDateTime.of(
                LocalDateTime.of(2021, 5, 5, 17, 50, 0),
                zone);
        clock.setFixed(creationTime.toInstant(), zone);
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .params(TestOrderFactory.OrderParams.builder()
                        .externalId(externalId)
                        .deliveryDate(LocalDate.of(2021, 5, 6))
                        .type(OrderType.CLIENT)
                        .paymentType(OrderPaymentType.PREPAID)
                        .places(List.of(
                                TestOrderFactory.OrderPlaceParams.builder()
                                        .barcode("P001248514")
                                        .build(),
                                TestOrderFactory.OrderPlaceParams.builder()
                                        .barcode("P160750615")
                                        .build(),
                                TestOrderFactory.OrderPlaceParams.builder()
                                        .barcode("P160758752")
                                        .build()
                        ))
                        .verificationCode(DEFAULT_VERIFICATION_CODE)
                        .build())
                .build());

        OffsetDateTime arrivedTime = OffsetDateTime.of(
                LocalDateTime.of(2021, 5, 6, 17, 40, 0),
                zone);
        clock.setFixed(arrivedTime.toInstant(), zone);
        return orderFactory.receiveOrder(order.getId());
    }


    private BigDecimal toRub(int value) {
        return BigDecimal.valueOf(value).setScale(2);
    }

    private Order createOrder(boolean withFullCis) {
        return orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPointFactory.createPickupPoint(CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .brandingType(PickupPointBrandingType.NONE)
                                .build())
                        .build()))
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(OrderPaymentType.CARD)
                        .items(List.of(
                                TestOrderFactory.OrderItemParams.builder()
                                        .name("Футболка")
                                        .price(BigDecimal.valueOf(2000))
                                        .count(1)
                                        .isService(false)
                                        .cisValues(List.of(CIS_1_1))
                                        .cisFullValues(withFullCis ? List.of(CIS_1_1 + CIS_FULL_POSTFIX) : emptyList())
                                        .supplierTaxpayerNumber("item_1_sup")
                                        .build(),

                                TestOrderFactory.OrderItemParams.builder()
                                        .name("Штаны")
                                        .price(BigDecimal.valueOf(4000))
                                        .count(2)
                                        .isService(false)
                                        .cisValues(List.of(CIS_2_1, CIS_2_2))
                                        .cisFullValues(withFullCis ?
                                                List.of(CIS_2_1 + CIS_FULL_POSTFIX, CIS_2_2 + CIS_FULL_POSTFIX) :
                                                emptyList())
                                        .supplierTaxpayerNumber("item_2_sup")
                                        .build(),

                                TestOrderFactory.OrderItemParams.builder()
                                        .name("Доставка")
                                        .price(BigDecimal.valueOf(100))
                                        .count(1)
                                        .isService(true)
                                        .cisValues(null)
                                        .supplierTaxpayerNumber("delivery_sup")
                                        .build()
                        ))
                        .build())
                .build());
    }

    @Test
    void testCashboxPaymentAction() {
        var pickupPoint = pickupPointFactory.createPickupPointFromCrm();
        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .cashboxUrl("https://produman.ru/api")
                        .cashboxToken("ieiwufhew")
                        .build());
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .pickupPoint(pickupPoint)
                .build());

        var pickupPointRequestData = new PickupPointRequestData(
                pickupPoint.getId(),
                pickupPoint.getPvzMarketId(),
                pickupPoint.getName(),
                DEFAULT_UID,
                pickupPoint.getTimeOffset(),
                pickupPoint.getStoragePeriod());
        var actual = orderPartnerService.getOrderPage(
                order.getId(), pickupPointRequestData);
        assertThat(actual.getOrder().getActions()).contains(new OrderActionDto(CASHBOX_PAYMENT));

        pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .cashboxUrl("  ")
                        .cashboxToken("ieiwufhew")
                        .build());
        actual = orderPartnerService.getOrderPage(
                order.getId(), pickupPointRequestData);
        assertThat(actual.getOrder().getActions()).doesNotContain(new OrderActionDto(CASHBOX_PAYMENT));
    }
}
