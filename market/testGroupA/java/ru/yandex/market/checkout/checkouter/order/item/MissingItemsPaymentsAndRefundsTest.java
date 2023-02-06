package ru.yandex.market.checkout.checkouter.order.item;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.DeliveryTrack;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstance;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestPatchRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.MissingItemsNotification;
import ru.yandex.market.checkout.checkouter.order.item.removalrules.OrderTotalItemsRemovalRule;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.pay.RefundStatus;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.storage.payment.PaymentReadingDao;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;
import ru.yandex.market.checkout.providers.DeliveryTrackCheckpointProvider;
import ru.yandex.market.checkout.providers.DeliveryTrackMetaProvider;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventReason.ITEMS_NOT_SUPPLIED;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventReason.USER_REQUESTED_REMOVE;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_CASH_PAYMENT;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_OFFSET_ADVANCE_RECEIPT;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_REFUND;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.blueOrderParametersWithCisItems;
import static ru.yandex.market.checkout.test.providers.TrackProvider.TRACK_CODE;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_REFUND_STUB;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.getRequestBodyAsJson;

class MissingItemsPaymentsAndRefundsTest extends MissingItemsAbstractTest {

    @Autowired
    private OrderStatusHelper orderStatusHelper;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private PaymentReadingDao paymentReadingDao;
    @Autowired
    private NotifyTracksHelper notifyTracksHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private RefundHelper refundHelper;
    @Autowired
    private OrderTotalItemsRemovalRule orderTotalItemsRemovalRule;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private PaymentService paymentService;

    static Stream<Arguments> paymentMethods() {
        return Stream.of(
                Arguments.arguments(PaymentMethod.CASH_ON_DELIVERY, true),
                Arguments.arguments(PaymentMethod.CARD_ON_DELIVERY, true),
                Arguments.arguments(PaymentMethod.YANDEX, true),
                Arguments.arguments(PaymentMethod.APPLE_PAY, true),
                Arguments.arguments(PaymentMethod.TINKOFF_CREDIT, true),
                Arguments.arguments(PaymentMethod.TINKOFF_INSTALLMENTS, true),
                Arguments.arguments(PaymentMethod.GOOGLE_PAY, true)
        );
    }

    static Stream<Arguments> missingItemCounts() {
        return Stream.of(
                Arguments.arguments(4, 1),
                Arguments.arguments(4, 2),
                Arguments.arguments(4, 3)
        );
    }

    static Stream<Arguments> missingItemCountsHalf() {
        return Stream.of(
                Arguments.arguments(4, 2)
        );
    }

    private static void checkReturnReceiptMatchOrderDiff(@Nonnull Receipt receipt,
                                                         @Nonnull Order orderBefore,
                                                         @Nonnull Order orderAfter) {
        Map<Long, OrderItem> itemsBefore = orderBefore.getItems().stream()
                .collect(Collectors.toMap(OrderItem::getId, Function.identity()));
        Map<Long, OrderItem> itemsDiff = orderAfter.getItems().stream()
                .filter(itemAfter -> !Objects.equals(
                        itemAfter.getCount(),
                        itemsBefore.get(itemAfter.getId()).getCount()
                ))
                .map(itemAfter -> {
                    OrderItem diffItem = new OrderItem();
                    diffItem.setId(itemAfter.getId());
                    diffItem.setBuyerPrice(itemAfter.getBuyerPrice());
                    diffItem.setCount(itemsBefore.get(itemAfter.getId()).getCount() - itemAfter.getCount());
                    return diffItem;
                })
                .collect(Collectors.toMap(OrderItem::getId, Function.identity()));

        checkReceiptItemsMatchOrderItems(receipt, itemsDiff);
    }

