package ru.yandex.market.checkout.checkouter.pay.bnpl;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.client.ClientInfo.SYSTEM;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_RECEIVED;
import static ru.yandex.market.checkout.checkouter.pay.PaymentGoal.CASHBACK_EMIT;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_EMIT_CASHBACK;
import static ru.yandex.market.checkout.providers.BnplTestProvider.bnplAndCashbackParameters;

@DisplayName("Проверяем QC кэшбэка для покупок в Сплите.")
public class BnplEmitCashbackTest extends AbstractWebTestBase {

    @Autowired
    OrderPayHelper orderPayHelper;
    @Autowired
    private OrderStatusHelper orderStatusHelper;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;
    private Order order;

    @BeforeEach
    public void createOrder() throws Exception {
        bnplMockConfigurer.mockWholeBnpl();
        checkouterProperties.setEnableServicesPrepay(true);
        checkouterProperties.setEnableBnpl(true);

        Parameters parameters = bnplAndCashbackParameters();
        order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payWithRealResponse(order);
        order = orderService.getOrder(order.getId());
    }

    @Test
    @DisplayName("Проверяем добавление QC кэшбэка на покупку в Сплите полностью оплаченную ПОСЛЕ доставки.")
    public void emitCashbackByNotifyAfterDelivered() throws IOException {
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertFalse(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        Payment payment = paymentService.getPayment(order.getPaymentId(), SYSTEM);
        orderPayHelper.notifyBnplFinished(payment);
        bnplMockConfigurer.mockGetBnplOrder("orderInfoResponseByCompleted.json");

        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, order.getId());
        var payments = paymentService.getPayments(order.getId(), SYSTEM, CASHBACK_EMIT);
        assertEquals(1, payments.size());
    }

    @Test
    @DisplayName("Проверяем добавление QC кэшбэка на покупку в Сплите полностью оплаченную ДО доставки.")
    public void emitCashbackByDeliveredAfterNotify() throws IOException {
        orderStatusHelper.proceedOrderToStatus(order, DELIVERY);
        Payment payment = paymentService.getPayment(order.getPaymentId(), SYSTEM);
        orderPayHelper.notifyBnplFinished(payment);

        bnplMockConfigurer.mockGetBnplOrder("orderInfoResponseByCompleted.json");
        assertFalse(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, order.getId());
        var payments = paymentService.getPayments(order.getId(), SYSTEM, CASHBACK_EMIT);
        assertEquals(1, payments.size());
    }

    @Test
    @DisplayName("Проверяем добавление QC кэшбэка на покупку в Сплите полностью оплаченную ПОСЛЕ вручения.")
    public void emitCashbackByNotifyAfterReceived() throws IOException {
        orderStatusHelper.proceedOrderToStatus(order, DELIVERY);
        Payment payment = paymentService.getPayment(order.getPaymentId(), SYSTEM);
        client.updateOrderStatus(order.getId(), ClientRole.SYSTEM, 1L, null, DELIVERY, USER_RECEIVED);

        assertFalse(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        orderPayHelper.notifyBnplFinished(payment);
        bnplMockConfigurer.mockGetBnplOrder("orderInfoResponseByCompleted.json");

        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, order.getId());
        var payments = paymentService.getPayments(order.getId(), SYSTEM, CASHBACK_EMIT);
        assertEquals(1, payments.size());
    }

    @Test
    @DisplayName("Проверяем добавление QC кэшбэка на покупку в Сплите полностью оплаченную ДО вручения.")
    public void emitCashbackByReceivedAfterNotify() throws IOException {
        Payment payment = paymentService.getPayment(order.getPaymentId(), SYSTEM);
        orderStatusHelper.proceedOrderToStatus(order, DELIVERY);
        orderPayHelper.notifyBnplFinished(payment);

        bnplMockConfigurer.mockGetBnplOrder("orderInfoResponseByCompleted.json");
        assertFalse(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        client.updateOrderStatus(order.getId(), ClientRole.SYSTEM, 1L, null, DELIVERY, USER_RECEIVED);

        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, order.getId());
        var payments = paymentService.getPayments(order.getId(), SYSTEM, CASHBACK_EMIT);
        assertEquals(1, payments.size());
    }

