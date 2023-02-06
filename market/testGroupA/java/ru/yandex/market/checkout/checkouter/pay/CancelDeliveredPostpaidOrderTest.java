package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.balance.model.notifications.RefundInspectorNotification;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.postpaidBlueOrderParameters;
import static ru.yandex.market.checkout.util.balance.ShopSettingsHelper.metaWithPaymentControlFlag;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;

/**
 * @author : poluektov
 * date: 15.10.2018.
 */
public class CancelDeliveredPostpaidOrderTest extends AbstractWebTestBase {

    private static final Long SHOP_ID = 33669944L;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private RefundHelper refundHelper;
    @Autowired
    private EventsGetHelper eventsGetHelper;
    private Order order;

    @BeforeEach
    public void createPostPaidOrder() {
        trustMockConfigurer.mockWholeTrust();
        Parameters params = postpaidBlueOrderParameters(123L);
        OrderItem item1 = OrderItemProvider.buildOrderItem("item-1", new BigDecimal("111.00"), 1);
        item1.setMsku(332L);
        item1.setShopSku("sku-1");
        item1.setSku("332");
        item1.setSupplierId(SHOP_ID);
        item1.setWareMd5(OrderItemProvider.OTHER_WARE_MD5);
        item1.setShowInfo(OrderItemProvider.OTHER_SHOW_INFO);

        params.getOrder().setItems(List.of(item1));
        params.addShopMetaData(SHOP_ID, metaWithPaymentControlFlag(
                SHOP_ID.intValue()));
        order = orderCreateHelper.createOrder(params);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_CASH_PAYMENT);
        //считаем что ордер заклирился, теперь считаем что траст возвращает статус клиред
        trustMockConfigurer.mockCheckBasket(buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);
    }

    @Test
    public void shouldRefundCashPayment() throws Exception {
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);
        //После смены статуса платеж попадает в очередь на обработку
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));
        //Таска обрабатывающая платежи создает полный рефанд по данному платежу.
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);
        refundHelper.proceedAsyncRefunds(order.getId());
        Payment payment = paymentService.findPayment(order.getPaymentId(), ClientInfo.SYSTEM);
        assertFalse(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));
        checkRefundAndReceipt(payment);
        checkEvents();
    }

    private void checkRefundAndReceipt(Payment payment) {
        Collection<Refund> refunds = refundService.getRefunds(order.getId());
        assertThat(refunds, hasSize(1));
        Refund refund = refunds.iterator().next();
        assertThat(refund.getAmount(), equalTo(payment.getTotalAmount()));
        assertThat(refund.getStatus(), equalTo(RefundStatus.ACCEPTED));
        assertNotNull(refund.getTrustRefundKey().getRefundBasketKey().getBasketId());
        assertNotNull(refund.getTrustRefundKey().getRefundBasketKey().getPurchaseToken());
        List<Receipt> receipts = receiptService.findByRefund(refund);
        assertThat(receipts, hasSize(1));
        assertThat(receipts.get(0).getStatus(), equalTo(ReceiptStatus.GENERATED));

        refundService.notifyRefund(new RefundInspectorNotification(refund.getId()));
        refund = refundService.getRefunds(order.getId()).iterator().next();
        assertThat(refund.getStatus(), equalTo(RefundStatus.SUCCESS));
    }

    private void checkEvents() throws Exception {
        PagedEvents orderHistoryEvents = eventsGetHelper.getOrderHistoryEvents(order.getId(), Integer.MAX_VALUE);
        assertTrue(orderHistoryEvents.getItems().stream()
                .anyMatch(e -> HistoryEventType.CASH_REFUND == e.getType()));
        assertTrue(orderHistoryEvents.getItems().stream()
                .anyMatch(e -> HistoryEventType.RECEIPT_GENERATED == e.getType()));
    }
}
