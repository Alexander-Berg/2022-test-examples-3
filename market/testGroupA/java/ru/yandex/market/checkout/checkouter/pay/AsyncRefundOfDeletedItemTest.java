package ru.yandex.market.checkout.checkouter.pay;

import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemChangeRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author : poluektov
 * date: 2022-02-04.
 */
public class AsyncRefundOfDeletedItemTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private RefundHelper refundHelper;
    @Autowired
    private RefundService refundService;

    @Test
    void testAsyncRefundOfDeletedItem() {
        Order order = createOrder();
        Payment payment = orderPayHelper.payForOrder(order);
        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), equalTo(OrderStatus.PROCESSING));
        orderPayHelper.notifyPaymentClear(payment);
        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        assertEquals(payment.getStatus(), PaymentStatus.CLEARED);

        // Удаление одного айтема из заказа. Должен запуститься рефанд.
        Iterator<OrderItem> orderItemIterator = order.getItems().iterator();
        OrderItem orderItemToRemove = orderItemIterator.next();
        OrderItem orderItemToLeave = orderItemIterator.next();
        OrderItemChangeRequest orderItemChangeRequest = new OrderItemChangeRequest(orderItemToLeave.getId(),
                orderItemToLeave.getCount(), orderItemToLeave.getQuantity(), orderItemToLeave.getFeedId(),
                orderItemToLeave.getOfferId());
        orderUpdateService.updateOrderItems(order.getId(), List.of(orderItemChangeRequest), ClientInfo.SYSTEM);
        refundHelper.proceedAsyncRefunds(order.getId());
        var refunds = refundService.getRefunds(order.getId());
        var refund = refunds.iterator().next();
        assertEquals(RefundStatus.ACCEPTED, refund.getStatus());
    }

    private Order createOrder() {
        Parameters parameters = WhiteParametersProvider.shopDeliveryOrder(OrderProvider.orderBuilder()
                .item(OrderItemProvider.getOrderItem())
                .item(OrderItemProvider.getOrderItem())
                .build()
        );
        return orderCreateHelper.createOrder(parameters);
    }
}
