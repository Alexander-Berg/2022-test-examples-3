package ru.yandex.market.checkout.checkouter.pay;

import java.util.ArrayList;
import java.util.List;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.TrustTestHelper;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;

public class MultiPaymentOrderStatusTest extends AbstractPaymentTestBase {

    private final List<Order> orders = new ArrayList<>();
    @Autowired
    PaymentService paymentService;
    @Autowired
    RefundService refundService;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private TrustTestHelper trustTestHelper;
    @Autowired
    private QueuedCallService queuedCallService;

    @BeforeEach
    public void prepareOrders() {
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        orders.add(orderServiceTestHelper.createUnpaidBlueOrder(null));
        orders.add(orderServiceTestHelper.createUnpaidBlueOrder(null));
        trustTestHelper.resetMockRequests();
        checkouterProperties.setEnableUpdatePaymentMode(true);
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Проверяем, что при переходе всех заказов в DELIVERY будет вызов ClearPayment")
    @Test
    public void checkAllDeliveryPaymentCleared() {
        Payment payment = orderPayHelper.payForOrders(orders);
        assertNull(payment.getOrderId());
        proceedToStatus(DELIVERY);
        trustTestHelper.assertClearCallCollection(hasSize(0));
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        trustTestHelper.assertClearCallCollection(hasSize(1));
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Проверяем, что при переходе одного из заказов в DELIVERY, второго в DELIVERED будет вызов " +
            "ClearPayment")
    @Test
    public void checkDeliveryAndDeliveredPaymentCleared() {
        Payment payment = orderPayHelper.payForOrders(orders);
        assertNull(payment.getOrderId());
        Order firstOrder = orderService.getOrder(orders.get(0).getId());
        proceedToStatus(DELIVERY);
        trustTestHelper.assertClearCallCollection(hasSize(0));
        orderStatusHelper.proceedOrderToStatusWithoutTask(orderService.getOrder(firstOrder.getId()), DELIVERED);
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        trustTestHelper.assertClearCallCollection(hasSize(1));
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Проверяем, что при переходе одного из заказов в DELIVERY, второго в DELIVERED будет вызов " +
            "ClearPayment" +
            " причём только один, даже если джоба отработает в промежутке")
    @Test
    public void checkDeliveryAndDeliveredPaymentCleared2() {
        Payment payment = orderPayHelper.payForOrders(orders);
        assertNull(payment.getOrderId());
        Order firstOrder = orderService.getOrder(orders.get(0).getId());
        proceedToStatus(DELIVERY);
        trustTestHelper.assertClearCallCollection(hasSize(0));
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        trustTestHelper.assertClearCallCollection(hasSize(1));
        orderStatusHelper.proceedOrderToStatusWithoutTask(orderService.getOrder(firstOrder.getId()), DELIVERED);
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        trustTestHelper.assertClearCallCollection(hasSize(1));
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Проверяем, что при переходе всех заказов в DELIVERED будет вызов ClearPayment")
    @Test
    public void checkAllDeliveredPaymentCleared() {
        Payment payment = orderPayHelper.payForOrders(orders);
        assertNull(payment.getOrderId());
        proceedToStatus(DELIVERED);
        trustTestHelper.assertClearCallCollection(hasSize(0));
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        trustTestHelper.assertClearCallCollection(hasSize(1));
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Проверяем, что при переходе всех заказов в DELIVERED будет вызов ClearPayment, причём однажды, " +
            "даже если джоба отработала сначала когда оба заказа были в DELIVERY")
    @Test
    public void checkAllDeliveredAfterAllDeliveryPaymentClearedOnce() {
        Payment payment = orderPayHelper.payForOrders(orders);
        assertNull(payment.getOrderId());
        proceedToStatus(DELIVERY);
        trustTestHelper.assertClearCallCollection(hasSize(0));
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        trustTestHelper.assertClearCallCollection(hasSize(1));
        proceedToStatus(DELIVERED);
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        trustTestHelper.assertClearCallCollection(hasSize(1));
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Проверяем, что в случае магазинной доставки, в момент клира платежа будет запланирован вызов баланса")
    @Test
    public void checkDeliveryPaymentQueuedBalanceCall() {
        Payment payment = orderPayHelper.payForOrders(orders);
        assertNull(payment.getOrderId());
        proceedToStatus(DELIVERY);
        // запуск клира платежа
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.PAYMENT_CALL_BALANCE_UPDATE_PAYMENT,
                payment.getId()));
    }

    @SuppressWarnings("checkstyle:HiddenField")
    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Проверяем, что в случае маркетной доставки, при переходе хотя бы одного заказа в DELIVERED будет " +
            "запланирован вызов баланса " +
            "в момент клира платежей")
    @Test
    public void checkDeliveredPaymentQueuedBalanceCall() {
        List<Order> orders = new ArrayList<>();
        orders.add(orderServiceTestHelper.createUnpaidBlueOrderWithShopDelivery(null));
        orders.add(orderServiceTestHelper.createUnpaidBlueOrderWithShopDelivery(null));
        Payment payment = orderPayHelper.payForOrders(orders);
        assertNull(payment.getOrderId());
        orderStatusHelper.proceedOrderToStatusWithoutTask(orderService.getOrder(orders.get(0).getId()), DELIVERY);
        // запуск клира платежа
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.PAYMENT_CALL_BALANCE_UPDATE_PAYMENT,
                payment.getId()));
        orderStatusHelper.proceedOrderToStatusWithoutTask(orderService.getOrder(orders.get(0).getId()), DELIVERED);
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.PAYMENT_CALL_BALANCE_UPDATE_PAYMENT,
                payment.getId()));
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Проверяем, что после клира платежа, а затем доставки всех заказов - будет запланирован вызов баланса")
    @Test
    public void checkAllDeliveredAfterAllDeliveryQueuedBalanceCall() {
        Payment payment = orderPayHelper.payForOrders(orders);
        assertNull(payment.getOrderId());
        proceedToStatus(DELIVERY);
        // клирим платеж
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        // доставляем все заказы
        proceedToStatus(DELIVERED);

        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.PAYMENT_CALL_BALANCE_UPDATE_PAYMENT,
                payment.getId()));
    }

    @DisplayName("Не должны принимать оплату по мультиплатежу и двигать заказы по статусу дальше, если хотя бы один " +
            "из заказов уже оплачен")
    @Test
    public void shouldNotProcessAnyOrdersInFailedMultiPayment() {
        Order first = orders.get(0);
        Order second = orders.get(1);
        // открыта форма оплаты по мультизаказу
        Payment multiPayment = orderPayHelper.payForOrdersWithoutNotification(orders);

        // оплачиваем отдельно второй заказ
        Payment payment = orderPayHelper.payForOrder(second);
        assertEquals(OrderStatus.PROCESSING, orderService.getOrder(second.getId()).getStatus());
        assertEquals(PaymentStatus.HOLD, paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM).getStatus());

        // оплачиваем мультизаказ через открытую ранее форму
        orderPayHelper.notifyPayment(multiPayment);

        assertEquals(OrderStatus.UNPAID, orderService.getOrder(first.getId()).getStatus());
        assertEquals(OrderStatus.PROCESSING, orderService.getOrder(second.getId()).getStatus());
        assertEquals(PaymentStatus.CANCELLED,
                paymentService.getPayment(multiPayment.getId(), ClientInfo.SYSTEM).getStatus());
    }

    private void proceedToStatus(OrderStatus targetOrdersStatus) {
        orders.stream()
                .map(o -> orderService.getOrder(o.getId()))
                .forEach(
                        o -> orderStatusHelper.proceedOrderToStatusWithoutTask(o, targetOrdersStatus)
                );
    }
}
