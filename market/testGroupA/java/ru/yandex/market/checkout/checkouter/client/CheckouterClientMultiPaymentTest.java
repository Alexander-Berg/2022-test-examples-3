package ru.yandex.market.checkout.checkouter.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.backbone.order.reservation.OrderCompletionService;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.checkouter.pay.builders.PrepayPaymentBuilder.PLACEHOLDER_PAYMENT_ID;

public class CheckouterClientMultiPaymentTest extends AbstractWebTestBase {

    @Autowired
    private OrderCompletionService orderCompletionService;

    @Test
    public void canCreatePaymentWithNullCardId() {
        List<Order> orderList = prepareOrderList();
        List<Long> orderIds = orderList.stream().map(Order::getId).collect(Collectors.toList());
        log.debug(Arrays.toString(orderIds.toArray()));

        Payment payment = client.payments().payOrders(PayOrdersParameters.builder()
                .withOrderIds(orderIds)
                .withUid(orderList.get(0).getBuyer().getUid())
                .withReturnPath("https://market-test.pepelac1ft.yandex.ru/payment/status/"
                        + PLACEHOLDER_PAYMENT_ID + "/")
                .withSandbox(true)
                .build());
        checkPayment(payment);
    }

    private List<Order> prepareOrderList() {
        List<Order> orderList = new ArrayList<>();
        orderList.add(prepareOrder());
        orderList.add(prepareOrder());
        orderList.add(prepareOrder());
        orderList.add(prepareOrder());
        return orderList;
    }

    private Order prepareOrder() {
        Order order = OrderProvider.getPrepaidOrder();
        order.setFake(true);

        shopService.updateMeta(order.getShopId(), ShopSettingsHelper.getDefaultMeta());

        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        order = orderUpdateService.reserveOrder(orderId, String.valueOf(orderId), order.getDelivery());
        orderCompletionService.completeOrder(order, ClientInfo.SYSTEM);
        return order;
    }

    private void checkPayment(Payment payment) {
        assertEquals(PaymentStatus.INIT, payment.getStatus());
        assertNotNull(payment.getId());
        assertNull(payment.getFailReason());
    }

}
