package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.balance.model.notifications.PaymentNotification;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.NullifyCashbackEmitRequest;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.cashier.CashierService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.CashbackTestProvider;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.balance.TrustMockConfigurer;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkout.util.balance.checkers.CreateProductParams;
import ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.pay.PaymentTestHelper.MARKET_PARTNER_ID;
import static ru.yandex.market.checkout.checkouter.pay.strategies.CashbackEmitPaymentStrategyImpl.CASH_BACK_EMIT_PRODUCT_ID;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_EMIT_CASHBACK;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CHECK_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_REFUND_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.DO_REFUND_STUB;
import static ru.yandex.market.checkout.util.balance.checkers.CreateRefundParams.refund;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkCreateRefundCall;

/**
 * @author : poluektov
 * date: 2020-09-14.
 */
public class CashbackEmitPaymentTest extends AbstractWebTestBase {

    private static final String EMIT_ORDER_ID = "emitOrderId";

    @Autowired
    OrderPayHelper orderPayHelper;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private OrderStatusHelper orderStatusHelper;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private CashierService cashierService;
    @Autowired
    private TrustMockConfigurer trustMockConfigurer;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ReturnService returnService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private RefundHelper refundHelper;
    private Order order;

    @BeforeEach
    public void createOrder() throws Exception {
        var properties = CashbackTestProvider.defaultCashbackParameters();
        properties.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        var cart = orderCreateHelper.cart(properties);
        var checkout = orderCreateHelper.checkout(cart, properties);
        order = checkout.getCarts().get(0);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
    }

    @Test
    public void testOrderMatcher() {
        order = orderService.getOrder(order.getId());
        assertTrue(PaymentMatchers.orderContainsCashbackEmit(order));
    }