    @Test
    @DisplayName("Проверяем отсутствие повторного QC кэшбэка на покупку в Сплите после вручения и доставки.")
    public void noDoubleEmitCashbackAfterReceivedAndDelivered() throws IOException {
        Payment payment = paymentService.getPayment(order.getPaymentId(), SYSTEM);
        orderStatusHelper.proceedOrderToStatus(order, DELIVERY);
        orderPayHelper.notifyBnplFinished(payment);

        bnplMockConfigurer.mockGetBnplOrder("orderInfoResponseByCompleted.json");
        assertFalse(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        client.updateOrderStatus(order.getId(), ClientRole.SYSTEM, 1L, null, DELIVERY, USER_RECEIVED);

        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, order.getId());
        var payments = paymentService.getPayments(order.getId(), SYSTEM, CASHBACK_EMIT);
        assertEquals(1, payments.size());
    }

    @Test
    @DisplayName("Проверяем добавление QC кэшбэка на покупку в Сплите после вручения и доставки и частичного рефанда.")
    public void emitCashbackAfterPartiallyRefundedAndDelivered() throws IOException {
        Payment payment = paymentService.getPayment(order.getPaymentId(), SYSTEM);
        orderStatusHelper.proceedOrderToStatus(order, DELIVERY);
        orderPayHelper.notifyBnplFinished(payment);

        bnplMockConfigurer.mockGetBnplOrder("orderInfoResponseByPartiallyRefunded.json");
        assertFalse(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        client.updateOrderStatus(order.getId(), ClientRole.SYSTEM, 1L, null, DELIVERY, USER_RECEIVED);

        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, order.getId());
        var payments = paymentService.getPayments(order.getId(), SYSTEM, CASHBACK_EMIT);
        assertEquals(1, payments.size());
    }

    @Test
    @DisplayName("Проверяем добавление QC кэшбэка на покупку в Сплите после частичного рефанда.")
    public void emitCashbackAfterPartiallyRefunded() throws IOException {
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertFalse(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        Payment payment = paymentService.getPayment(order.getPaymentId(), SYSTEM);
        orderPayHelper.notifyBnplFinished(payment);
        bnplMockConfigurer.mockGetBnplOrder("orderInfoResponseByPartiallyRefunded.json");

        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, order.getId());
        var payments = paymentService.getPayments(order.getId(), SYSTEM, CASHBACK_EMIT);
        assertEquals(1, payments.size());
    }

