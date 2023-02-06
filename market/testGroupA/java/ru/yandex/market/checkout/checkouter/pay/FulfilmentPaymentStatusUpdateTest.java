package ru.yandex.market.checkout.checkouter.pay;

import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;

public class FulfilmentPaymentStatusUpdateTest extends AbstractPaymentTestBase {

    @BeforeEach
    public void setUp() throws Exception {
        createUnpaidFFOrder();
    }

    @DisplayName(value = "Первый платеж по любому из созданных платежей должен обновлять статус")
    @Test
    public void payFirstAndSecondAndNotifyFirst() throws Exception {
        Long receiptId = paymentTestHelper.initPayment();
        long paymentId = order().getPaymentId();
        Payment payment = order().getPayment();
        String deliveryBalanceOrderId = order().getDelivery().getBalanceOrderId();
        Map<Long, String> itemIdToBalanceOrderId = getItemToBalanceOrderId();

        paymentTestHelper.initPayment();

        order().setPaymentId(paymentId);
        order().setPayment(payment);
        paymentTestHelper.notifyPaymentSucceeded(receiptId, false, false);

        Assertions.assertEquals(OrderStatus.PROCESSING, order().getStatus());

        order.set(orderService.getOrder(order.get().getId()));

        Assertions.assertEquals(paymentId, order().getPaymentId().longValue());
        Assertions.assertEquals(itemIdToBalanceOrderId, getItemToBalanceOrderId());
        Assertions.assertEquals(deliveryBalanceOrderId, order().getDelivery().getBalanceOrderId());

    }

    private Map<Long, String> getItemToBalanceOrderId() {
        return order().getItems().stream()
                .filter(oi -> oi.getBalanceOrderId() != null)
                .collect(Collectors.toMap(OrderItem::getId, OrderItem::getBalanceOrderId));
    }
}