    private static void checkReceiptMatchOrder(@Nonnull Receipt receipt, @Nonnull Order order) {
        BigDecimal deliveryPrice = order.getTotal().subtract(order.getItemsTotal());
        ReceiptItem deliveryReceiptItem = receipt.getItems().stream()
                .filter(ReceiptItem::isDelivery)
                .findFirst().orElse(null);
        if (deliveryPrice.compareTo(BigDecimal.ZERO) == 0) {
            assertNull(deliveryReceiptItem);
        } else {
            assertNotNull(deliveryReceiptItem);
            assertThat(deliveryPrice, comparesEqualTo(deliveryReceiptItem.getPrice()));
        }

        Map<Long, OrderItem> orderItems = order.getItems().stream()
                .collect(Collectors.toMap(OrderItem::getId, Function.identity()));

        checkReceiptItemsMatchOrderItems(receipt, orderItems);
    }

    private static void checkReceiptItemsMatchOrderItems(@Nonnull Receipt receipt,
                                                         @Nonnull Map<Long, OrderItem> orderItems) {
        Map<Long, ReceiptItem> receiptItems = receipt.getItems().stream()
                .filter(ReceiptItem::isOrderItem)
                .collect(Collectors.toMap(ReceiptItem::getItemId, Function.identity()));
        assertEquals(orderItems.keySet(), receiptItems.keySet());
        orderItems.keySet().forEach(itemId -> {
            OrderItem orderItem = orderItems.get(itemId);
            ReceiptItem receiptItem = receiptItems.get(itemId);
            assertEquals(orderItem.getCount(), receiptItem.getCount());
            assertThat(orderItem.getBuyerPrice(), comparesEqualTo(receiptItem.getPrice()));
        });
    }

    @Nonnull
    private static Receipt requireReceiptByType(@Nonnull Collection<Receipt> receipts,
                                                @Nonnull ReceiptType receiptType) {
        Receipt receipt = getReceiptByType(receipts, receiptType);
        assertNotNull(receipt);
        return receipt;
    }

    @Nullable
    private static Receipt getReceiptByType(@Nonnull Collection<Receipt> receipts, @Nonnull ReceiptType receiptType) {
        return receipts.stream()
                .filter(receipt -> receiptType.equals(receipt.getType()))
                .findFirst()
                .orElse(null);
    }

    @ParameterizedTest
    @MethodSource("paymentMethods")
    void receiptsValidationForPrepaidOrders(PaymentMethod paymentMethod,
                                            boolean expectSuccessfulRemoval) throws Exception {
        Order initOrder = createOrderWithTwoItems(paymentMethod, DeliveryType.DELIVERY);
        initOrder = orderStatusHelper.proceedOrderToStatus(initOrder, OrderStatus.PROCESSING);
        OrderEditRequest editRequest = getEditRequestWithOneMissingUnit(initOrder, USER_REQUESTED_REMOVE);
        initOrder = orderStatusHelper.proceedOrderToStatus(initOrder, OrderStatus.DELIVERY);
        addTrackAndCheckpoint(initOrder);

        if (expectSuccessfulRemoval) {

            // MDB уведомляет Чекаутер о ненайденных товарах, создается ChangeRequest на удаление товаров из заказа
            notifyMissingItemsAndExpectItemsRemoval(initOrder.getId(), editRequest);
        } else {

            // MDB уведомляет Чекаутер о ненайденных товарах, создается CancellationRequest
            notifyMissingItemsAndExpectOrderCancellation(initOrder.getId(), editRequest);
            return;
        }

        orderStatusHelper.proceedOrderToStatus(initOrder, OrderStatus.DELIVERED);
        refundHelper.proceedAsyncRefunds(initOrder.getId());

        if (paymentMethod.getPaymentType() == PaymentType.PREPAID) {
            checkPaymentForPrepaidOrder(initOrder);
            checkRefundsForPrepaidOrder(initOrder);
        } else {
            checkPaymentForPostpaidOrder(initOrder);
        }
    }