    @Test
    @DisplayName("Проверяем отсутствие повторного QC кэшбэка на покупку в Сплите после вручения, оплаты и доставки.")
    public void noDoubleEmitCashbackReceivedNotifyDelivered() throws IOException {
        Payment payment = paymentService.getPayment(order.getPaymentId(), SYSTEM);
        orderStatusHelper.proceedOrderToStatus(order, DELIVERY);
        client.updateOrderStatus(order.getId(), ClientRole.SYSTEM, 1L, null, DELIVERY, USER_RECEIVED);

        orderPayHelper.notifyBnplFinished(payment);
        bnplMockConfigurer.mockGetBnplOrder("orderInfoResponseByCompleted.json");
        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));
        queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, order.getId());
        assertFalse(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        var payments = paymentService.getPayments(order.getId(), SYSTEM, CASHBACK_EMIT);
        assertEquals(1, payments.size());
    }

    @Test
    @DisplayName("Проверяем отсутствие повторного QC кэшбэка на покупку в Сплите после вручения, оплаты и доставки.")
    public void noDoubleEmitCashbackReceivedNotifyDelivered1() throws IOException {
        Payment payment = paymentService.getPayment(order.getPaymentId(), SYSTEM);
        orderStatusHelper.proceedOrderToStatus(order, DELIVERY);
        client.updateOrderStatus(order.getId(), ClientRole.SYSTEM, 1L, null, DELIVERY, USER_RECEIVED);

        orderPayHelper.notifyBnplFinished(payment);
        bnplMockConfigurer.mockGetBnplOrder("orderInfoResponseByCompleted.json");
        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));
        queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, order.getId());
        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        assertFalse(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        var payments = paymentService.getPayments(order.getId(), SYSTEM, CASHBACK_EMIT);
        assertEquals(1, payments.size());
    }

    @Test
    @DisplayName("Проверяем, что если в ответ от Сплита BnplOrder = null, то создается, но не выполняется QC на " +
            "кэшбэк.")
    public void missBnplOrderInBnplServiceResponse() throws IOException {
        Payment payment = paymentService.getPayment(order.getPaymentId(), SYSTEM);
        orderStatusHelper.proceedOrderToStatus(order, DELIVERY);
        orderPayHelper.notifyBnplFinished(payment);
        assertFalse(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        bnplMockConfigurer.mockNotOkGetBnplOrder();

        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        try {
            queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, order.getId());
        } catch (RuntimeException ignored) {

        }
        var payments = paymentService.getPayments(order.getId(), SYSTEM, CASHBACK_EMIT);
        assertEquals(0, payments.size());
    }

    @Test
    @DisplayName("Проверяем, что если в ответ от Сплита приходит BnplOrder без поля plan, то создается, но не " +
            "выполняется QC на кэшбэк.")
    public void missPlanInOrderInfoResponse() throws IOException {
        Payment payment = paymentService.getPayment(order.getPaymentId(), SYSTEM);
        orderStatusHelper.proceedOrderToStatus(order, DELIVERY);
        orderPayHelper.notifyBnplFinished(payment);
        assertFalse(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        bnplMockConfigurer.mockGetBnplOrder("orderInfoWithMissPlan.json");

        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        try {
            queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, order.getId());
        } catch (RuntimeException ignored) {

        }
        var payments = paymentService.getPayments(order.getId(), SYSTEM, CASHBACK_EMIT);
        assertEquals(0, payments.size());
    }

    @Test
    @DisplayName("Проверяем что после того, как сервис BNPL восстановился и стал присылать корректный BnplOrder " +
            "выполняется начисление кэшбэка.")
    public void emitCashbackAfterRecoveredBnplService() throws IOException {
        Payment payment = paymentService.getPayment(order.getPaymentId(), SYSTEM);
        orderStatusHelper.proceedOrderToStatus(order, DELIVERY);
        orderPayHelper.notifyBnplFinished(payment);
        assertFalse(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        bnplMockConfigurer.mockNotOkGetBnplOrder();

        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        try {
            queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, order.getId());
        } catch (RuntimeException ignored) {

        }
        var payments = paymentService.getPayments(order.getId(), SYSTEM, CASHBACK_EMIT);
        assertEquals(0, payments.size());

        bnplMockConfigurer.mockGetBnplOrder("orderInfoWithMissPlan.json");

        try {
            queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, order.getId());
        } catch (RuntimeException ignored) {

        }
        payments = paymentService.getPayments(order.getId(), SYSTEM, CASHBACK_EMIT);
        assertEquals(0, payments.size());

        bnplMockConfigurer.mockGetBnplOrder("orderInfoResponseByCompleted.json");

        queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, order.getId());
        payments = paymentService.getPayments(order.getId(), SYSTEM, CASHBACK_EMIT);
        assertEquals(1, payments.size());
    }

    @Test
    @DisplayName("Проверяем что после того, как сервис BNPL восстановился и стал присылать корректный BnplOrder " +
            "для отмененного заказа кэшбэк не начисляется.")
    public void orderCancelled() throws IOException {
        Payment payment = paymentService.getPayment(order.getPaymentId(), SYSTEM);
        orderStatusHelper.proceedOrderToStatus(order, DELIVERY);
        orderPayHelper.notifyBnplFinished(payment);
        assertFalse(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        bnplMockConfigurer.mockNotOkGetBnplOrder();

        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        assertTrue(queuedCallService.existsQueuedCall(ORDER_EMIT_CASHBACK, order.getId()));

        bnplMockConfigurer.mockGetBnplOrder("orderCancelled.json");

        queuedCallService.executeQueuedCallSynchronously(ORDER_EMIT_CASHBACK, order.getId());

        var payments = paymentService.getPayments(order.getId(), SYSTEM, CASHBACK_EMIT);
        payments = paymentService.getPayments(order.getId(), SYSTEM, CASHBACK_EMIT);
        assertEquals(0, payments.size());
    }
}
