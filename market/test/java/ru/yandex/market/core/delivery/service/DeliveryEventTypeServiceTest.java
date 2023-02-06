package ru.yandex.market.core.delivery.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentSubmethod;
import ru.yandex.market.core.billing.OrderTrantimeDao;
import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.core.fulfillment.model.DeliveryEventType;
import ru.yandex.market.core.order.OrderService;
import ru.yandex.market.core.order.model.DeliveryRoute;
import ru.yandex.market.core.order.model.MbiOrder;
import ru.yandex.market.core.order.model.MbiOrderBuilder;
import ru.yandex.market.core.order.model.MbiOrderItem;
import ru.yandex.market.core.order.model.MbiOrderStatus;
import ru.yandex.market.core.order.model.OrderDeliveryBuilder;
import ru.yandex.market.core.order.model.OrderDeliveryPartnerType;
import ru.yandex.market.core.order.model.OrderDeliveryType;
import ru.yandex.market.core.util.DateTimes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.CASHBACK;
import static ru.yandex.market.core.order.TestOrderFactory.defaultItemPromo;
import static ru.yandex.market.core.order.TestOrderFactory.defaultOrderItem;
import static ru.yandex.market.core.order.TestOrderFactory.defaultOrderPromo;
import static ru.yandex.market.core.order.model.MbiOrderStatus.CANCELLED_BEFORE_PROCESSING;
import static ru.yandex.market.core.order.model.MbiOrderStatus.CANCELLED_IN_DELIVERY;
import static ru.yandex.market.core.order.model.MbiOrderStatus.CANCELLED_IN_PROCESSING;
import static ru.yandex.market.core.order.model.MbiOrderStatus.DELIVERED;
import static ru.yandex.market.core.order.model.MbiOrderStatus.DELIVERY;
import static ru.yandex.market.core.order.model.MbiOrderStatus.PROCESSING;

/**
 * Тест для {@link DeliveryEventTypeService}
 */
class DeliveryEventTypeServiceTest {

    @Mock
    private OrderService orderService;

    @Mock
    private DeliveryServiceTypeCache deliveryServiceTypeCache;

    @Mock
    private OrderTrantimeDao orderTrantimeDao;