    @Test
    public void testScheduleQC() {
        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));
    }

    @Test
    public void testQCCreatePayment() {
        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));
        queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, order.getId());
        var payments = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM,
                PaymentGoal.CASHBACK_EMIT);
        assertEquals(1, payments.size());
    }

    @Test
    public void testQCCreatePaymentForUnknownUid() throws Exception {
        //Создаем заказ с muid-ом
        var params = CashbackTestProvider.defaultCashbackParameters();
        params.getBuyer().setUid(1152921505039173605L);
        params.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        var cart = orderCreateHelper.cart(params);
        var checkout = orderCreateHelper.checkout(cart, params);
        order = checkout.getCarts().get(0);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        order = orderService.getOrder(order.getId());

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));
        queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, order.getId());
        var payments = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM,
                PaymentGoal.CASHBACK_EMIT);

        //QC завершился без создания платежа.
        assertEquals(0, payments.size());
        assertFalse(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));
    }

    @Test
    public void testPaymentCreation() {
        trustMockConfigurer.mockWholeTrust();

        var payment = cashierService.createAndBindCashbackEmitPayment(order.getId());
        assertEquals(PaymentStatus.IN_PROGRESS, payment.getStatus());
        assertEquals(Currency.RUR, payment.getCurrency());
        assertEquals(new BigDecimal("100.00"), payment.getTotalAmount());
        assertEquals(PaymentGoal.CASHBACK_EMIT, payment.getType());
        assertEquals(order.getId(), payment.getOrderId());
        assertNotNull(payment.getStatusExpiryDate());
    }

    @Disabled // сломался при обновлении PG 10->12
    @Test
    public void testCashbackRefundCreation() {
        order = orderService.getOrder(order.getId());
        var cashbackEmitPayment = createAndClearCashbackPayment();
        createFullOrderReturn(cashbackEmitPayment);

        refundHelper.proceedAsyncRefunds(order.getId());
        var refunds = refundService.getRefunds(cashbackEmitPayment);
        assertEquals(1, refunds.size());
        var refund = refunds.iterator().next();
        assertEquals(cashbackEmitPayment.getTotalAmount(), refund.getAmount());

        var receipts = receiptService.findByRefund(refund);
        assertEquals(1, receipts.size());
        var receipt = receipts.iterator().next();
        assertEquals(ReceiptStatus.GENERATED, receipt.getStatus());
        assertEquals(ReceiptType.INCOME_RETURN, receipt.getType());
        assertEquals(1, receipt.getItems().size());
        var receiptItem = receipt.getItems().iterator().next();
        assertEquals(cashbackEmitPayment.getTotalAmount(), receiptItem.getAmount());
        assertEquals(1, receiptItem.getCount());
        assertEquals(cashbackEmitPayment.getTotalAmount(), receiptItem.getPrice());

        checkTrustRefundCalls(cashbackEmitPayment);
    }

    @Test
    public void testManualNullifyCashbackEmit() throws Exception {
        createOrder();

        var nullifyCashbackEmitRequest = new NullifyCashbackEmitRequest(Collections.singleton(order.getId()));

        var response = client.nullifyCashbackEmit(new RequestClientInfo(ClientRole.SYSTEM, null),
                nullifyCashbackEmitRequest);
        assertTrue(response.getFailedOrderIds().isEmpty());
        assertThat(response.getSucceededOrderIds(), hasItem(is(order.getId())));

        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertFalse(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));
    }

    @Test
    @SuppressWarnings("checkstyle:HiddenField")
    public void shouldEmitCashbackForPostpaidOrderWithoutPayment() throws Exception {
        var parameters = CashbackTestProvider.defaultCashbackParameters();
        parameters.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        var cart = orderCreateHelper.cart(parameters);
        var checkout = orderCreateHelper.checkout(cart, parameters);
        var order = checkout.getOrders().get(0);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        order = orderService.getOrder(order.getId());

        assertNull(order.getPayment());

        queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, order.getId());

        var cashbackEmitPayments = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM,
                PaymentGoal.CASHBACK_EMIT);
        assertEquals(1, cashbackEmitPayments.size());
    }

    private void checkTrustRefundCalls(Payment payment) {
        var iterator = trustMockConfigurer.eventsIterator();
        //return validation
        TrustCallsChecker.skipTrustCall(iterator, CHECK_BASKET_STUB);
        //resume return
        TrustCallsChecker.skipTrustCall(iterator, CHECK_BASKET_STUB);
        //prepare cashback refund
        TrustCallsChecker.skipTrustCall(iterator, CHECK_BASKET_STUB);
        //create Items refund
        if (refundHelper.isAsyncRefundStrategyEnabled(PaymentGoal.CASHBACK_EMIT)) {
            TrustCallsChecker.skipTrustCall(iterator, CHECK_BASKET_STUB);
        }
        TrustCallsChecker.skipTrustCall(iterator, CHECK_BASKET_STUB);
        TrustCallsChecker.skipTrustCall(iterator, CREATE_REFUND_STUB);
        //create Cashback refund
        checkCreateRefundCall(iterator, order.getUid(), refund(payment.getBasketKey())
                .withUserIp("127.0.0.1")
                .withReason("Some comment")
                .withRefundLine(
                        EMIT_ORDER_ID,
                        BigDecimal.ZERO,
                        payment.getTotalAmount()
                )
        );
        //start 1st refund
        TrustCallsChecker.skipTrustCall(iterator, DO_REFUND_STUB);
        //start 2nd refund
        if (refundHelper.isAsyncRefundStrategyEnabled(PaymentGoal.ORDER_PREPAY)) {
            TrustCallsChecker.skipTrustCall(iterator, CHECK_BASKET_STUB);
            TrustCallsChecker.skipTrustCall(iterator, CREATE_REFUND_STUB);
        }
        TrustCallsChecker.skipTrustCall(iterator, DO_REFUND_STUB);

    }

    private Payment createAndClearCashbackPayment() {
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));
        queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, order.getId());
        var cashbackEmitPayment = paymentService.getPayments(order.getId(), ClientInfo.SYSTEM,
                PaymentGoal.CASHBACK_EMIT).iterator().next();
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildPostAuth());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildPostAuth(), null);
        orderPayHelper.notifyPaymentClear(cashbackEmitPayment);
        return cashbackEmitPayment;
    }

    private void createFullOrderReturn(Payment cashbackPayment) {
        var clientInfo = new ClientInfo(ClientRole.REFEREE, 123123L);
        var returnRequest = ReturnProvider.generateFullReturn(order);
        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildCashbackEmitCheckBasket(EMIT_ORDER_ID,
                cashbackPayment.getBasketKey()));
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildCashbackEmitCheckBasket(EMIT_ORDER_ID,
                cashbackPayment.getBasketKey()), null);
        var ret = returnService.initReturn(order.getId(), clientInfo, returnRequest, Experiments.empty());
        ret = returnService.resumeReturn(order.getId(), clientInfo, ret.getId(), ret, true);
        returnService.createAndDoRefunds(ret, order);
    }

    @Test
    public void testReceiptCreation() {
        trustMockConfigurer.mockWholeTrust();

        var payment = cashierService.createAndBindCashbackEmitPayment(order.getId());
        var receipts = receiptService.findByPayment(payment, ReceiptType.INCOME);
        assertEquals(1, receipts.size());

        var receipt = receipts.iterator().next();
        assertEquals(payment.getId(), receipt.getPaymentId());
        assertEquals(1, receipt.getItems().size());
        assertEquals(ReceiptStatus.GENERATED, receipt.getStatus());

        var receiptItem = receipt.getItems().iterator().next();
        assertEquals(new BigDecimal("100.00"), receiptItem.getAmount());
        assertEquals(1, receiptItem.getCount());
        assertEquals(new BigDecimal("100.00"), receiptItem.getPrice());
    }

    @Test
    public void testTrustCalls() {
        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockWholeTrust();
        final var accountId = "yandex_account-w/30b153cc-8e30-58e2-8d1a-1095bc49b915";
        var payment = cashierService.createAndBindCashbackEmitPayment(order.getId());

        long uid = order.getBuyer().getUid();

        var iterator = trustMockConfigurer.eventsIterator();

        TrustCallsChecker.checkListWalletBalanceMethodCall(trustMockConfigurer.eventsGatewayIterator());
        TrustCallsChecker.checkOptionalCreateServiceProductCall(iterator,
                CreateProductParams.product(MARKET_PARTNER_ID, CASH_BACK_EMIT_PRODUCT_ID, CASH_BACK_EMIT_PRODUCT_ID,
                        1));
        TrustCallsChecker.checkTopupCall(iterator, uid, accountId, new BigDecimal("100.00"));
        TrustCallsChecker.checkStatusBasketCall(iterator, payment.getBasketKey().getPurchaseToken());
        TrustCallsChecker.checkPayBasketCall(iterator, uid, payment.getBasketKey());
    }

    @Test
    public void testGetOrdersWithoutPartials() {
        trustMockConfigurer.mockWholeTrust();
        var payment = cashierService.createAndBindCashbackEmitPayment(order.getId());
        order = orderService.getOrder(order.getId());
        assertNull(order.getCashbackEmitInfo());
    }

    @Test
    public void testGetOrdersWithPartials() {
        trustMockConfigurer.mockWholeTrust();

        var payment = cashierService.createAndBindCashbackEmitPayment(order.getId());
        order = orderService.getOrders(List.of(order.getId()), ClientInfo.SYSTEM, null,
                Set.of(OptionalOrderPart.CASHBACK_EMIT_INFO), null)
                .iterator().next();
        assertNotNull(order.getCashbackEmitInfo());
        assertEquals(payment.getStatus(), order.getCashbackEmitInfo().getStatus());
        assertEquals(payment.getTotalAmount(), order.getCashbackEmitInfo().getTotalAmount());
    }

    @Test
    public void clientGetOrdersTest() {
        trustMockConfigurer.mockWholeTrust();
        var payment = cashierService.createAndBindCashbackEmitPayment(order.getId());

        order = client.getOrder(
                new RequestClientInfo(ClientRole.USER, order.getUserClientInfo().getUid()),
                OrderRequest.builder(order.getId()).withPartials(Set.of(OptionalOrderPart.CASHBACK_EMIT_INFO)).build());

        assertNotNull(order.getCashbackEmitInfo());
        assertEquals(payment.getStatus(), order.getCashbackEmitInfo().getStatus());
        assertEquals(payment.getTotalAmount().setScale(0), order.getCashbackEmitInfo().getTotalAmount());
    }

    @Test
    public void shouldEmitCashbackOnDelivery() {
        var orderId = order.getId();
        order = orderService.getOrder(orderId);
        orderStatusHelper.updateOrderStatus(orderId, OrderStatus.DELIVERY, OrderSubstatus.USER_RECEIVED);
        assertThat(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, orderId), is(true));
    }

    /**
     * Здесь специально удаляется qc, который был на USER_RECEIVED, чтобы имитировать поведение на проде во время релиза
     * для тех заказов, которые будут уже в USER_RECEIVED.
     */
    @Test
    public void shouldEmitCashbackOnDelivered() {
        var orderId = order.getId();
        order = orderService.getOrder(orderId);
        orderStatusHelper.updateOrderStatus(orderId, OrderStatus.DELIVERY, OrderSubstatus.USER_RECEIVED);
        assertThat(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, orderId), is(true));
        queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, order.getId());
        queuedCallService.removeOldCompletedCalls(0, 100);
        assertThat(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, orderId), is(false));
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertThat(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, orderId), is(true));
    }

    @Test
    @SuppressWarnings("checkstyle:HiddenField")
    public void testCashbackEmitForPickupOrder() throws Exception {
        var parameters = CashbackTestProvider.defaultCashbackParameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        var cart = orderCreateHelper.cart(parameters);
        var order = orderCreateHelper.checkout(cart, parameters).getCarts().iterator().next();
        var orderId = order.getId();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertThat(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, orderId), is(true));
    }

    @Test
    public void testCashbackEmitWithRetries() {
        order = orderService.getOrder(order.getId());
        long orderId = order.getId();

        bindAndCancelCashbackPayment(orderId);
        bindAndCancelCashbackPayment(orderId);

        var payments = paymentService.getPayments(Collections.singletonList(
                order.getId()),
                ClientInfo.SYSTEM,
                PaymentGoal.CASHBACK_EMIT
        );

        assertEquals(2, payments.size());
        assertTrue(payments.stream().allMatch(Payment::isCancelled));

        orderStatusHelper.updateOrderStatus(orderId, OrderStatus.DELIVERY, OrderSubstatus.USER_RECEIVED);
        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, orderId));

        queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, orderId);

        payments = paymentService.getPayments(Collections.singletonList(
                order.getId()),
                ClientInfo.SYSTEM,
                PaymentGoal.CASHBACK_EMIT
        );

        assertEquals(3, payments.size());
        assertTrue(payments.stream().anyMatch(p -> !p.isCancelled()));

    }

    @Test
    public void testCashbackEmitForExceededRetries() {
        order = orderService.getOrder(order.getId());
        long orderId = order.getId();

        bindAndCancelCashbackPayment(orderId);
        bindAndCancelCashbackPayment(orderId);
        bindAndCancelCashbackPayment(orderId);
        bindAndCancelCashbackPayment(orderId);
        bindAndCancelCashbackPayment(orderId);

        var payments = paymentService.getPayments(Collections.singletonList(
                order.getId()),
                ClientInfo.SYSTEM,
                PaymentGoal.CASHBACK_EMIT
        );

        assertEquals(5, payments.size());
        assertTrue(payments.stream().allMatch(Payment::isCancelled));

        orderStatusHelper.updateOrderStatus(orderId, OrderStatus.DELIVERY, OrderSubstatus.USER_RECEIVED);
        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, orderId));

        queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, orderId);

        payments = paymentService.getPayments(Collections.singletonList(
                order.getId()),
                ClientInfo.SYSTEM,
                PaymentGoal.CASHBACK_EMIT
        );

        assertEquals(5, payments.size());
        assertTrue(payments.stream().allMatch(Payment::isCancelled));

    }

    @Test
    public void testCashbackEmissionQCRetry() {
        order = orderService.getOrder(order.getId());
        long orderId = order.getId();
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildFailCheckBasket(), null);

        Payment payment = cashierService.createAndBindCashbackEmitPayment(orderId);

        assertFalse(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, orderId));

        paymentService.notifyPayment(PaymentNotification.checkPaymentNotification(payment.getId(), false));
        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, orderId));
    }

    /**
     * https://st.yandex-team.ru/MARKETCHECKOUT-25410
     * Проверяем что после рефанда начисляется актуальная сумма кешбэка
     */
    @Test
    public void testCashbackEmitAfterPartialRefund() {
        OrderItem firstItem = OrderItemProvider.getOrderItem();
        OrderItem secondItem = OrderItemProvider.getAnotherOrderItem();
        Parameters parameters = CashbackTestProvider.severalItemsCashbackParameters(firstItem, secondItem);
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        var cart = parameters.getBuiltMultiCart();

        order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        order = orderService.getOrder(order.getId());

        String offerId = secondItem.getOfferId();
        createRefundOfItemByItemId(order, offerId);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), equalTo(OrderStatus.DELIVERED));

        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_EMIT_CASHBACK, order.getId()));
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_EMIT_CASHBACK, order.getId());

        var payments = paymentService.getPayments(Collections.singletonList(order.getId()), ClientInfo.SYSTEM,
                PaymentGoal.CASHBACK_EMIT);

        assertEquals(1, payments.size());
        assertEquals(new BigDecimal("100.00"), payments.get(0).getTotalAmount());
    }

    private void createRefundOfItemByItemId(Order order, String offerId) {
        final RefundableItems refundableItems = refundService.getRefundableItems(order);
        RefundableItem refundableItem = refundableItems.getItems().stream()
                .filter(item -> offerId.equals(item.getOfferId()))
                .findFirst()
                .get();
        RefundableItems itemsToRefund = refundableItems.withItems(Collections.singletonList(refundableItem));
        createRefund(order, order.getPayment(), itemsToRefund);
    }

    private void createRefund(Order order, Payment payment, RefundableItems items) {
        try {
            var refunds = refundService.createRefund(order.getId(), order.getBuyerTotal(), "Just Test",
                    ClientInfo.SYSTEM, RefundReason.ORDER_CANCELLED, payment.getType(), false, items.toRefundItems(),
                    false, null, false);
            refundHelper.proceedAsyncRefunds(refunds);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void bindAndCancelCashbackPayment(long orderId) {
        Payment payment = cashierService.createAndBindCashbackEmitPayment(orderId);
        paymentService.updatePaymentStatusToCancel(payment);

    }
}
