package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.checkouter.order.BasicOrder;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.service.business.OrderFinancialService;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.ReceiptRepairHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.utils.PaymentParameters;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.util.matching.NumberMatcher.numberEqualsTo;

public class MultiPaymentBlueTest extends AbstractPaymentTestBase {

    private static final Long EXPECTED_BALANCE_SERVICE_ID = 610L;
    @Autowired
    PaymentService paymentService;
    @Autowired
    RefundService refundService;
    private List<Order> orders = new ArrayList<>();
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private ReceiptRepairHelper receiptRepairHelper;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private RefundHelper refundHelper;
    @Autowired
    private OrderFinancialService financialService;

    @BeforeEach
    public void prepareOrders() {
        orders.add(orderServiceTestHelper.createUnpaidBlueOrder(null));
        orders.add(orderServiceTestHelper.createUnpaidBlueOrder(null));
        trustMockConfigurer.resetRequests();
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Оплата нескольких синих заказов одним платежом.")
    @Test
    public void payBlueOrderTest() throws Exception {
        BigDecimal total = orders.stream()
                .map(BasicOrder::getBuyerTotal)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        String returnPath = (new PaymentParameters()).getReturnPath();
        ordersPay(returnPath)
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.balanceServiceId").value(numberEqualsTo(EXPECTED_BALANCE_SERVICE_ID)))
                .andExpect(jsonPath("$.paymentForm").isNotEmpty())
                .andExpect(jsonPath("$.paymentForm.purchase_token").isNotEmpty())
                .andExpect(jsonPath("$.paymentForm._TARGET").isNotEmpty())
                .andExpect(jsonPath("$.totalAmount").value(formatAmount(total)));

        List<Order> payedOrders = getOrdersFromDB();

        assertEquals(2, payedOrders.size());
        assertEquals(1, payedOrders.stream().map(o -> o.getPayment().getBasketId()).distinct().count());
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Отмена заклиренного заказа")
    @Test
    public void cancelClearedOrder() throws Exception {
        //создаем платеж и холдим $$$
        ordersPay((new PaymentParameters()).getReturnPath());
        notifyPayment(asIds(), orders.get(0).getPayment());

        //клирим платеж путем перевода заказов в delivery
        paymentTestHelper.tryClearMultipayment(orders, Collections.emptyList());

        Payment payment = orderService.getOrder(orders.get(0).getId()).getPayment();
        assertThat(payment.getUid(), notNullValue());
        validateThatPayBasketEventContainsXUid();

        //Отменяем один из заказов
        Order canceledOrder = orderUpdateService.updateOrderStatus(orders.get(0).getId(), OrderStatus.CANCELLED,
                OrderSubstatus.CUSTOM);
        assertEquals(OrderStatus.CANCELLED, canceledOrder.getStatus());
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND_SUBSIDY_PAYMENT);

        //Проверяем что в базе есть рефанд и его сумма совпадает с общей стоимостью заказа
        Collection<Refund> refunds = refundHelper.proceedAsyncRefunds(refundService.getRefunds(canceledOrder.getId()));
        assertEquals(1, refunds.size());
        Refund refund = refunds.iterator().next();
        assertEquals(refund.getAmount(), canceledOrder.getBuyerTotal());
        List<Receipt> refundReceipts = receiptService.findByRefund(refund);
        assertEquals(1, refundReceipts.size());
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Отмена незаклиренного заказа")
    @Test
    public void cancelUnclearedOrder() throws Exception {
        //создаем платеж и холдим $$$
        ordersPay((new PaymentParameters()).getReturnPath());
        notifyPayment(asIds(), orders.get(0).getPayment());

        //Первый заказ отменим до клира денег, остальные переведем в delivery и заклирим платеж
        Order orderToBeCancelled = orders.get(0);
        List<Order> ordersToBeDelivered = new ArrayList<>(orders);
        ordersToBeDelivered.remove(orderToBeCancelled);

        //клирим платеж путем перевода оставшихся заказов в delivery
        paymentTestHelper.tryClearMultipayment(ordersToBeDelivered, Collections.singleton(orderToBeCancelled));

        //Проверяем что в базе есть чек возврата
        List<Receipt> refundReceipts = receiptService.findByPayment(
                orderToBeCancelled.getPayment(),
                ReceiptType.INCOME_RETURN
        );
        assertEquals(1, refundReceipts.size());
        Receipt returnReceipt = refundReceipts.get(0);
        assertEquals(ReceiptStatus.WAITING_FOR_CLEARANCE, returnReceipt.getStatus());
        trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildReversalConfig());
        trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildReversalConfig(), null);

        receiptRepairHelper.repairReceipts();

        returnReceipt = receiptService.getReceipt(returnReceipt.getId());
        assertEquals(ReceiptStatus.PRINTED, returnReceipt.getStatus());
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Оплата нескольких синих заказов одним платежом. Проверка колбека notify.")
    @Test
    public void payBlueOrderNotifyTest() throws Exception {
        List<Long> orderIds = asIds();

        // Инициируем платеж
        ordersPay((new PaymentParameters()).getReturnPath());

        // Убедимся, что платеж в нужном статусе
        Order createdOrder = orderService.getOrder(orders.get(0).getId());
        Payment createdPayment = createdOrder.getPayment();
        assertThat(createdPayment.getUid(), notNullValue());
        assertEquals(PaymentStatus.INIT, createdPayment.getStatus());
        validateThatPayBasketEventContainsXUid();

        // Проверяем, что чек есть и в нужном статусе
        receiptTestHelper.checkReceiptForOrders(orders, ReceiptStatus.NEW);
        // Подтверждаем платеж
        notifyPayment(orderIds, createdPayment);

        Map<Long, Order> updateOrders = orderService.getOrders(orderIds);
        Assertions.assertNotNull(updateOrders);
        Assertions.assertFalse(updateOrders.isEmpty());

        // Убедимся, что статус платежа и заказа изменилиcь
        for (Order order : updateOrders.values()) {
            assertEquals(PaymentStatus.HOLD, order.getPayment().getStatus());
            assertEquals(OrderStatus.PROCESSING, order.getStatus());
        }
        receiptTestHelper.checkReceiptForOrders(new ArrayList<>(updateOrders.values()),
                ReceiptStatus.PRINTED
        );
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @Test
    public void getColorForExistingBlueMultiPayment() throws Exception {
        ordersPay((new PaymentParameters()).getReturnPath());
        Payment payment = orderService.getOrder(orders.get(0).getId()).getPayment();
        assertThat(payment.getUid(), notNullValue());
        validateThatPayBasketEventContainsXUid();

        final Color color = paymentService.getColor(payment);
        assertEquals(Color.BLUE, color);
    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Отмена всего мультизаказа с платной доставкой")
    @Test
    public void cancelClearedMultiOrder() throws Exception {
        //создаем платеж и холдим $$$
        ordersPay((new PaymentParameters()).getReturnPath());
        notifyPayment(asIds(), orders.get(0).getPayment());

        //клирим платеж путем перевода заказов в delivery
        paymentTestHelper.tryClearMultipayment(orders, Collections.emptyList());

        Payment payment = orderService.getOrder(orders.get(0).getId()).getPayment();
        assertThat(payment.getUid(), notNullValue());

        //Отменяем один из заказов
        Order firstCanceledOrder = orderUpdateService.updateOrderStatus(orders.get(0).getId(), OrderStatus.CANCELLED,
                OrderSubstatus.CUSTOM);
        Order secondCanceledOrder = orderUpdateService.updateOrderStatus(orders.get(1).getId(), OrderStatus.CANCELLED,
                OrderSubstatus.CUSTOM);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND_SUBSIDY_PAYMENT);

        //Проверяем что в базе есть рефанд и его сумма совпадает с общей стоимостью заказа
        Refund refund1 = refundService.getRefunds(firstCanceledOrder.getId()).iterator().next();
        assertEquals(refund1.getAmount(), firstCanceledOrder.getBuyerTotal());

        Refund refund2 = refundService.getRefunds(secondCanceledOrder.getId()).iterator().next();
        assertEquals(refund2.getAmount(), secondCanceledOrder.getBuyerTotal());
    }

    @SuppressWarnings("checkstyle:HiddenField")
    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Разделение суммы платежа мультизаказа")
    @Test
    public void shouldSeparateTotalPaymentForMultiOrder() {
        checkouterProperties.setEnableSeparateTotalAmountInPaymentByOrders(true);

        BigDecimal totalAmount = orders.stream()
                .map(BasicOrder::getBuyerTotal)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        orderPayHelper.payForOrders(orders);
        List<Long> orderIds = this.orders.stream()
                .map(Order::getId)
                .collect(Collectors.toList());
        Map<Long, Order> orders = orderService.getOrders(orderIds);
        BigDecimal firstOrderPaymentPart =
                BigDecimal.ZERO.add(orders.get(orderIds.get(0)).getPayment().getTotalAmount());
        BigDecimal secondOrderPaymentPart =
                BigDecimal.ZERO.add(orders.get(orderIds.get(1)).getPayment().getTotalAmount());

        assertThat(firstOrderPaymentPart.add(secondOrderPaymentPart), Matchers.comparesEqualTo(totalAmount));
    }

    @SuppressWarnings("checkstyle:HiddenField")
    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Разделение суммы платежа мультизаказа, при наличии зафейленных чеков")
    @Test
    public void shouldSeparateTotalPaymentForMultiOrderWithFailedReceipts() {
        checkouterProperties.setEnableSeparateTotalAmountInPaymentByOrders(true);

        BigDecimal totalAmount = orders.stream()
                .map(BasicOrder::getBuyerTotal)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        Payment payment = orderPayHelper.payForOrders(orders);

        Receipt receiptClone = receiptService.findByOrder(orders.get(0).getId()).get(0);
        long receiptId = receiptService.createReceipt(receiptClone);
        receiptClone.setId(receiptId);
        receiptService.updateReceiptStatus(receiptClone, ReceiptStatus.FAILED);

        List<Receipt> allPaymentReceipts = receiptService.findAllPaymentReceipts(payment.getId());

        List<Long> orderIds = this.orders.stream()
                .map(Order::getId)
                .collect(Collectors.toList());
        Map<Long, Order> orders = orderService.getOrders(orderIds);
        BigDecimal firstOrderPaymentPart =
                BigDecimal.ZERO.add(orders.get(orderIds.get(0)).getPayment().getTotalAmount());
        BigDecimal secondOrderPaymentPart =
                BigDecimal.ZERO.add(orders.get(orderIds.get(1)).getPayment().getTotalAmount());

        assertThat(firstOrderPaymentPart.add(secondOrderPaymentPart), Matchers.comparesEqualTo(totalAmount));

        BigDecimal allReceiptItemsTotalForFirstOrder = allPaymentReceipts.stream()
                .map(Receipt::getItems)
                .flatMap(Collection::stream)
                .filter(ri -> ri.getOrderId().equals(orderIds.get(0)))
                .map(ReceiptItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(allReceiptItemsTotalForFirstOrder, Matchers.comparesEqualTo(
                firstOrderPaymentPart.multiply(BigDecimal.valueOf(2))));

        BigDecimal allReceiptItemsTotalForSecondOrder = allPaymentReceipts.stream()
                .map(Receipt::getItems)
                .flatMap(Collection::stream)
                .filter(ri -> ri.getOrderId().equals(orderIds.get(1)))
                .map(ReceiptItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(allReceiptItemsTotalForSecondOrder, Matchers.comparesEqualTo(
                secondOrderPaymentPart.multiply(BigDecimal.valueOf(2))));

    }

    @Epic(Epics.PAYMENT)
    @Story(Stories.PAYMENT)
    @DisplayName("Разделение платежа в рефандах")
    @Test
    public void shouldSeparateTotalPaymentForRefund() throws Exception {
        checkouterProperties.setEnableSeparateTotalAmountInPaymentByOrders(true);

        //создаем платеж и холдим $$$
        ordersPay((new PaymentParameters()).getReturnPath());
        notifyPayment(asIds(), orders.get(0).getPayment());

        //клирим платеж путем перевода заказов в delivery
        paymentTestHelper.tryClearMultipayment(orders, Collections.emptyList());

        BigDecimal totalAmount = orders.stream()
                .map(BasicOrder::getBuyerTotal)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);

        Payment payment = orderService.getOrder(orders.get(0).getId()).getPayment();
        assertThat(payment.getUid(), notNullValue());

        //Отменяем один из заказов
        Order firstCanceledOrder = orderUpdateService.updateOrderStatus(orders.get(0).getId(), OrderStatus.CANCELLED,
                OrderSubstatus.CUSTOM);
        Order secondCanceledOrder = orderUpdateService.updateOrderStatus(orders.get(1).getId(), OrderStatus.CANCELLED,
                OrderSubstatus.CUSTOM);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND_SUBSIDY_PAYMENT);

        //Проверяем что в базе есть рефанд и его сумма совпадает с общей стоимостью заказа
        Refund refund1 = refundService.getRefunds(firstCanceledOrder.getId()).iterator().next();
        assertEquals(refund1.getAmount(), firstCanceledOrder.getBuyerTotal());

        Refund refund2 = refundService.getRefunds(secondCanceledOrder.getId()).iterator().next();
        assertEquals(refund2.getAmount(), secondCanceledOrder.getBuyerTotal());

        BigDecimal firstRefundPaymentPart = BigDecimal.ZERO.add(refund1.getPayment().getTotalAmount());
        BigDecimal secondRefundPaymentPart = BigDecimal.ZERO.add(refund2.getPayment().getTotalAmount());

        assertThat(firstRefundPaymentPart.add(secondRefundPaymentPart), Matchers.comparesEqualTo(totalAmount));
    }

    private List<Long> asIds() {
        return orders.stream().map(Order::getId).collect(Collectors.toList());
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(0, BigDecimal.ROUND_HALF_EVEN).toString();
    }

    private ResultActions ordersPay(String returnPath) throws Exception {
        ResultActions resultActions = ordersPay(asIds(), returnPath);
        orders = getOrdersFromDB();
        return resultActions;
    }

    private List<Order> getOrdersFromDB() {
        return new ArrayList<>(
                orderService.getOrders(
                        asIds()
                ).values()
        );
    }
}