    private void checkRefundsForPrepaidOrder(Order initOrder) {
        var reducedOrder = orderService.getOrder(initOrder.getId());
        var reducedAmount = initOrder.getBuyerTotal().subtract(reducedOrder.getBuyerTotal());
        assertTrue(reducedAmount.signum() > 0);
        log.info("Order buyer total reduced by: {}", reducedAmount);

        Collection<Refund> refunds = refundService.getRefunds(initOrder.getPayment());
        assertEquals(1, refunds.size());
        assertEquals(reducedAmount, refunds.iterator().next().getAmount());
    }

    @Test
    @DisplayName("Должны корректно посчитать сумму рефанда за отмену заказа, " +
            "если перед этим был рефанд за удаленные из заказа товары")
    void refundValidationForFurtherCancellation() throws Exception {
        Order initOrder = createOrderWithTwoItems(PaymentMethod.YANDEX, DeliveryType.DELIVERY);
        initOrder = orderStatusHelper.proceedOrderToStatus(initOrder, OrderStatus.DELIVERY);
        addTrackAndCheckpoint(initOrder);

        OrderEditRequest editRequest = getEditRequestWithOneMissingUnit(initOrder, USER_REQUESTED_REMOVE);

        // MDB уведомляет Чекаутер о ненайденных товарах, создается ChangeRequest на удаление товаров из заказа
        notifyMissingItemsAndExpectItemsRemoval(initOrder.getId(), editRequest);

        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_SUBSIDY_PAYMENT);

        orderStatusHelper.proceedOrderToStatus(initOrder, OrderStatus.CANCELLED);
        queuedCallService.executeQueuedCallBatch(ORDER_REFUND);

