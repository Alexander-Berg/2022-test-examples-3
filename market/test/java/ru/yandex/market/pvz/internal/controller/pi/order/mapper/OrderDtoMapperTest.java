package ru.yandex.market.pvz.internal.controller.pi.order.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.model.order.DeliveryServiceType;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.order.OrderItemParams;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.order.model.OrderType;
import ru.yandex.market.pvz.core.domain.order.model.OrderVerification;
import ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus;
import ru.yandex.market.pvz.core.domain.order.model.VatType;
import ru.yandex.market.pvz.core.domain.order.model.history.OrderHistoryFullParams;
import ru.yandex.market.pvz.core.domain.order.model.params.OrderAdditionalInfoParams;
import ru.yandex.market.pvz.core.domain.order.model.params.OrderPageParams;
import ru.yandex.market.pvz.core.domain.order.model.sender.OrderSenderParams;
import ru.yandex.market.pvz.core.domain.order.model.sibling.SiblingGroupParams;
import ru.yandex.market.pvz.core.domain.order.model.sibling.SiblingOrderQueryService;
import ru.yandex.market.pvz.core.domain.order_delivery_result.PartialDeliveryStatus;
import ru.yandex.market.pvz.core.domain.order_delivery_result.service.OrderDeliveryResultQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.WebLayerTest;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.ItemPrintFormDto;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.ItemProductType;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionDto;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderDto;
import ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderPrintFormDto;
import ru.yandex.market.tpl.common.util.configuration.ConfigurationProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.VERIFICATION_CODE_CLIENT_ENABLED;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.VERIFICATION_CODE_ON_DEMAND_ENABLED;
import static ru.yandex.market.pvz.core.domain.order.model.Order.DEFAULT_EXTEND_STORAGE_PERIOD_MAX_DAYS;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.ARRIVED_TO_PICKUP_POINT;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.CREATED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.DELIVERED_TO_RECIPIENT;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.READY_FOR_RETURN;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.RETURNED_ORDER_WAS_DISPATCHED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.STORAGE_PERIOD_EXPIRED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.STORAGE_PERIOD_EXTENDED;
import static ru.yandex.market.pvz.core.domain.order.model.PvzOrderStatus.TRANSMITTED_TO_RECIPIENT;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_VERIFICATION_CODE;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.CANCEL_DELIVERY;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.CASHBOX_PAYMENT;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.DELIVER;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.EXTEND_STORAGE_PERIOD;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.PRINT_FORM;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.SEND_CODE_VIA_SMS;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.SHOW_NOT_ACCEPTED_BY_COURIER;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.SHOW_SEND_SMS_BUTTON;
import static ru.yandex.market.pvz.internal.controller.pi.order.dto.OrderActionType.SIMPLIFIED_DELIVERY;

