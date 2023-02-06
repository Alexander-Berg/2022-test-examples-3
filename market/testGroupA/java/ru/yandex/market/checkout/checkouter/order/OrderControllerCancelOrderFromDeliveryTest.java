package ru.yandex.market.checkout.checkouter.order;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;

public class OrderControllerCancelOrderFromDeliveryTest extends AbstractWebTestBase {

    @Autowired
    private QueuedCallService queuedCallService;

    @Test
    public void shouldAllowToCancelPrepaidOrderFromDelivery() throws IOException {
        Parameters parameters = new Parameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        Order order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        mockBalance();

        orderStatusHelper.updateOrderStatus(
                order.getId(), OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND
        );

        Order cancelled = orderService.getOrder(order.getId());
        assertThat(cancelled.getStatus(), equalTo(OrderStatus.CANCELLED));
        Assertions.assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, cancelled.getId()));
    }

    private void mockBalance() throws IOException {
        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockCheckBasket(buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);
        trustMockConfigurer.mockCreateRefund(null);
        trustMockConfigurer.mockDoRefund();
    }
}
