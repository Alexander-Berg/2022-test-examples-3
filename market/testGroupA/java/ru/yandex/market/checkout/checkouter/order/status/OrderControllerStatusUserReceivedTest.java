package ru.yandex.market.checkout.checkouter.order.status;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.util.ClientHelper;

public class OrderControllerStatusUserReceivedTest extends AbstractWebTestBase {

    @Test
    void shouldAllowUserToMoveOrderToDeliveryUserReceived() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        MatcherAssert.assertThat(order.getDelivery().getType(), CoreMatchers.is(DeliveryType.DELIVERY));
        MatcherAssert.assertThat(order.getDelivery().getDeliveryPartnerType(),
                CoreMatchers.is(DeliveryPartnerType.SHOP));

        Order delivery = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        orderStatusHelper.updateOrderStatus(delivery.getId(), ClientHelper.userClientFor(delivery),
                OrderStatus.DELIVERY, OrderSubstatus.USER_RECEIVED);
    }

    @Test
    void shouldAllowUserToMoveOrderToDeliveredUserReceived() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        MatcherAssert.assertThat(order.getDelivery().getType(), CoreMatchers.is(DeliveryType.DELIVERY));
        MatcherAssert.assertThat(order.getDelivery().getDeliveryPartnerType(),
                CoreMatchers.is(DeliveryPartnerType.SHOP));

        Order delivery = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        orderStatusHelper.updateOrderStatus(delivery.getId(), ClientHelper.userClientFor(delivery),
                OrderStatus.DELIVERED, OrderSubstatus.DELIVERED_USER_RECEIVED);
    }
}