    private DeliveryEventTypeService deliveryEventTypeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        deliveryEventTypeService = new DeliveryEventTypeService(orderService, deliveryServiceTypeCache,
                orderTrantimeDao);
    }

    private static Stream<Arguments> testGetDeliveryEventTypesArgs() {
        return Stream.of(
                Arguments.of(MbiOrderStatus.PROCESSING, true),
                Arguments.of(MbiOrderStatus.PROCESSING, false),
                Arguments.of(MbiOrderStatus.UNKNOWN, true),
                Arguments.of(MbiOrderStatus.UNKNOWN, false),
                Arguments.of(MbiOrderStatus.PICKUP, true),
                Arguments.of(MbiOrderStatus.PICKUP, false),
                Arguments.of(MbiOrderStatus.RESERVED, true),
                Arguments.of(MbiOrderStatus.RESERVED, false),
                Arguments.of(MbiOrderStatus.UNPAID, true),
                Arguments.of(MbiOrderStatus.UNPAID, false),
                Arguments.of(MbiOrderStatus.PENDING, true),
                Arguments.of(MbiOrderStatus.PENDING, false)
        );
    }

    private static Stream<Arguments> cancellationOrderStatusesWithFeeCancellationFromDelivered() {
        return Stream.of(
                Arguments.of(CANCELLED_IN_DELIVERY)
        );
    }

    private static Stream<Arguments> cancellationOrderStatusesWithoutFeeCancellationFromDelivered() {
        return Stream.of(
                Arguments.of(CANCELLED_BEFORE_PROCESSING),
                Arguments.of(CANCELLED_IN_PROCESSING)
        );
    }

    private static Stream<Arguments> testGetDeliveryEventTypesForTrantimesCancelledArgs() {
        return Stream.concat(
                cancellationOrderStatusesWithFeeCancellationFromDelivered(),
                cancellationOrderStatusesWithoutFeeCancellationFromDelivered()
        );
    }

    private static List<OrderSubstatus> ORDER_SUBSTATUSES_FOR_CANCELLED = List.of(
            OrderSubstatus.SHOP_FAILED,
            OrderSubstatus.PENDING_EXPIRED,
            OrderSubstatus.WAREHOUSE_FAILED_TO_SHIP,
            OrderSubstatus.MISSING_ITEM
    );

    private static Stream<Arguments> orderStatusWithSubstatusesWithFeeCancellationFromDelivered() {
        return getMultiplyCollection(List.of(CANCELLED_IN_DELIVERY), ORDER_SUBSTATUSES_FOR_CANCELLED);
    }

    private static Stream<Arguments> orderStatusWithSubstatusesWithoutFeeCancellationFromDelivered() {
        return getMultiplyCollection(List.of(CANCELLED_BEFORE_PROCESSING, CANCELLED_IN_PROCESSING),
                ORDER_SUBSTATUSES_FOR_CANCELLED);
    }

    private static Stream<Arguments> testGetDeliveryEventTypesForTrantimesCancelledFulfilmentIsFalseArgs() {
        return Stream.concat(
                orderStatusWithSubstatusesWithFeeCancellationFromDelivered(),
                orderStatusWithSubstatusesWithoutFeeCancellationFromDelivered()
        );
    }

    private static List<PaymentSubmethod> INSTALLMENTS_PAYMENT_SUBMETHODS = List.of(
            PaymentSubmethod.TINKOFF_INSTALLMENTS_3,
            PaymentSubmethod.TINKOFF_INSTALLMENTS_6,
            PaymentSubmethod.TINKOFF_INSTALLMENTS_12,
            PaymentSubmethod.TINKOFF_INSTALLMENTS_24
    );

    private static <T, E> Stream<Arguments> getMultiplyCollection(Collection<T> firstCollection,
                                                                  Collection<E> secondCollection) {
        ArrayList<Arguments> result = new ArrayList<>();
        firstCollection.forEach(first -> secondCollection.forEach(second -> result.add(Arguments.of(first, second))));
        return result.stream();
    }

    private static Stream<Arguments> orderStatusWithPaymentSubmethodFromDelivery() {
        return getMultiplyCollection(List.of(DELIVERY), INSTALLMENTS_PAYMENT_SUBMETHODS);
    }

    private static Stream<Arguments> orderStatusWithPaymentSubmethodCancelledInProcessing() {
        return getMultiplyCollection(INSTALLMENTS_PAYMENT_SUBMETHODS, ORDER_SUBSTATUSES_FOR_CANCELLED);
    }

    private static Stream<Arguments> orderStatusWithPaymentSubmethodCancelledInProcessingForBnpl() {
        return getMultiplyCollection(List.of(PaymentSubmethod.BNPL), ORDER_SUBSTATUSES_FOR_CANCELLED);
    }

    private static Stream<Arguments> orderStatusWithPaymentSubmethodCancelledInDelivery() {
        return getMultiplyCollection(List.of(CANCELLED_IN_DELIVERY), INSTALLMENTS_PAYMENT_SUBMETHODS);
    }

    private static Stream<Arguments> orderStatusWithPaymentSubmethodCancelledInDeliveryForBnpl() {
        return getMultiplyCollection(List.of(CANCELLED_IN_DELIVERY), List.of(PaymentSubmethod.BNPL));
    }

    @DisplayName("Тест: newStatus not in (DELIVERY, DELIVERED, CANCELLED_BEFORE_PROCESSING, CANCELLED_IN_PROCESSING, " +
            "CANCELLED_IN_DELIVERY)")
    @ParameterizedTest
    @MethodSource("testGetDeliveryEventTypesArgs")
    public void testGetDeliveryEventTypes(MbiOrderStatus newStatus, boolean fromDeliveredStatus) {
        MbiOrder mbiOrder = new MbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setCreationDate(new Date())
                .setStatus(newStatus)
                .setTrantime(new Date())
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(mbiOrder,
                newStatus,
                fromDeliveredStatus,
                false);

        assertTrue(result.isEmpty());
    }

    @DisplayName("Тест: newStatus == DELIVERY & order.isMarketDelivery == false")
    @Test
    public void testGetDeliveryEventTypesForDeliveryStatus() {
        MbiOrder order = getMbiOrderBuilder()
                .setMarketDelivery(false)
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, DELIVERY, true,
                false);

        assertTrue(result.isEmpty());
    }

    @DisplayName("Тест: newStatus == DELIVERY & order.isZdravCityOrder == true")
    @Test
    void testGetDeliveryEventTypesForDeliveryStatusIsZdravCityOrder() {
        MbiOrder order = getMbiOrderBuilder()
                .setMarketDelivery(false)
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setShopId(534145L)
                .setFulfilment(false)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, DELIVERY, true,
                false);

        assertEquals(1, result.size());
        assertTrue(result.contains(DeliveryEventType.FEE));
    }

    @DisplayName("Тест: newStatus == DELIVERED & order.isZdravCityOrder == false & order.isDropShipBySellerOrder == " +
            "true")
    @Test
    void testGetDeliveryEventTypesForDeliveredStatusIsDropShipBySellerOrder() {
        MbiOrder order = getMbiOrderBuilder()
                .setColor(Color.WHITE)
                .setMarketDelivery(false)
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setShopId(1L)
                .setFulfilment(false)
                .setDeliveryPartnerType(OrderDeliveryPartnerType.SHOP)
                .setDeliveryType(OrderDeliveryType.COURIER)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, DELIVERED, true,
                false);

        assertEquals(1, result.size());
        assertTrue(result.contains(DeliveryEventType.FEE));
    }

    @DisplayName("Тест: newStatus == DELIVERY & order.isZdravCityOrder == false & order.isDropShipBySellerOrder == " +
            "false & order.isCrossborder == true")
    @Test
    void testGetDeliveryEventTypesForDeliveryStatusIsCrossborder() {
        MbiOrder order = getMbiOrderBuilder()
                .setMarketDelivery(false)
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setShopId(1L)
                .setFulfilment(false)
                .setDeliveryPartnerType(OrderDeliveryPartnerType.YANDEX_MARKET)
                .setCrossborder(true)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, DELIVERY, true,
                false);

        assertEquals(1, result.size());
        assertTrue(result.contains(DeliveryEventType.CROSSBORDER20_FEE));
    }

    @DisplayName("Тест: newStatus == DELIVERED & order.isZdravCityOrder == false & order.isDropShipBySellerOrder == " +
            "false & order.isCrossborder == false & isNotSortingCenter(order) == false & order.isMarketDelivery == " +
            "true")
    @Test
    void testGetDeliveryEventTypesForDeliveredStatusIsNotSortingCenterWithEmptyOrderItems() {
        long orderId = 666L;
        Mockito.when(orderService.getOrderItems(Mockito.eq(orderId))).thenReturn(List.of());
        Mockito.when(deliveryServiceTypeCache.getDeliveryServiceType(Mockito.anyLong())).thenReturn(DeliveryServiceType.CARRIER);
        MbiOrder order = getMbiOrderBuilder()
                .setMarketDelivery(true)
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setShopId(1L)
                .setFulfilment(false)
                .setDeliveryPartnerType(OrderDeliveryPartnerType.YANDEX_MARKET)
                .setCrossborder(false)
                .setId(orderId)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, DELIVERED, true,
                false);

        assertEquals(3, result.size());
        assertTrue(result.contains(DeliveryEventType.FEE));
        assertTrue(result.contains(DeliveryEventType.DELIVERY_TO_CUSTOMER));
        assertTrue(result.contains(DeliveryEventType.CROSSREGIONAL_DELIVERY));
    }

    @DisplayName("Тест: newStatus == DELIVERY & order.isZdravCityOrder == false & order.isDropShipBySellerOrder == " +
            "false & order.isCrossborder == false & isNotSortingCenter(order) == true & order.isMarketDelivery == true")
    @Test
    void testGetDeliveryEventTypesForDeliveryStatusIsNotSortingCenter() {
        long orderId = 666L;
        long fulfilmentWarehouseId = 123L;
        MbiOrderItem orderItem = new MbiOrderItem.Builder()
                .setFulfilmentWarehouseId(fulfilmentWarehouseId)
                .build();
        Mockito.when(orderService.getOrderItems(Mockito.eq(orderId))).thenReturn(List.of(orderItem, orderItem));
        Mockito.when(deliveryServiceTypeCache.getDeliveryServiceType(Mockito.eq(fulfilmentWarehouseId))).thenReturn(DeliveryServiceType.CARRIER);
        MbiOrder order = getMbiOrderBuilder()
                .setMarketDelivery(true)
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setShopId(1L)
                .setFulfilment(false)
                .setDeliveryPartnerType(OrderDeliveryPartnerType.YANDEX_MARKET)
                .setCrossborder(false)
                .setId(orderId)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, DELIVERY, true,
                false);

        assertEquals(1, result.size());
        assertTrue(result.contains(DeliveryEventType.SORTING));
    }

    @DisplayName("Тест: newStatus == DELIVERED & order.isZdravCityOrder == false & order.isDropShipBySellerOrder == " +
            "false & order.isCrossborder == false & isNotSortingCenter(order) == false & order.isMarketDelivery == " +
            "true")
    @Test
    void testGetDeliveryEventTypesForDeliveredStatusIsMarketDelivery() {
        MbiOrder order = getMbiOrderBuilder()
                .setMarketDelivery(true)
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setShopId(1L)
                .setFulfilment(true)
                .setDeliveryPartnerType(OrderDeliveryPartnerType.YANDEX_MARKET)
                .setCrossborder(false)
                .setColor(Color.BLUE)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, DELIVERED, true,
                false);

        assertEquals(4, result.size());
        assertTrue(result.contains(DeliveryEventType.FEE));
        assertTrue(result.contains(DeliveryEventType.DELIVERY_TO_CUSTOMER));
        assertTrue(result.contains(DeliveryEventType.CROSSREGIONAL_DELIVERY));
        assertTrue(result.contains(DeliveryEventType.FF_PROCESSING));
    }

    @DisplayName("Тест: newStatus == DELIVERED & order.isZdravCityOrder == false & order.isDropShipBySellerOrder == " +
            "false & order.isCrossborder == false & isNotSortingCenter(order) == false & order.isMarketDelivery == " +
            "true & isExpress = true")
    @Test
    void testGetDeliveryEventTypesForDeliveredStatusIsMarketDeliveryIsExpress() {
        MbiOrder order = getMbiOrderBuilder()
                .setMarketDelivery(true)
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setShopId(1L)
                .setFulfilment(false)
                .setExpress(true)
                .setDeliveryPartnerType(OrderDeliveryPartnerType.YANDEX_MARKET)
                .setCrossborder(false)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, DELIVERED, true,
                false);

        assertEquals(2, result.size());
        assertTrue(result.contains(DeliveryEventType.FEE));
        assertTrue(result.contains(DeliveryEventType.EXPRESS_DELIVERED));
    }

    @DisplayName("Тест: newStatus == DELIVERED & order.getColor != BLUE")
    @Test
    void testGetDeliveryEventTypesForDeliveredStatusColorIsNotBlue() {
        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setColor(Color.UNKNOWN)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, DELIVERED, true,
                false);

        assertTrue(result.isEmpty());
    }

    @DisplayName("Тест: newStatus == DELIVERED & order.getCrossborder == true")
    @Test
    void testGetDeliveryEventTypesForDeliveredStatusCrossborderIsTrue() {
        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setCrossborder(true)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, DELIVERED, true,
                false);

        assertTrue(result.isEmpty());
    }

    @DisplayName("Тест: newStatus == DELIVERED & order.getCrossborder == false")
    @Test
    void testGetDeliveryEventTypesForDeliveredStatusCrossborderIsFalse() {
        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setCrossborder(false)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, DELIVERED, true,
                false);

        assertEquals(1, result.size());
        assertTrue(result.contains(DeliveryEventType.FEE));
    }

    @DisplayName("Тест: newStatus == DELIVERED & order.getCrossborder == false & loyaltyProgramPartner = true")
    @Test
    void testGetDeliveryEventTypesForDeliveredStatusCrossborderIsFalseLoyaltyParticipation() {
        long orderId = 500L;
        MbiOrderItem orderItem1 = new MbiOrderItem.Builder()
                .setIsLoyaltyProgramPartner(false)
                .build();
        MbiOrderItem orderItem2 = new MbiOrderItem.Builder()
                .setIsLoyaltyProgramPartner(true)
                .build();
        Mockito.when(orderService.getOrderItems(Mockito.eq(orderId))).thenReturn(List.of(orderItem1, orderItem2));
        Mockito.when(deliveryServiceTypeCache.getDeliveryServiceType(Mockito.anyLong())).thenReturn(DeliveryServiceType.CARRIER);

        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setId(orderId)
                .setCrossborder(false)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, DELIVERED, true,
                false);

        assertEquals(2, result.size());
        assertTrue(result.contains(DeliveryEventType.FEE));
        assertTrue(result.contains(DeliveryEventType.LOYALTY_PARTICIPATION_FEE));
    }

    @DisplayName("Тест: newStatus == DELIVERED & order.getCrossborder == false & loyaltyProgramPartner = false")
    @Test
    void testGetDeliveryEventTypesForDeliveredStatusCrossborderIsFalseLoyaltyProgramPartnerIsFalse_LoyaltyParticipation
            () {
        long orderId = 500L;

        var orderPromos = List.of(
                defaultOrderPromo(CASHBACK, "promo-1").build(),
                defaultOrderPromo(CASHBACK, "promo-2").build()
        );
        MbiOrderItem orderItem1 = defaultOrderItem(orderId, 1, emptyList())
                .setIsLoyaltyProgramPartner(false)
                .build();
        MbiOrderItem orderItem2 = defaultOrderItem(orderId, 2, emptyList())
                .setIsLoyaltyProgramPartner(null)
                .build();
        Mockito.when(orderService.getOrderItems(Mockito.eq(orderId))).thenReturn(List.of(orderItem1, orderItem2));
        Mockito.when(orderService.getOrderItemPromos(anyCollection(), eq(CASHBACK)))
                .thenReturn(Map.ofEntries(
                        Map.entry(orderItem1.getId(), List.of(
                                defaultItemPromo(orderPromos.get(0))
                                        .setPartnerId(null)
                                        .build()
                        )),
                        Map.entry(orderItem2.getId(), List.of(
                                defaultItemPromo(orderPromos.get(0))
                                        .setPartnerId(123L)
                                        .setPartnerCashbackPercent(null)
                                        .build(),
                                defaultItemPromo(orderPromos.get(1))
                                        .setPartnerId(123L)
                                        .setPartnerCashbackPercent(BigDecimal.TEN)
                                        .build()
                        ))
                ));
        Mockito.when(deliveryServiceTypeCache.getDeliveryServiceType(Mockito.anyLong())).thenReturn(DeliveryServiceType.CARRIER);

        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setId(orderId)
                .setCrossborder(false)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, DELIVERED, true,
                false);

        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertThat(result).contains(DeliveryEventType.FEE, DeliveryEventType.LOYALTY_PARTICIPATION_FEE)
        );
    }

    // При отмене FF заказа (CANCELLED_IN_DELIVERY) из статуса DELIVERED - должна быть отмена комиссии
    @DisplayName("Тест: newStatus in (CANCELLED_IN_DELIVERY) & fromDeliveredStatus == true & order.getFulfilment == " +
            "true & order.getCrossborder == false")
    @ParameterizedTest
    @MethodSource("cancellationOrderStatusesWithFeeCancellationFromDelivered")
    void testGetDeliveryEventTypesForTrantimesCancelledWithFeeCancelFF(MbiOrderStatus newStatus) {
        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setFulfilment(true)
                .setCrossborder(false)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, true,
                false);

        assertEquals(1, result.size());
        assertTrue(result.contains(DeliveryEventType.FEE_CANCELLATION));
    }

    // При отмене заказа (CANCELLED_IN_DELIVERY) из статуса DELIVERED - должна быть отмена комиссии
    @DisplayName("Тест: newStatus in (CANCELLED_IN_DELIVERY) & fromDeliveredStatus == true & order.getFulfilment == " +
            "false & order.getCrossborder == false")
    @ParameterizedTest
    @MethodSource("cancellationOrderStatusesWithFeeCancellationFromDelivered")
    void testGetDeliveryEventTypesForTrantimesCancelledWithFeeCancel(MbiOrderStatus newStatus) {
        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setFulfilment(false)
                .setCrossborder(false)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, true,
                false);

        assertEquals(1, result.size());
        assertTrue(result.contains(DeliveryEventType.FEE_CANCELLATION));
    }

    // При отмене FF заказа (CANCELLED_BEFORE_PROCESSING, CANCELLED_IN_PROCESSING) из статуса DELIVERED - не должно
    // быть отмены комиссии
    @DisplayName("Тест: newStatus in (CANCELLED_BEFORE_PROCESSING, CANCELLED_IN_PROCESSING) & fromDeliveredStatus == " +
            "true & order.getCrossborder == false")
    @ParameterizedTest
    @MethodSource("cancellationOrderStatusesWithoutFeeCancellationFromDelivered")
    void testGetDeliveryEventTypesForTrantimesCancelledWithoutFeeCancelFF(MbiOrderStatus newStatus) {
        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setFulfilment(true)
                .setCrossborder(false)
                .setMarketDelivery(true)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, true,
                false);

        assertTrue(result.isEmpty());
    }

    // При отмене заказа (CANCELLED_BEFORE_PROCESSING, CANCELLED_IN_PROCESSING) из статуса DELIVERED - не должно быть
    // отмены комиссии
    @DisplayName("Тест: newStatus in (CANCELLED_BEFORE_PROCESSING, CANCELLED_IN_PROCESSING) & fromDeliveredStatus == " +
            "true & order.getCrossborder == false")
    @ParameterizedTest
    @MethodSource("cancellationOrderStatusesWithoutFeeCancellationFromDelivered")
    void testGetDeliveryEventTypesForTrantimesCancelledWithoutFeeCancel(MbiOrderStatus newStatus) {
        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setFulfilment(false)
                .setCrossborder(false)
                .setMarketDelivery(true)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, true,
                false);

        assertTrue(result.isEmpty());
    }

    // При отмене заказа (CANCELLED_IN_DELIVERY) из статуса DELIVERED - должна быть отмена комиссии
    // Тест с участием поставщика в программе лояльности
    @DisplayName("Тест: newStatus == (CANCELLED_IN_DELIVERY) & fromDeliveredStatus == true & order.getFulfilment == " +
            "true & order.getCrossborder == false")
    @ParameterizedTest
    @MethodSource("cancellationOrderStatusesWithFeeCancellationFromDelivered")
    void testGetDeliveryEventTypesForTrantimesCancelledLoyaltyParticipationFeeCancel(MbiOrderStatus newStatus) {
        long orderId = 500L;
        MbiOrderItem orderItem1 = new MbiOrderItem.Builder()
                .setIsLoyaltyProgramPartner(false)
                .build();
        MbiOrderItem orderItem2 = new MbiOrderItem.Builder()
                .setIsLoyaltyProgramPartner(true)
                .build();
        Mockito.when(orderService.getOrderItems(Mockito.eq(orderId))).thenReturn(List.of(orderItem1, orderItem2));
        Mockito.when(deliveryServiceTypeCache.getDeliveryServiceType(Mockito.anyLong())).thenReturn(DeliveryServiceType.CARRIER);

        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setId(orderId)
                .setFulfilment(true)
                .setCrossborder(false)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, true,
                false);

        assertEquals(2, result.size());
        assertTrue(result.contains(DeliveryEventType.FEE_CANCELLATION));
        assertTrue(result.contains(DeliveryEventType.LOYALTY_PARTICIPATION_FEE_CANCELLATION));
    }

    // При отмене заказа (CANCELLED_BEFORE_PROCESSING, CANCELLED_IN_PROCESSING) из статуса DELIVERED - не должно быть
    // отмена комиссии
    // Тест с участием поставщика в программе лояльности
    @DisplayName("Тест: newStatus == (CANCELLED_BEFORE_PROCESSING, CANCELLED_IN_PROCESSING) & fromDeliveredStatus == " +
            "true & order.getFulfilment == true & order.getCrossborder == false")
    @ParameterizedTest
    @MethodSource("cancellationOrderStatusesWithoutFeeCancellationFromDelivered")
    void testGetDeliveryEventTypesForTrantimesCancelledLoyaltyParticipation(MbiOrderStatus newStatus) {
        long orderId = 500L;
        MbiOrderItem orderItem1 = new MbiOrderItem.Builder()
                .setIsLoyaltyProgramPartner(false)
                .build();
        MbiOrderItem orderItem2 = new MbiOrderItem.Builder()
                .setIsLoyaltyProgramPartner(true)
                .build();
        Mockito.when(orderService.getOrderItems(Mockito.eq(orderId))).thenReturn(List.of(orderItem1, orderItem2));
        Mockito.when(deliveryServiceTypeCache.getDeliveryServiceType(Mockito.anyLong())).thenReturn(DeliveryServiceType.CARRIER);

        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setId(orderId)
                .setFulfilment(true)
                .setCrossborder(false)
                .setMarketDelivery(true)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, true,
                false);

        assertTrue(result.isEmpty());
    }

    // Тест отмены заказа не из статуса DELIVERED с подстатусами, FF=false
    @DisplayName("Тест: newStatus in (CANCELLED_BEFORE_PROCESSING, CANCELLED_IN_PROCESSING, CANCELLED_IN_DELIVERY) & " +
            "fromDeliveredStatus == false & order.getFulfilment == false & order.getCrossborder == false")
    @ParameterizedTest
    @MethodSource("testGetDeliveryEventTypesForTrantimesCancelledFulfilmentIsFalseArgs")
    void testGetDeliveryEventTypesForTrantimesCancelledFulfilmentIsFalse(MbiOrderStatus newStatus,
                                                                         OrderSubstatus substatus) {
        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setFulfilment(false)
                .setCrossborder(false)
                .setMarketDelivery(true)
                .setStatus(CANCELLED_BEFORE_PROCESSING)
                .setSubstatus(substatus)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, false,
                false);

        assertEquals(1, result.size());
        assertTrue(result.contains(DeliveryEventType.CANCELLED_ORDER_FEE));
    }

    // Тест отмены заказа не из статуса DELIVERED с подстатусами, FF=true
    @DisplayName("Тест: newStatus in (CANCELLED_BEFORE_PROCESSING, CANCELLED_IN_PROCESSING, CANCELLED_IN_DELIVERY) & " +
            "fromDeliveredStatus == false & order.getFulfilment == true & order.getCrossborder == false")
    @ParameterizedTest
    @MethodSource("testGetDeliveryEventTypesForTrantimesCancelledFulfilmentIsFalseArgs")
    void testGetDeliveryEventTypesForTrantimesCancelledFulfilmentIsTrue(MbiOrderStatus newStatus,
                                                                        OrderSubstatus substatus) {
        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setFulfilment(true)
                .setCrossborder(false)
                .setMarketDelivery(true)
                .setStatus(CANCELLED_BEFORE_PROCESSING)
                .setSubstatus(substatus)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, false,
                false);

        assertTrue(result.isEmpty());
    }

    // Заказ не доставлен
    @DisplayName("Тест: newStatus in (CANCELLED_BEFORE_PROCESSING, CANCELLED_IN_PROCESSING, CANCELLED_IN_DELIVERY) & " +
            "fromDeliveredStatus == false & order.getFulfilment == false & order.getCrossborder == false & order" +
            ".getSubstatus == DELIVERY_SERVICE_UNDELIVERED")
    @ParameterizedTest
    @MethodSource("testGetDeliveryEventTypesForTrantimesCancelledArgs")
    void testGetDeliveryEventTypesForTrantimesCancelledSubstatusIsUndelivered(MbiOrderStatus newStatus) {
        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setFulfilment(false)
                .setCrossborder(false)
                .setMarketDelivery(true)
                .setSubstatus(OrderSubstatus.DELIVERY_SERVICE_UNDELIVERED)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, false,
                false);

        assertEquals(0, result.size());
    }

    // Заказ не доставлен с подстатусами
    @DisplayName("Тест: newStatus in (CANCELLED_BEFORE_PROCESSING, CANCELLED_IN_PROCESSING, CANCELLED_IN_DELIVERY) & " +
            "fromDeliveredStatus == false & order.getFulfilment == false & order.getCrossborder == false")
    @ParameterizedTest
    @MethodSource("testGetDeliveryEventTypesForTrantimesCancelledFulfilmentIsFalseArgs")
    void testGetDeliveryEventTypesForTrantimesCancelledFulfilmentIsFalseFromDelivered(MbiOrderStatus newStatus,
                                                                                      OrderSubstatus substatus) {
        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setFulfilment(false)
                .setCrossborder(false)
                .setMarketDelivery(true)
                .setStatus(CANCELLED_BEFORE_PROCESSING)
                .setSubstatus(substatus)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, false,
                false);

        assertEquals(1, result.size());
        assertTrue(result.contains(DeliveryEventType.CANCELLED_ORDER_FEE));
    }

    // FF заказ не доставлен с подстатусами
    @DisplayName("Тест: newStatus in (CANCELLED_BEFORE_PROCESSING, CANCELLED_IN_PROCESSING, CANCELLED_IN_DELIVERY) & " +
            "fromDeliveredStatus == false & order.getFulfilment == true & order.getCrossborder == false")
    @ParameterizedTest
    @MethodSource("testGetDeliveryEventTypesForTrantimesCancelledFulfilmentIsFalseArgs")
    void testGetDeliveryEventTypesForTrantimesCancelledFulfilmentIsTrueFromDelivered(MbiOrderStatus newStatus,
                                                                                     OrderSubstatus substatus) {
        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setFulfilment(true)
                .setCrossborder(false)
                .setMarketDelivery(true)
                .setStatus(CANCELLED_BEFORE_PROCESSING)
                .setSubstatus(substatus)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, false,
                false);

        assertTrue(result.isEmpty());
    }

    // При отмене заказа (CANCELLED_IN_DELIVERY) из статуса DELIVERED не по вине партнёра -
    // должна быть отмена комиссии за размещение, отмена комиссии за доставка и отмена услуги магистраль
    @DisplayName("Тест: newStatus in (CANCELLED_BEFORE_PROCESSING, CANCELLED_IN_PROCESSING, CANCELLED_IN_DELIVERY) & " +
            "fromDeliveredStatus == true & order.getFulfilment == true & order.getCrossborder == false")
    @ParameterizedTest
    @MethodSource("orderStatusWithSubstatusesWithFeeCancellationFromDelivered")
    void testGetDeliveryEventTypesForTrantimesCancelledDeliveryToCustomerCancellation(MbiOrderStatus newStatus) {
        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setFulfilment(true)
                .setCrossborder(false)
                .setMarketDelivery(true)
                .setSubstatus(OrderSubstatus.USER_CHANGED_MIND)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, true,
                false);

        assertEquals(3, result.size());
        assertTrue(result.contains(DeliveryEventType.FEE_CANCELLATION));
        assertTrue(result.contains(DeliveryEventType.DELIVERY_TO_CUSTOMER_CANCELLATION));
        assertTrue(result.contains(DeliveryEventType.CROSSREGIONAL_DELIVERY_CANCELLATION));

    }

    // При отмене заказа (CANCELLED_IN_DELIVERY) из статуса Delivery по вине партнёра -
    // добавляем трантаймы  DELIVERY_TO_CUSTOMER_RETURN и DELIVERY_TO_CUSTOMER
    @DisplayName("Тест: newStatus = CANCELLED_IN_DELIVERY & fromDeliveryStatus == true & order.getFulfilment == true " +
            "& order.getCrossborder == false")
    @ParameterizedTest
    @MethodSource("orderStatusWithSubstatusesWithFeeCancellationFromDelivered")
    void testGetDeliveryEventTypesForTrantimesCancelledDeliveryToCustomerCancellationFromDeliveryStatus(MbiOrderStatus newStatus,
                                                                                                        OrderSubstatus substatus) {
        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setFulfilment(true)
                .setCrossborder(false)
                .setMarketDelivery(true)
                .setStatus(CANCELLED_IN_DELIVERY)
                .setSubstatus(substatus)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, false,
                true);

        assertEquals(2, result.size());
        assertTrue(result.contains(DeliveryEventType.DELIVERY_TO_CUSTOMER_RETURN));
        assertTrue(result.contains(DeliveryEventType.DELIVERY_TO_CUSTOMER));
    }

    // При отмене заказа (CANCELLED_IN_DELIVERY) из статуса Delivered по вине партнёра -
    // добавляем трантаймы  DELIVERY_TO_CUSTOMER_RETURN
    @DisplayName("Тест: newStatus = CANCELLED_IN_DELIVERY & fromDeliveredStatus == true & order.getFulfilment == true" +
            " & order.getCrossborder == false")
    @ParameterizedTest
    @MethodSource("orderStatusWithSubstatusesWithFeeCancellationFromDelivered")
    void testGetDeliveryEventTypesForTrantimesCancelledDeliveryToCustomerCancellationFromDeliveredStatus(MbiOrderStatus newStatus,
                                                                                                         OrderSubstatus substatus) {
        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setFulfilment(true)
                .setCrossborder(false)
                .setMarketDelivery(true)
                .setStatus(CANCELLED_IN_DELIVERY)
                .setSubstatus(substatus)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, true,
                false);

        assertEquals(2, result.size());
        assertTrue(result.contains(DeliveryEventType.DELIVERY_TO_CUSTOMER_RETURN));
        assertTrue(result.contains(DeliveryEventType.FEE_CANCELLATION));
    }

    // При отмене экспресс заказа (CANCELLED_IN_DELIVERY) из статуса DELIVERED не по вине партнёра -
    // должна быть отмена комиссии за размещение и отмена комиссии за экспресс-доставку
    @DisplayName("Тест: newStatus in (CANCELLED_BEFORE_PROCESSING, CANCELLED_IN_PROCESSING, CANCELLED_IN_DELIVERY) & " +
            "fromDeliveredStatus == true & order.getFulfilment == false & order.getCrossborder == false " +
            " & order.isExpress = true")
    @ParameterizedTest
    @MethodSource("orderStatusWithSubstatusesWithFeeCancellationFromDelivered")
    void testGetDeliveryEventTypesForTrantimesCancelledExpressDeliveredCancellation(MbiOrderStatus newStatus) {
        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setFulfilment(false)
                .setCrossborder(false)
                .setMarketDelivery(true)
                .setExpress(true)
                .setSubstatus(OrderSubstatus.USER_CHANGED_MIND)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, true,
                false);

        assertEquals(2, result.size());
        assertTrue(result.contains(DeliveryEventType.FEE_CANCELLATION));
        assertTrue(result.contains(DeliveryEventType.EXPRESS_DELIVERED_CANCELLATION));
    }

    // При отмене экспресс заказа (CANCELLED_IN_DELIVERY) из статуса Delivered по вине партнёра -
    // добавляем трантаймы FEE_CANCELLATION
    // EXPRESS_CANCELLED_BY_PARTNER не добавляем, так как заказ перешёл из статуса Delivered
    @DisplayName("Тест: newStatus = CANCELLED_IN_DELIVERY & fromDeliveredStatus == true & order.getFulfilment == " +
            "false" +
            " & order.getCrossborder == false & order.isExpress = true")
    @ParameterizedTest
    @MethodSource("orderStatusWithSubstatusesWithFeeCancellationFromDelivered")
    void testGetDeliveryEventTypesForTrantimesCancelledFromDeliveredStatusIsExpress(MbiOrderStatus newStatus,
                                                                                    OrderSubstatus substatus) {
        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setFulfilment(false)
                .setCrossborder(false)
                .setMarketDelivery(true)
                .setExpress(true)
                .setStatus(CANCELLED_IN_DELIVERY)
                .setSubstatus(substatus)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, true,
                false);

        assertEquals(1, result.size());
        assertTrue(result.contains(DeliveryEventType.FEE_CANCELLATION));
    }

    // При отмене экспресс заказа (CANCELLED_IN_PROCESSING) из статуса Delivered по вине партнёра -
    // добавляем трантаймы CANCELLED_ORDER_FEE, FEE_CANCELLATION
    // EXPRESS_CANCELLED_BY_PARTNER не добавляем, так как заказ перешёл из статуса Delivered
    @DisplayName("Тест: newStatus = CANCELLED_IN_PROCESSING & fromDeliveredStatus == true & order.getFulfilment == " +
            "false" +
            " & order.getCrossborder == false & order.isExpress = true")
    @ParameterizedTest
    @MethodSource("orderStatusWithSubstatusesWithFeeCancellationFromDelivered")
    void testGetDeliveryEventTypesForTrantimesCancelledInProcessingStatusIsExpress(MbiOrderStatus newStatus,
                                                                                   OrderSubstatus substatus) {
        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setFulfilment(false)
                .setCrossborder(false)
                .setMarketDelivery(true)
                .setExpress(true)
                .setStatus(CANCELLED_IN_PROCESSING)
                .setSubstatus(substatus)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, true,
                false);

        assertEquals(2, result.size());
        assertTrue(result.contains(DeliveryEventType.CANCELLED_ORDER_FEE));
        assertTrue(result.contains(DeliveryEventType.FEE_CANCELLATION));
    }

    // При отмене экспресс заказа (CANCELLED_IN_PROCESSING) из статуса Delivery по вине партнёра -
    // добавляем трантаймы CANCELLED_ORDER_FEE, EXPRESS_CANCELLED_BY_PARTNER
    @DisplayName("Тест: newStatus = CANCELLED_IN_PROCESSING & fromDeliveredStatus == false & fromDeliveryStatus = " +
            "true" +
            " & order.getFulfilment == false & order.getCrossborder == false & order.isExpress = true")
    @ParameterizedTest
    @MethodSource("orderStatusWithSubstatusesWithFeeCancellationFromDelivered")
    void testGetDeliveryEventTypesForTrantimesCancelledInProcessingStatusIsExpressFromDelivery(MbiOrderStatus newStatus,
                                                                                               OrderSubstatus substatus) {
        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(1L, 1L).build())
                .setFulfilment(false)
                .setCrossborder(false)
                .setMarketDelivery(true)
                .setExpress(true)
                .setStatus(CANCELLED_IN_PROCESSING)
                .setSubstatus(substatus)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, false,
                true);

        assertEquals(2, result.size());
        assertTrue(result.contains(DeliveryEventType.CANCELLED_ORDER_FEE));
        assertTrue(result.contains(DeliveryEventType.EXPRESS_CANCELLED_BY_PARTNER));
    }

    // Проверяем, что добавляется услуга Магистраль
    @DisplayName("Тест: newStatus == DELIVERED & order.isZdravCityOrder == false & order.isDropShipBySellerOrder == " +
            "false & order.isCrossborder == false & isNotSortingCenter(order) == false & order.isMarketDelivery == " +
            "true")
    @Test
    void testGetDeliveryEventTypesForDeliveredStatusDifferentAreasIsMarketDelivery() {
        MbiOrder order = getMbiOrderBuilder()
                .setOrderDelivery(getOrderDeliveryBuilder(101L, 202L).build())
                .setMarketDelivery(true)
                .setShopId(1L)
                .setFulfilment(true)
                .setDeliveryPartnerType(OrderDeliveryPartnerType.YANDEX_MARKET)
                .setCrossborder(false)
                .build();

        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, DELIVERED, true,
                false);

        assertEquals(4, result.size());
        assertTrue(result.contains(DeliveryEventType.FEE));
        assertTrue(result.contains(DeliveryEventType.DELIVERY_TO_CUSTOMER));
        assertTrue(result.contains(DeliveryEventType.CROSSREGIONAL_DELIVERY));
        assertTrue(result.contains(DeliveryEventType.FF_PROCESSING));

    }

    // При отмене заказа (CANCELLED_IN_DELIVERY) из статуса DELIVERED не по вине партнёра, в разных
    // федеральных округа, учитывая, что если заказ перешёл в статус DELIVERED до 1-го ноября,
    // то трантайм для отмены услуги Магистраль не создается
    // создаем для отмены комиссии за размещение и отмены комиссии за доставку
    @DisplayName("Тест: newStatus in (CANCELLED_BEFORE_PROCESSING, CANCELLED_IN_PROCESSING, CANCELLED_IN_DELIVERY) & " +
            "fromDeliveredStatus == true & order.getFulfilment == true & order.getCrossborder == false")
    @ParameterizedTest
    @MethodSource("orderStatusWithSubstatusesWithFeeCancellationFromDelivered")
    void testGetDeliveryEventTypesForTrantimesCancelledDifferentAreasDeliveredTimeBeforeFirstNovember(MbiOrderStatus newStatus) {
        MbiOrder order = getMbiOrderBuilder(DateTimes.asDate(LocalDate.of(2021, 10, 28)))
                .setOrderDelivery(getOrderDeliveryBuilder(101L, 202L).build())
                .setFulfilment(true)
                .setCrossborder(false)
                .setMarketDelivery(true)
                .setSubstatus(OrderSubstatus.USER_CHANGED_MIND)
                .build();

        Mockito.when(orderTrantimeDao.isExistTrantimeForOrderByServiceType(order.getId(),
                        DeliveryEventType.CROSSREGIONAL_DELIVERY))
                .thenReturn(false);


        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, true,
                false);

        assertEquals(2, result.size());
        assertTrue(result.contains(DeliveryEventType.FEE_CANCELLATION));
        assertTrue(result.contains(DeliveryEventType.DELIVERY_TO_CUSTOMER_CANCELLATION));
    }

    // При отмене заказа (CANCELLED_IN_DELIVERY) из статуса DELIVERED не по вине партнёра, в разных
    // федеральных округа, учитывая, что если заказ был создан до 1-го ноября и заказ перешёл в статус DELIVERED
    // после 1-го ноября,
    // то создаем трантайм для отмены услуги Магистраль, для отмены комиссии за размещение
    // и отмены комиссии за доставку
    @DisplayName("Тест: newStatus in (CANCELLED_BEFORE_PROCESSING, CANCELLED_IN_PROCESSING, CANCELLED_IN_DELIVERY) & " +
            "fromDeliveredStatus == true & order.getFulfilment == true & order.getCrossborder == false")
    @ParameterizedTest
    @MethodSource("orderStatusWithSubstatusesWithFeeCancellationFromDelivered")
    void testGetDeliveryEventTypesForTrantimesCancelledDifferentAreasDeliveredTimeAfterFirstNovember(MbiOrderStatus newStatus) {
        MbiOrderItem orderItem = new MbiOrderItem.Builder()
                .setWarehouseId(10)
                .setFfSupplierId(301L)
                .build();
        MbiOrder order = getMbiOrderBuilder(DateTimes.asDate(LocalDate.of(2021, 10, 28)))
                .setOrderDelivery(getOrderDeliveryBuilder(101L, 202L).build())
                .setFulfilment(true)
                .setCrossborder(false)
                .setMarketDelivery(true)
                .setSubstatus(OrderSubstatus.USER_CHANGED_MIND)
                .setItems(List.of(orderItem))
                .build();

        Mockito.when(orderTrantimeDao.isExistTrantimeForOrderByServiceType(
                order.getId(),
                DeliveryEventType.CROSSREGIONAL_DELIVERY)
        ).thenReturn(true);


        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, true,
                false);

        assertEquals(3, result.size());
        assertTrue(result.contains(DeliveryEventType.FEE_CANCELLATION));
        assertTrue(result.contains(DeliveryEventType.DELIVERY_TO_CUSTOMER_CANCELLATION));
        assertTrue(result.contains(DeliveryEventType.CROSSREGIONAL_DELIVERY_CANCELLATION));
    }

    //При переходе в DELIVERY заказа с рассрочкой добавляем трантайм INSTALLMENT
    @DisplayName("Тест: newStatus = DELIVERY & order.getPaymentSubmethod == INSTALLMENT")
    @ParameterizedTest
    @MethodSource("orderStatusWithPaymentSubmethodFromDelivery")
    void testGetDeliveryEventTypesForTrantimesInstallmentDelivery(MbiOrderStatus newStatus,
                                                                  PaymentSubmethod paymentSubmethod) {
        MbiOrder order = getMbiOrderBuilder()
                .setStatus(newStatus)
                .setPaymentMethod(PaymentMethod.TINKOFF_INSTALLMENTS)
                .setPaymentSubmethod(paymentSubmethod)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, false, false);

        assertEquals(1, result.size());
        assertTrue(result.contains(DeliveryEventType.INSTALLMENT));
    }

    @Test
    @DisplayName("Тест: newStatus = DELIVERY & order.getPaymentSubmethod == INSTALLMENT for BNPL")
    void testGetDeliveryEventTypesForTrantimesInstallmentDelivery() {
        MbiOrder order = getMbiOrderBuilder()
                .setStatus(DELIVERY)
                .setPaymentMethod(PaymentMethod.YANDEX)
                .setPaymentSubmethod(PaymentSubmethod.BNPL)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, DELIVERY, false, false);

        assertEquals(1, result.size());
        assertTrue(result.contains(DeliveryEventType.INSTALLMENT));
    }

    @Test
    @DisplayName("Тест: newStatus = DELIVERY & order.getPaymentSubmethod != INSTALLMENT")
    void testGetDeliveryEventTypesForTrantimesInstallmentDeliveryIncorrect() {
        MbiOrder order = getMbiOrderBuilder()
                .setStatus(DELIVERY)
                .setPaymentMethod(PaymentMethod.YANDEX)
                .setPaymentSubmethod(PaymentSubmethod.TINKOFF_INSTALLMENTS_6)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, DELIVERY, false, false);

        assertEquals(0, result.size());
    }

    //Магазин сам отменил INSTALLMENT заказ между статусом PROCCESSING и DELIVERY - биллим услугу как штраф
    @DisplayName("Тест: newStatus = CANCELLED_IN_PROCESSING & order.getPaymentSubmethod == INSTALLMENT for BLUE")
    @ParameterizedTest
    @MethodSource("orderStatusWithPaymentSubmethodCancelledInProcessing")
    void testGetDeliveryEventTypesForTrantimesInstallmentFineBlue(PaymentSubmethod paymentSubmethod,
                                                                  OrderSubstatus substatus) {
        MbiOrder order = getMbiOrderBuilder()
                .setStatus(CANCELLED_IN_PROCESSING)
                .setPaymentMethod(PaymentMethod.TINKOFF_INSTALLMENTS)
                .setPaymentSubmethod(paymentSubmethod)
                .setSubstatus(substatus)
                .setColor(Color.BLUE)
                .setFulfilment(false)
                .setMarketDelivery(true)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, order.getStatus(),
                false, false);

        assertEquals(2, result.size());
        assertTrue(result.contains(DeliveryEventType.CANCELLED_ORDER_FEE));
        assertTrue(result.contains(DeliveryEventType.INSTALLMENT_FINE));
    }

    //Магазин сам отменил INSTALLMENT заказ между статусом PROCCESSING и DELIVERY - биллим услугу как штраф
    @DisplayName("Тест: newStatus = CANCELLED_IN_PROCESSING & order.getPaymentSubmethod == INSTALLMENT for BLUE for " +
            "BNPL")
    @ParameterizedTest
    @MethodSource("orderStatusWithPaymentSubmethodCancelledInProcessingForBnpl")
    void testGetDeliveryEventTypesForTrantimesInstallmentFineBlueForBnpl(PaymentSubmethod paymentSubmethod,
                                                                         OrderSubstatus substatus) {
        MbiOrder order = getMbiOrderBuilder()
                .setStatus(CANCELLED_IN_PROCESSING)
                .setPaymentMethod(PaymentMethod.YANDEX)
                .setPaymentSubmethod(paymentSubmethod)
                .setSubstatus(substatus)
                .setColor(Color.BLUE)
                .setFulfilment(false)
                .setMarketDelivery(true)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, order.getStatus(),
                false, false);

        assertEquals(2, result.size());
        assertTrue(result.contains(DeliveryEventType.CANCELLED_ORDER_FEE));
        assertTrue(result.contains(DeliveryEventType.INSTALLMENT_FINE));
    }

    //Магазин сам отменил INSTALLMENT заказ между статусом PROCCESSING и DELIVERY - биллим услугу как штраф
    @DisplayName("Тест: newStatus = CANCELLED_IN_PROCESSING & order.getPaymentSubmethod == INSTALLMENT for Dsbs")
    @ParameterizedTest
    @MethodSource("orderStatusWithPaymentSubmethodCancelledInProcessing")
    void testGetDeliveryEventTypesForTrantimesInstallmentFineDsbs(PaymentSubmethod paymentSubmethod,
                                                                  OrderSubstatus substatus) {
        MbiOrder order = getMbiOrderBuilder()
                .setStatus(CANCELLED_IN_PROCESSING)
                .setPaymentMethod(PaymentMethod.TINKOFF_INSTALLMENTS)
                .setPaymentSubmethod(paymentSubmethod)
                .setSubstatus(substatus)
                .setColor(Color.WHITE)
                .setFulfilment(false)
                .setDeliveryPartnerType(OrderDeliveryPartnerType.SHOP)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, order.getStatus(),
                false, false);

        assertEquals(2, result.size());
        assertTrue(result.contains(DeliveryEventType.CANCELLED_ORDER_FEE));
        assertTrue(result.contains(DeliveryEventType.INSTALLMENT_FINE));
    }

    @DisplayName("Тест: newStatus = CANCELLED_IN_PROCESSING & order.getPaymentSubmethod == INSTALLMENT for Dsbs")
    @ParameterizedTest
    @MethodSource("orderStatusWithPaymentSubmethodCancelledInProcessingForBnpl")
    void testGetDeliveryEventTypesForTrantimesInstallmentFineDsbsForBnpl(PaymentSubmethod paymentSubmethod,
                                                                         OrderSubstatus substatus) {
        MbiOrder order = getMbiOrderBuilder()
                .setStatus(CANCELLED_IN_PROCESSING)
                .setPaymentMethod(PaymentMethod.YANDEX)
                .setPaymentSubmethod(paymentSubmethod)
                .setSubstatus(substatus)
                .setColor(Color.WHITE)
                .setFulfilment(false)
                .setDeliveryPartnerType(OrderDeliveryPartnerType.SHOP)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, order.getStatus(),
                false, false);

        assertEquals(2, result.size());
        assertTrue(result.contains(DeliveryEventType.CANCELLED_ORDER_FEE));
        assertTrue(result.contains(DeliveryEventType.INSTALLMENT_FINE));
    }

    //DELIVERED не состоялся по причине отмены покупателя, невыкупа, отмены по вине СД, отмены по вине Маркета -
    // откатываем комиссию
    @DisplayName("Тест: newStatus = CANCELLED_IN_DELIVERY & order.getPaymentSubmethod == INSTALLMENT")
    @ParameterizedTest
    @MethodSource("orderStatusWithPaymentSubmethodCancelledInDelivery")
    void testGetDeliveryEventTypesForTrantimesInstallmentCancelledInDelivery(MbiOrderStatus newStatus,
                                                                             PaymentSubmethod paymentSubmethod) {
        MbiOrder order = getMbiOrderBuilder()
                .setStatus(newStatus)
                .setPaymentMethod(PaymentMethod.TINKOFF_INSTALLMENTS)
                .setPaymentSubmethod(paymentSubmethod)
                .setSubstatus(OrderSubstatus.DELIVERY_SERVICE_UNDELIVERED)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, false, false);

        assertEquals(1, result.size());
        assertTrue(result.contains(DeliveryEventType.INSTALLMENT_CANCELLATION));
    }

    @DisplayName("Тест: newStatus = CANCELLED_IN_DELIVERY & order.getPaymentSubmethod == INSTALLMENT for BNPL")
    @ParameterizedTest
    @MethodSource("orderStatusWithPaymentSubmethodCancelledInDeliveryForBnpl")
    void testGetDeliveryEventTypesForTrantimesInstallmentCancelledInDeliveryForBnpl(MbiOrderStatus newStatus,
                                                                                    PaymentSubmethod paymentSubmethod) {
        MbiOrder order = getMbiOrderBuilder()
                .setStatus(newStatus)
                .setPaymentMethod(PaymentMethod.YANDEX)
                .setPaymentSubmethod(paymentSubmethod)
                .setSubstatus(OrderSubstatus.DELIVERY_SERVICE_UNDELIVERED)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, false, false);

        assertEquals(1, result.size());
        assertTrue(result.contains(DeliveryEventType.INSTALLMENT_CANCELLATION));
    }

    //если заказ переходит из статуса DELIVERED в CANCELED, то откатываем комиссию по аналогии с fee
    @DisplayName("Тест: newStatus = CANCELLED_IN_DELIVERY (from DELIVERED) & order.getPaymentSubmethod == INSTALLMENT")
    @ParameterizedTest
    @MethodSource("orderStatusWithPaymentSubmethodCancelledInDelivery")
    void testGetDeliveryEventTypesForTrantimesInstallmentCancelledFromDelivered(MbiOrderStatus newStatus,
                                                                                PaymentSubmethod paymentSubmethod) {
        MbiOrder order = getMbiOrderBuilder()
                .setStatus(newStatus)
                .setPaymentMethod(PaymentMethod.TINKOFF_INSTALLMENTS)
                .setPaymentSubmethod(paymentSubmethod)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, true, false);

        assertEquals(2, result.size());
        assertTrue(result.contains(DeliveryEventType.INSTALLMENT_CANCELLATION));
    }

    @DisplayName("Тест: newStatus = CANCELLED_IN_DELIVERY (from DELIVERED) & order.getPaymentSubmethod == INSTALLMENT")
    @ParameterizedTest
    @MethodSource("orderStatusWithPaymentSubmethodCancelledInDeliveryForBnpl")
    void testGetDeliveryEventTypesForTrantimesInstallmentCancelledFromDeliveredForBnpl(MbiOrderStatus newStatus,
                                                                                       PaymentSubmethod paymentSubmethod) {
        MbiOrder order = getMbiOrderBuilder()
                .setStatus(newStatus)
                .setPaymentMethod(PaymentMethod.YANDEX)
                .setPaymentSubmethod(paymentSubmethod)
                .build();
        Set<DeliveryEventType> result = deliveryEventTypeService.getDeliveryEventTypes(order, newStatus, true, false);

        assertEquals(2, result.size());
        assertTrue(result.contains(DeliveryEventType.INSTALLMENT_CANCELLATION));
    }

    private MbiOrderBuilder getMbiOrderBuilder() {
        return new MbiOrderBuilder()
                .setCreationDate(new Date())
                .setStatus(PROCESSING)
                .setColor(Color.BLUE)
                .setTrantime(new Date());
    }

    private MbiOrderBuilder getMbiOrderBuilder(Date creationDate) {
        return new MbiOrderBuilder()
                .setCreationDate(creationDate)
                .setStatus(PROCESSING)
                .setColor(Color.BLUE)
                .setTrantime(new Date());
    }

    private OrderDeliveryBuilder getOrderDeliveryBuilder(Long regionFrom, Long regionTo) {
        return new OrderDeliveryBuilder().setRoute(getDeliverRoute(regionFrom, regionTo));
    }

    private DeliveryRoute getDeliverRoute(Long regionFrom, Long regionTo) {
        return new DeliveryRoute(regionFrom, regionTo);
    }
}