@WebLayerTest
@ExtendWith(SpringExtension.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderDtoMapperTest {

    private static final LocalDate EXPIRATION_DATE = LocalDate.of(2020, 12, 10);

    @Mock
    private OrderDeliveryResultQueryService orderDeliveryResultQueryService;

    @Mock
    private OrderItemDtoMapper orderItemDtoMapper;

    @MockBean
    private SiblingOrderQueryService siblingOrderQueryService;

    private final TestPickupPointFactory testPickupPointFactory;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;
    private final ConfigurationProvider configurationProvider;

    private final TestableClock clock = new TestableClock();
    private OrderDtoMapper mapper;

    @BeforeEach
    void setup() {
        configurationGlobalCommandService.setValue(VERIFICATION_CODE_CLIENT_ENABLED, true);
        configurationGlobalCommandService.setValue(VERIFICATION_CODE_ON_DEMAND_ENABLED, true);

        when(orderDeliveryResultQueryService.getStatus(anyLong())).thenReturn(PartialDeliveryStatus.NOT_STARTED);

        mapper = new OrderDtoMapper(clock, configurationProvider, orderItemDtoMapper);
    }

    @Test
    void mapToPrintForm() {
        OrderPageParams order = OrderPageParams.builder()
                .externalId("1")
                .deliveryDate(LocalDate.of(2020, 8, 10))
                .deliveredAt(OffsetDateTime.of(
                        LocalDateTime.of(2020, 8, 11, 10, 50, 30),
                        ZoneOffset.ofHours(3)))
                .deliveredAtDate(LocalDate.of(2020, 8, 11))
                .sender(OrderSenderParams.builder().incorporation("ООО Яндекс Маркет").build())
                .recipientName("Александр Гудков")
                .orderVerification(new OrderVerification(DEFAULT_VERIFICATION_CODE))
                .orderAdditionalInfoParams(OrderAdditionalInfoParams.builder().build())
                .partialDeliveryStatus(PartialDeliveryStatus.NOT_STARTED)
                .items(List.of(
                        OrderItemParams.builder()
                                .id(1L)
                                .name("Шар предсказаний")
                                .count(1)
                                .price(BigDecimal.valueOf(5199.99))
                                .sumPrice(BigDecimal.valueOf(5199.99))
                                .vatType(VatType.VAT_20)
                                .build(),
                        OrderItemParams.builder()
                                .id(2L)
                                .name("Крем-сода")
                                .vatType(VatType.NO_VAT)
                                .count(3)
                                .price(BigDecimal.valueOf(2020.2))
                                .sumPrice(BigDecimal.valueOf(3 * 2020.2))
                                .build())
                )
                .build();

        OrderPrintFormDto expected = OrderPrintFormDto.builder()
                .externalId("1")
                .deliveryDate("11 августа 2020 г.")
                .orderSender("ООО Яндекс Маркет")
                .recipientName("Александр Гудков")
                .items(List.of(
                        ItemPrintFormDto.builder()
                                .number(2)
                                .name("Крем-сода")
                                .amount(3)
                                .price(BigDecimal.valueOf(2020.2))
                                .sumWithoutSale(BigDecimal.valueOf(3 * 2020.2))
                                .sum(BigDecimal.valueOf(3 * 2020.2))
                                .vatType(VatType.NO_VAT)
                                .productType(ItemProductType.GOOD)
                                .build(),
                        ItemPrintFormDto.builder()
                                .number(1)
                                .name("Шар предсказаний")
                                .amount(1)
                                .price(BigDecimal.valueOf(5199.99))
                                .sumWithoutSale(BigDecimal.valueOf(5199.99))
                                .sum(BigDecimal.valueOf(5199.99))
                                .vatType(VatType.VAT_20)
                                .productType(ItemProductType.GOOD)
                                .build()
                ))
                .totalSumWithoutSale(BigDecimal.valueOf(5199.99 + 3 * 2020.2))
                .totalSum(BigDecimal.valueOf(5199.99 + 3 * 2020.2))
                .totalVat(BigDecimal.valueOf(5199.99 * 0.2 + 3 * 2020.2 * 0).setScale(2, RoundingMode.HALF_UP))
                .amount(2)
                .build();

        assertThat(mapper.mapToPrintForm(order)).isEqualTo(expected);
    }

    @Test
    void mapToPrintFormWithoutHistoryStamp() {
        OrderPageParams order = OrderPageParams.builder()
                .externalId("1")
                .deliveryDate(LocalDate.of(2020, 8, 8))
                .deliveredAt(OffsetDateTime.of(
                        LocalDateTime.of(2020, 8, 10, 10, 50, 30),
                        ZoneOffset.ofHours(3)))
                .deliveredAtDate(LocalDate.of(2020, 8, 10))
                .sender(OrderSenderParams.builder().incorporation("ООО Яндекс Маркет").build())
                .recipientName("Александр Гудков")
                .orderVerification(new OrderVerification(DEFAULT_VERIFICATION_CODE))
                .orderAdditionalInfoParams(OrderAdditionalInfoParams.builder().build())
                .partialDeliveryStatus(PartialDeliveryStatus.NOT_STARTED)
                .items(List.of(
                        OrderItemParams.builder()
                                .name("Шар предсказаний")
                                .count(1)
                                .price(BigDecimal.valueOf(5199.99))
                                .sumPrice(BigDecimal.valueOf(5199.99))
                                .vatType(VatType.VAT_20)
                                .build())
                )
                .build();

        OrderPrintFormDto expected = OrderPrintFormDto.builder()
                .externalId("1")
                .deliveryDate("10 августа 2020 г.")
                .orderSender("ООО Яндекс Маркет")
                .recipientName("Александр Гудков")
                .items(List.of(
                        ItemPrintFormDto.builder()
                                .number(1)
                                .name("Шар предсказаний")
                                .amount(1)
                                .price(BigDecimal.valueOf(5199.99))
                                .sumWithoutSale(BigDecimal.valueOf(5199.99))
                                .sum(BigDecimal.valueOf(5199.99))
                                .vatType(VatType.VAT_20)
                                .productType(ItemProductType.GOOD)
                                .build()
                ))
                .totalSumWithoutSale(BigDecimal.valueOf(5199.99))
                .totalSum(BigDecimal.valueOf(5199.99))
                .totalVat(BigDecimal.valueOf(5199.99 * 0.2).setScale(2, RoundingMode.HALF_UP))
                .amount(1)
                .build();

        assertThat(mapper.mapToPrintForm(order)).isEqualTo(expected);
    }

    @Test
    void actionsForCreatedOrder() {
        testActions(CREATED, List.of(SHOW_NOT_ACCEPTED_BY_COURIER), SiblingGroupParams.EMPTY);
    }

    @Test
    void actionsForArrivedOrder() {
        setCurrentDate(EXPIRATION_DATE);
        testActions(ARRIVED_TO_PICKUP_POINT,
                List.of(DELIVER, EXTEND_STORAGE_PERIOD, SEND_CODE_VIA_SMS, SHOW_SEND_SMS_BUTTON),
                SiblingGroupParams.EMPTY);
    }

    @Test
    void actionsForExtendedStorageOrder() {
        testActions(STORAGE_PERIOD_EXTENDED,
                List.of(DELIVER, SEND_CODE_VIA_SMS, SHOW_SEND_SMS_BUTTON), SiblingGroupParams.EMPTY);
    }

    @Test
    void actionsForExpiredOrder() {
        setCurrentDate(EXPIRATION_DATE);
        testActions(STORAGE_PERIOD_EXPIRED,
                List.of(DELIVER, EXTEND_STORAGE_PERIOD, SEND_CODE_VIA_SMS, SHOW_SEND_SMS_BUTTON),
                SiblingGroupParams.EMPTY);
    }

    @Test
    void actionsForExpiredLongTimeAgoOrder() {
        setCurrentDate(EXPIRATION_DATE.plusDays(DEFAULT_EXTEND_STORAGE_PERIOD_MAX_DAYS));
        testActions(STORAGE_PERIOD_EXPIRED, List.of(DELIVER, SEND_CODE_VIA_SMS, SHOW_SEND_SMS_BUTTON),
                SiblingGroupParams.EMPTY);
    }

    @Test
    void actionsForExpiredButPreviouslyExtendedOrder() {
        testActions(STORAGE_PERIOD_EXPIRED,
                List.of(STORAGE_PERIOD_EXTENDED, STORAGE_PERIOD_EXPIRED),
                List.of(DELIVER, SEND_CODE_VIA_SMS, SHOW_SEND_SMS_BUTTON));
    }

    @Test
    void actionsForTransmittedOrder() {
        testActions(TRANSMITTED_TO_RECIPIENT, List.of(CANCEL_DELIVERY, PRINT_FORM), SiblingGroupParams.EMPTY);
    }

    @Test
    void actionsForDeliveredOrder() {
        testActions(DELIVERED_TO_RECIPIENT, List.of(PRINT_FORM), SiblingGroupParams.EMPTY);
    }

    @Test
    void actionsForReadyForReturnOrder() {
        testActions(READY_FOR_RETURN, List.of(), SiblingGroupParams.EMPTY);
    }

    @Test
    void actionsForDispatchedOrder() {
        testActions(RETURNED_ORDER_WAS_DISPATCHED, List.of(), SiblingGroupParams.EMPTY);
    }

    private void testActions(
            PvzOrderStatus status, List<OrderActionType> expectedActions, SiblingGroupParams siblingGroupParams
    ) {
        var pickupPoint = testPickupPointFactory.createPickupPoint();
        testActions(status, expectedActions, pickupPoint, siblingGroupParams);
    }

    private void testActions(PvzOrderStatus status, List<OrderActionType> expectedActions,
                             PickupPoint pickupPoint, SiblingGroupParams siblingGroupParams) {
        OrderPageParams order = OrderPageParams.builder()
                .id(123L)
                .status(status)
                .type(OrderType.CLIENT)
                .paymentType(OrderPaymentType.PREPAID)
                .expirationDate(EXPIRATION_DATE)
                .items(List.of())
                .orderVerification(new OrderVerification(DEFAULT_VERIFICATION_CODE))
                .sender(new OrderSenderParams())
                .partialDeliveryStatus(PartialDeliveryStatus.NOT_STARTED)
                .cashboxPaymentAllowed(pickupPoint.cashboxPaymentAllowed())
                .maxStorageExtensionDate(EXPIRATION_DATE.plusDays(DEFAULT_EXTEND_STORAGE_PERIOD_MAX_DAYS))
                .orderAdditionalInfoParams(
                        OrderAdditionalInfoParams.builder()
                                .deliveryServiceType(DeliveryServiceType.MARKET_COURIER)
                                .acceptedByCourier(false)
                                .build()
                )
                .build();

        OrderDto dto = mapper.mapToOrderDto(order, siblingGroupParams, List.of());

        assertThat(dto.getActions().stream().map(OrderActionDto::getType).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(expectedActions);
    }

    @Test
    void cashboxPaymentAction() {
        var pickupPoint = testPickupPointFactory.createPickupPoint(
                TestPickupPointFactory.PickupPointTestParams
                        .builder()
                        .cashboxUrl("yandex.ru").cashboxToken("LERA_TOP").build());
        testActions(ARRIVED_TO_PICKUP_POINT,
                List.of(DELIVER, SEND_CODE_VIA_SMS, SHOW_SEND_SMS_BUTTON, CASHBOX_PAYMENT), pickupPoint,
                SiblingGroupParams.EMPTY);
    }

    private void testActions(PvzOrderStatus status, List<PvzOrderStatus> history,
                             List<OrderActionType> expectedActions) {
        var pickupPoint = testPickupPointFactory.createPickupPoint();
        var order = OrderPageParams.builder()
                .id(123L)
                .status(status)
                .paymentType(OrderPaymentType.PREPAID)
                .items(List.of())
                .sender(new OrderSenderParams())
                .orderVerification(new OrderVerification(DEFAULT_VERIFICATION_CODE))
                .partialDeliveryStatus(PartialDeliveryStatus.NOT_STARTED)
                .orderAdditionalInfoParams(
                        OrderAdditionalInfoParams.builder()
                                .deliveryServiceType(DeliveryServiceType.MARKET_COURIER)
                                .acceptedByCourier(true)
                                .build()
                )
                .build();

        List<OrderHistoryFullParams> orderHistories = history.stream()
                .map(s -> OrderHistoryFullParams.builder().status(s).build())
                .collect(Collectors.toList());

        OrderDto dto = mapper.mapToOrderDto(order, SiblingGroupParams.EMPTY, orderHistories);

        assertThat(dto.getActions().stream().map(OrderActionDto::getType).collect(Collectors.toList()))
                .isEqualTo(expectedActions);
    }

    private void setCurrentDate(LocalDate date) {
        clock.setFixed(date.atStartOfDay(clock.getZone()).toInstant(), clock.getZone());
    }

    @ParameterizedTest
    @EnumSource(value = PvzOrderStatus.class,
            names = {"ARRIVED_TO_PICKUP_POINT", "STORAGE_PERIOD_EXTENDED", "STORAGE_PERIOD_EXPIRED"})
    void testSimplifiedDeliveryForArrivedOrder(PvzOrderStatus orderStatus) {
        OffsetDateTime recently = OffsetDateTime.now(clock).minus(30, ChronoUnit.MINUTES);

        testActions(orderStatus, List.of(SIMPLIFIED_DELIVERY, DELIVER, SEND_CODE_VIA_SMS, SHOW_SEND_SMS_BUTTON),
                new SiblingGroupParams(List.of(), recently));
    }

    @Test
    void testNoSimplifiedDeliveryForOrderWithDeliveryLongTimeAgo() {
        OffsetDateTime recently = OffsetDateTime.now(clock).minus(2, ChronoUnit.HOURS);

        testActions(ARRIVED_TO_PICKUP_POINT, List.of(DELIVER, SEND_CODE_VIA_SMS, SHOW_SEND_SMS_BUTTON),
                new SiblingGroupParams(List.of(), recently));
    }

    @Test
    void testNoSimplifiedDeliveryForOrderWithoutSiblings() {
        testActions(ARRIVED_TO_PICKUP_POINT, List.of(DELIVER, SEND_CODE_VIA_SMS, SHOW_SEND_SMS_BUTTON),
                SiblingGroupParams.EMPTY);
    }

    @Test
    void testNoSimplifiedDeliveryForR18Order() {
        OrderPageParams order = OrderPageParams.builder()
                .id(123L)
                .status(ARRIVED_TO_PICKUP_POINT)
                .type(OrderType.CLIENT)
                .paymentType(OrderPaymentType.PREPAID)
                .expirationDate(EXPIRATION_DATE)
                .items(List.of())
                .orderVerification(new OrderVerification(DEFAULT_VERIFICATION_CODE))
                .sender(new OrderSenderParams())
                .partialDeliveryStatus(PartialDeliveryStatus.NOT_STARTED)
                .partialDeliveryStatus(PartialDeliveryStatus.NOT_STARTED)
                .orderAdditionalInfoParams(
                        OrderAdditionalInfoParams.builder()
                                .deliveryServiceType(DeliveryServiceType.MARKET_COURIER)
                                .acceptedByCourier(false)
                                .isAdult(true)
                                .build()
                )
                .build();
        OrderDto dto = mapper.mapToOrderDto(order, SiblingGroupParams.EMPTY, List.of());

        assertThat(dto.getActions().stream().map(OrderActionDto::getType).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(DELIVER, SEND_CODE_VIA_SMS, SHOW_SEND_SMS_BUTTON);
    }

    @Test
    void testNoSimplifiedDeliveryForOnDemandOrder() {
        OrderPageParams order = OrderPageParams.builder()
                .id(123L)
                .status(ARRIVED_TO_PICKUP_POINT)
                .type(OrderType.ON_DEMAND)
                .paymentType(OrderPaymentType.PREPAID)
                .expirationDate(EXPIRATION_DATE)
                .items(List.of())
                .orderVerification(new OrderVerification(DEFAULT_VERIFICATION_CODE))
                .sender(new OrderSenderParams())
                .partialDeliveryStatus(PartialDeliveryStatus.NOT_STARTED)
                .orderAdditionalInfoParams(
                        OrderAdditionalInfoParams.builder()
                                .deliveryServiceType(DeliveryServiceType.MARKET_COURIER)
                                .acceptedByCourier(false)
                                .build()
                )
                .build();
        OrderDto dto = mapper.mapToOrderDto(order, SiblingGroupParams.EMPTY, List.of());

        assertThat(dto.getActions())
                .doesNotContain(new OrderActionDto(SIMPLIFIED_DELIVERY));
    }

    @Test
    void testNoSimplifiedDeliveryForPostPaidOrder() {
        OrderPageParams order = OrderPageParams.builder()
                .id(123L)
                .status(ARRIVED_TO_PICKUP_POINT)
                .type(OrderType.CLIENT)
                .paymentType(OrderPaymentType.CARD)
                .expirationDate(EXPIRATION_DATE)
                .items(List.of())
                .orderVerification(new OrderVerification(DEFAULT_VERIFICATION_CODE))
                .sender(new OrderSenderParams())
                .partialDeliveryStatus(PartialDeliveryStatus.NOT_STARTED)
                .orderAdditionalInfoParams(
                        OrderAdditionalInfoParams.builder()
                                .deliveryServiceType(DeliveryServiceType.MARKET_COURIER)
                                .acceptedByCourier(false)
                                .build()
                )
                .build();
        OrderDto dto = mapper.mapToOrderDto(order, SiblingGroupParams.EMPTY, List.of());

        assertThat(dto.getActions())
                .doesNotContain(new OrderActionDto(SIMPLIFIED_DELIVERY));
    }
}
