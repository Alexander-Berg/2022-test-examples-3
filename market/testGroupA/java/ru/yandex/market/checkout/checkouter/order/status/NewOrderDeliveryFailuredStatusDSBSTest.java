package ru.yandex.market.checkout.checkouter.order.status;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.util.ClientHelper;

public class NewOrderDeliveryFailuredStatusDSBSTest extends AbstractWebTestBase {

    private Order order;

    @Test
    void orderCanBeMovedToDelivered() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();

        order = orderCreateHelper.createOrder(parameters);
        MatcherAssert.assertThat(order.getDelivery().getDeliveryPartnerType(),
                CoreMatchers.is(DeliveryPartnerType.SHOP));

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        orderStatusHelper.updateOrderStatus(order.getId(), ClientHelper.userClientFor(order), OrderStatus.DELIVERY,
                OrderSubstatus.DELIVERY_USER_NOT_RECEIVED);
        orderStatusHelper.updateOrderStatus(order.getId(), ClientHelper.shopClientFor(order), OrderStatus.DELIVERED,
                OrderSubstatus.DELIVERY_SERVICE_DELIVERED);
    }
}
