package ru.yandex.market.checkout.checkouter.order.status;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderControllerStatusDeliveryTypeDeliveryTest extends AbstractWebTestBase {

    @Test
    public void shouldNotAllowToUpdateStatusToPickupIfDeliveryTypeIsDelivery() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setDeliveryType(DeliveryType.DELIVERY);

        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getDelivery().getType(), is(DeliveryType.DELIVERY));

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        orderStatusHelper.updateOrderStatusForActions(order.getId(), ClientInfo.SYSTEM, OrderStatus.PICKUP, null)
                .andExpect(status().isBadRequest());
    }
}
