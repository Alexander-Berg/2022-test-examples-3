package ru.yandex.market.checkout.checkouter.order.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class OrderControllerStatusCancelUnpaidByUserTest extends AbstractWebTestBase {

    private Order order;

    @BeforeEach
    public void prepareOrder() {
        Parameters parameters = new Parameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getStatus(), is(OrderStatus.UNPAID));
    }

    @Test
    public void testCancelByUser() {
        Order updated = orderStatusHelper.updateOrderStatus(
                this.order.getId(),
                new ClientInfo(ClientRole.USER, BuyerProvider.UID),
                OrderStatus.CANCELLED,
                OrderSubstatus.USER_CHANGED_MIND
        );

        assertThat(updated.getStatus(), is(OrderStatus.CANCELLED));
        assertThat(updated.getSubstatus(), is(OrderSubstatus.USER_CHANGED_MIND));
    }
}