        assertFalse(queuedCallService.existsQueuedCall(ORDER_REFUND, initOrder.getId()));
        Collection<Refund> refunds = refundService.getRefunds(initOrder.getId());
        assertThat(refunds, hasSize(2));
        assertThat(refunds, hasItems(
                allOf(
                        hasProperty("comment", equalTo("Возврат денег за удаленные из заказа товары и услуги")),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(777)))),
                allOf(
                        hasProperty("comment", equalTo("Возврат денег при отмене заказа")),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(7343))))));
    }

    @Test
    @DisplayName("MARKETCHECKOUT-24834 Должны пройти оба рефанда")
    void verifyOrderCanBeReturned() throws Exception {
        orderTotalItemsRemovalRule.setMaxTotalPercentRemovable(BigDecimal.valueOf(99));
        Order initOrder = createOrderWithTwoItems(PaymentMethod.YANDEX, DeliveryType.DELIVERY);
        initOrder = orderStatusHelper.proceedOrderToStatus(initOrder, OrderStatus.DELIVERY);
        addTrackAndCheckpoint(initOrder);

        // создаем запрос на изменение заказа
        OrderEditRequest editRequest = new OrderEditRequest();
        OrderItem itemWithMaxCount = getItemWithMaxCount(initOrder);
        OrderItem itemWithMinCount = getItemWithMinCount(initOrder);
        editRequest.setMissingItemsNotification(
                new MissingItemsNotification(true, List.of(
                        toItemInfo(itemWithMaxCount, 1, false),
                        toItemInfo(itemWithMinCount, itemWithMinCount.getCount(), false)
                ), USER_REQUESTED_REMOVE, true));

        // MDB уведомляет Чекаутер о ненайденных товарах, создается ChangeRequest на удаление товаров из заказа
        notifyMissingItemsAndExpectItemsRemoval(initOrder.getId(), editRequest, ClientRole.SYSTEM);

        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_SUBSIDY_PAYMENT);

        orderStatusHelper.proceedOrderToStatus(initOrder, OrderStatus.CANCELLED);
        queuedCallService.executeQueuedCallBatch(ORDER_REFUND);

        initOrder = orderService.getOrder(initOrder.getId());

        assertFalse(queuedCallService.existsQueuedCall(ORDER_REFUND, initOrder.getId()));
        Collection<Refund> refunds = refundService.getRefunds(initOrder.getId());
        assertThat(refunds, hasSize(2));
        assertThat(refunds, hasItems(
                allOf(
                        hasProperty("comment", equalTo("Возврат денег за удаленные из заказа товары и услуги")),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(6993)))),
                allOf(
                        hasProperty("comment", equalTo("Возврат денег при отмене заказа")),
                        hasProperty("amount", comparesEqualTo(BigDecimal.valueOf(1127))))));
    }


    @ParameterizedTest(name = "Проверяем что при частичном рефанде кизов в траст количество лайнов с balanceOrderId " +
            "совпадает с количеством недостающих в заказе товаров. Когда из {0} товаров в заказе осталось {1}.")
    @MethodSource("missingItemCounts")
    public void cisItemNotSuppliedRefund(int itemCount, int remainedCount) {
        orderTotalItemsRemovalRule.setMaxTotalPercentRemovable(BigDecimal.valueOf(99));

        // given
        var parameters = blueOrderParametersWithCisItems(itemCount, BigDecimal.valueOf(100));
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        // do
        var order = orderCreateHelper.createOrder(parameters);
        var orderId = order.getId();
        var payment = orderPayHelper.payForOrder(order);
        orderPayHelper.notifyPaymentClear(payment);

        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.CLEARED, payment.getStatus());

        order = orderService.getOrder(orderId);
        assertEquals(OrderStatus.PROCESSING, order.getStatus());

        OrderEditRequest editRequest = createOrderEditRequestForRefundByItemNotSupplied(order, itemCount,
                remainedCount);
        createAndUpdateChangeRequest(order, editRequest);

        CheckBasketParams config = CheckBasketParams.buildDividedItems(order);
        trustMockConfigurer.mockCheckBasket(config);
        trustMockConfigurer.mockStatusBasket(config, null);

        var queuedCalls = queuedCallService.findQueuedCallsByOrderId(orderId);
        queuedCalls.forEach(queuedCall ->
                queuedCallService.executeQueuedCallBatch(queuedCall.getCallType()));

        Collection<Refund> refunds = refundService.getRefunds(orderId);
        assertEquals(1, refunds.size());

        var refund = refunds.iterator().next();
        orderPayHelper.notifyRefund(refund);
        refund = refundService.getRefund(refund.getId());
        assertEquals(RefundStatus.SUCCESS, refund.getStatus());

        // check
        List<ServeEvent> serveEvents = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_REFUND_STUB))
                .collect(Collectors.toList());
        assertEquals(1, serveEvents.size());

        ServeEvent createRefund = Iterables.getOnlyElement(serveEvents);
        JsonObject refundBody = getRequestBodyAsJson(createRefund);

        var refundBodyOrders =
                Streams.stream(refundBody.get("orders")
                                .getAsJsonArray()
                                .iterator())
                        .collect(Collectors.toUnmodifiableList());
        assertEquals(itemCount - remainedCount, refundBodyOrders.size());

        var uniqOrderIdField = refundBodyOrders.stream().map(jsonElement ->
                        ((JsonObject) jsonElement).get("order_id").getAsString())
                .collect(Collectors.toSet());
        assertEquals(itemCount - remainedCount, uniqOrderIdField.size());
    }

    @ParameterizedTest(name = "Проверяем что при частичном рефанде кизов в траст количество лайнов с balanceOrderId " +
            "совпадает с количеством недостающих в заказе товаров. Когда из {0} товаров в заказе осталось {1}. " +
            "+ возврат оставшейся части")
    @MethodSource("missingItemCounts")
    public void cisItemNotSuppliedRefundAndFullRefund(int itemCount, int remainedCount) {
        orderTotalItemsRemovalRule.setMaxTotalPercentRemovable(BigDecimal.valueOf(99));

        // given
        var parameters = blueOrderParametersWithCisItems(itemCount, BigDecimal.valueOf(100));
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        // do
        var order = orderCreateHelper.createOrder(parameters);
        var orderId = order.getId();
        var payment = orderPayHelper.payForOrder(order);
        orderPayHelper.notifyPaymentClear(payment);

        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.CLEARED, payment.getStatus());

        order = orderService.getOrder(orderId);
        assertEquals(OrderStatus.PROCESSING, order.getStatus());

        OrderEditRequest editRequest = createOrderEditRequestForRefundByItemNotSupplied(order, itemCount,
                remainedCount);
        createAndUpdateChangeRequest(order, editRequest);

        CheckBasketParams config = CheckBasketParams.buildDividedItems(order);
        trustMockConfigurer.mockCheckBasket(config);
        trustMockConfigurer.mockStatusBasket(config, null);

        var queuedCalls = queuedCallService.findQueuedCallsByOrderId(orderId);
        queuedCalls.forEach(queuedCall ->
                queuedCallService.executeQueuedCallBatch(queuedCall.getCallType()));

        Collection<Refund> refunds = refundService.getRefunds(orderId);
        assertEquals(1, refunds.size());

        var refund = refunds.iterator().next();
        orderPayHelper.notifyRefund(refund);
        refund = refundService.getRefund(refund.getId());
        assertEquals(RefundStatus.SUCCESS, refund.getStatus());

        trustMockConfigurer.mockCheckBasket(config);
        trustMockConfigurer.mockStatusBasket(config, null);

        order = orderService.getOrder(orderId);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);

        refunds = refundService.getRefunds(orderId);
        assertEquals(2, refunds.size());

        // check
        List<ServeEvent> serveEvents = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_REFUND_STUB))
                .collect(Collectors.toList());
        assertEquals(2, serveEvents.size());

        ServeEvent createRefund = serveEvents.get(1);
        JsonObject refundBody = getRequestBodyAsJson(createRefund);

        var refundBodyOrders =
                Streams.stream(refundBody.get("orders")
                                .getAsJsonArray()
                                .iterator())
                        .collect(Collectors.toUnmodifiableList());
        assertEquals(remainedCount + 1, refundBodyOrders.size());

        var uniqOrderIdField = refundBodyOrders.stream().map(jsonElement ->
                        ((JsonObject) jsonElement).get("order_id").getAsString())
                .collect(Collectors.toSet());
        assertEquals(remainedCount + 1, uniqOrderIdField.size());

    }

    @ParameterizedTest(name = "Проверяем что при частичном рефанде кизов в траст количество лайнов с balanceOrderId " +
            "совпадает с количеством недостающих в заказе товаров. Когда из {0} товаров в заказе осталось {1}. " +
            "+ еще одна отмена + возврат оставшейся части")
    @MethodSource("missingItemCountsHalf")
    public void cisItemNotSuppliedRefundAndOneMoreAndFullRefund(int itemCount, int remainedCount) {
        orderTotalItemsRemovalRule.setMaxTotalPercentRemovable(BigDecimal.valueOf(99));

        // given
        var parameters = blueOrderParametersWithCisItems(itemCount, BigDecimal.valueOf(100));
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        // do
        var order = orderCreateHelper.createOrder(parameters);
        var orderId = order.getId();
        Long itemId = order.getItems().iterator().next().getId();
        var payment = orderPayHelper.payForOrder(order);
        orderPayHelper.notifyPaymentClear(payment);

        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        assertEquals(PaymentStatus.CLEARED, payment.getStatus());

        order = orderService.getOrder(orderId);
        assertEquals(OrderStatus.PROCESSING, order.getStatus());

        CheckBasketParams config = CheckBasketParams.buildDividedItems(order);


        // первый запрос на изменение
        OrderEditRequest editRequest1 = createOrderEditRequestForRefundByItemNotSupplied(order, itemCount,
                remainedCount);
        createAndUpdateChangeRequest(order, editRequest1);

        trustMockConfigurer.mockCheckBasket(config);
        trustMockConfigurer.mockStatusBasket(config, null);

        var queuedCalls = queuedCallService.findQueuedCallsByOrderId(orderId);
        queuedCalls.forEach(queuedCall ->
                queuedCallService.executeQueuedCallBatch(queuedCall.getCallType()));

        Collection<Refund> refunds = refundService.getRefunds(orderId);
        assertEquals(1, refunds.size());

        var refund = refunds.iterator().next();
        orderPayHelper.notifyRefund(refund);
        refund = refundService.getRefund(refund.getId());
        assertEquals(RefundStatus.SUCCESS, refund.getStatus());


        // второй запрос на изменение
        OrderEditRequest editRequest2 = createOrderEditRequestForRefundByItemNotSupplied(order, remainedCount,
                1);
        createAndUpdateChangeRequest(order, editRequest2);

        trustMockConfigurer.mockCheckBasket(config);
        trustMockConfigurer.mockStatusBasket(config, null);

        queuedCalls = queuedCallService.findQueuedCallsByOrderId(orderId);
        queuedCalls.forEach(queuedCall ->
                queuedCallService.executeQueuedCallBatch(queuedCall.getCallType()));

        refunds = refundService.getRefunds(orderId);
        assertEquals(2, refunds.size());

        refund = refunds.stream().filter(other -> RefundStatus.ACCEPTED.equals(other.getStatus())).findFirst().get();
        orderPayHelper.notifyRefund(refund);
        refund = refundService.getRefund(refund.getId());
        assertEquals(RefundStatus.SUCCESS, refund.getStatus());

        // отменяем заказ
        trustMockConfigurer.mockCheckBasket(config);
        trustMockConfigurer.mockStatusBasket(config, null);

        order = orderService.getOrder(orderId);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.PROCESS_REFUND);

        refunds = refundService.getRefunds(orderId);
        assertEquals(3, refunds.size());


        // check
        List<ServeEvent> serveEvents = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_REFUND_STUB))
                .collect(Collectors.toList());
        assertEquals(3, serveEvents.size());

        List<JsonArray> refundsOrders = serveEvents.stream()
                .map(TrustCallsChecker::getRequestBodyAsJson)
                .map(el -> (JsonArray) el.get("orders"))
                .collect(Collectors.toUnmodifiableList());

        Assertions.assertEquals(2, refundsOrders.get(0).size());
        Assertions.assertEquals(1, refundsOrders.get(1).size());
        Assertions.assertEquals(2, refundsOrders.get(2).size());

        for (JsonElement jsonElement : refundsOrders.get(0)) {
            String mOrderId = jsonElement.getAsJsonObject().get("order_id").getAsString();
            assertTrue(mOrderId.endsWith("-1") || mOrderId.endsWith("-2"));
        }

        Assertions.assertTrue(refundsOrders.get(1).get(0).getAsJsonObject()
                .get("order_id").getAsString().endsWith("-3"));

        for (JsonElement jsonElement : refundsOrders.get(2)) {
            String mOrderId = jsonElement.getAsJsonObject().get("order_id").getAsString();
            assertTrue(mOrderId.endsWith("-4") || mOrderId.endsWith("-delivery"));
        }
    }

    private void createAndUpdateChangeRequest(Order order, OrderEditRequest editRequest) {
        var changeRequest = client.editOrder(order.getId(), ClientRole.SYSTEM, null, List.of(Color.BLUE), editRequest);
        queuedCallsHelper.runItemsRefreezeQCProcessor(order.getId(), order.getItems());
        var changeRequestId = changeRequest.iterator().next().getId();
        client.updateChangeRequestStatus(
                order.getId(), changeRequestId, ClientRole.SYSTEM, null, new ChangeRequestPatchRequest(
                        ChangeRequestStatus.APPLIED, null, null
                )
        );
    }

    @NotNull
    private OrderEditRequest createOrderEditRequestForRefundByItemNotSupplied(Order order, int itemCount,
                                                                              int remainedCount) {
        var orderItemInstances = createItemInstance(itemCount, order.getId());
        var itemId = order.getItems().iterator().next().getId();
        var orderItem = order.getItem(itemId);
        var orderInfo = toItemInfoWithInstances(orderItem, remainedCount, orderItemInstances);

        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(
                new MissingItemsNotification(true, List.of(
                        orderInfo
                ), ITEMS_NOT_SUPPLIED, true));
        return editRequest;
    }

    private HashSet<OrderItemInstance> createItemInstance(int count, long orderId) {
        var orderItemInstances = new HashSet<OrderItemInstance>();

        for (int i = 1; i <= count; i++) {
            var balanceOrderId = orderId + "-item-" + orderId + "-" + i;
            var orderItemInstance = new OrderItemInstance();
            orderItemInstance.setBalanceOrderId(balanceOrderId);

            orderItemInstances.add(orderItemInstance);
        }

        return orderItemInstances;
    }

    private void addTrackAndCheckpoint(@Nonnull Order order) throws Exception {
        long parcelId = order.getDelivery().getParcels().iterator().next().getId();
        Track track = new Track(TRACK_CODE, DeliveryProvider.MOCK_DELIVERY_SERVICE_ID);
        TrackCheckpoint cp = new TrackCheckpoint();
        cp.setDeliveryCheckpointStatus(100);
        track.addCheckpoint(cp);
        track.setDeliveryServiceType(DeliveryServiceType.SORTING_CENTER);
        track = orderDeliveryHelper.addTrack(order.getId(), parcelId, track, ClientInfo.SYSTEM,
                new ResultActionsContainer().andExpect(status().is(200)));
        orderUpdateService.updateTrackSetTrackerId(
                order.getId(),
                track.getBusinessId(),
                DeliveryTrackMetaProvider.getDeliveryTrackMeta("any").getId()
        );

        DeliveryTrack deliveryTrack = DeliveryTrackProvider.getDeliveryTrack(
                String.valueOf(order.getId()),
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(1, 100)
        );
        notifyTracksHelper.notifyTracks(deliveryTrack);

        assertFalse(orderService.getOrder(order.getId()).getDelivery().getParcels()
                .iterator().next()
                .getTracks()
                .iterator().next()
                .getCheckpoints().isEmpty());
    }

    private void checkPaymentForPrepaidOrder(@Nonnull Order initOrder) {
        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_OFFSET_ADVANCE_RECEIPT, initOrder.getId()));
        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_OFFSET_ADVANCE_RECEIPT);

        Order reducedOrder = orderService.getOrder(initOrder.getId());
        List<Receipt> receipts = receiptService.findByOrder(reducedOrder.getId());

        assertEquals(3, receipts.size());
        checkReceiptMatchOrder(requireReceiptByType(receipts, ReceiptType.INCOME), initOrder);
        checkReturnReceiptMatchOrderDiff(
                requireReceiptByType(receipts, ReceiptType.INCOME_RETURN),
                initOrder,
                reducedOrder
        );
        checkReceiptMatchOrder(requireReceiptByType(receipts, ReceiptType.OFFSET_ADVANCE_ON_DELIVERED), reducedOrder);
    }

    private void checkPaymentForPostpaidOrder(@Nonnull Order initOrder) {
        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_CASH_PAYMENT, initOrder.getId()));
        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_CASH_PAYMENT);

        Order reducedOrder = orderService.getOrder(initOrder.getId());
        Payment payment = getOrderPostpayPayment(reducedOrder.getId());

        assertThat(payment.getTotalAmount(), comparesEqualTo(reducedOrder.getTotal()));
    }

    private Payment getOrderPostpayPayment(long orderId) {
        List<Payment> payments = paymentReadingDao.loadPaymentsByOrderId(orderId, PaymentGoal.ORDER_POSTPAY);
        assertThat(payments, hasSize(1));
        return payments.iterator().next();
    }
}
