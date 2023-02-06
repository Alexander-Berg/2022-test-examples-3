package ru.yandex.market.checkout.checkouter.order.status;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.ClientHelper;

public class NewOrderDeliveryFailuredStatusTest extends AbstractWebTestBase {

    private Order order;

    @BeforeEach
    void setUp() {
        order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        MatcherAssert.assertThat(order.getDelivery().getDeliveryPartnerType(),
                CoreMatchers.is(DeliveryPartnerType.YANDEX_MARKET));
    }


    @Test
    void userCanMoveFromDeliveryToDeliveryFailed() {
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        orderStatusHelper.updateOrderStatus(order.getId(), ClientHelper.userClientFor(order), OrderStatus.DELIVERY,
                OrderSubstatus.DELIVERY_USER_NOT_RECEIVED);
    }

    @Test
    void userCanMoveFromDeliveredToDeliveryFailed() {
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        orderStatusHelper.updateOrderStatus(order.getId(), ClientHelper.userClientFor(order), OrderStatus.DELIVERED,
                OrderSubstatus.DELIVERED_USER_NOT_RECEIVED);
    }

    @Test
    void callCenterCanMoveFromDeliveredToCancelled() {
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        orderStatusHelper.updateOrderStatus(order.getId(), ClientHelper.userClientFor(order), OrderStatus.DELIVERED,
                OrderSubstatus.DELIVERED_USER_NOT_RECEIVED);
        orderStatusHelper.updateOrderStatus(order.getId(), ClientHelper.callCenterOperatorFor(order),
                OrderStatus.CANCELLED, OrderSubstatus.SHOP_FAILED);
    }
}
