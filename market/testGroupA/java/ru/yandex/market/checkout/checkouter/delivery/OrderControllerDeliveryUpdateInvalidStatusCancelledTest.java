package ru.yandex.market.checkout.checkouter.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.DeliveryUpdateProvider;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderControllerDeliveryUpdateInvalidStatusCancelledTest extends AbstractWebTestBase {

    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;

    @Test
    public void shouldNotAllowToUpdateDeliveryOfCancelledOrder() throws Exception {
        Parameters parameters = new Parameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND);

        Delivery deliveryUpdate = DeliveryUpdateProvider.createDeliveryUpdate((d) -> {
            d.setAddress(AddressProvider.getAnotherAddress());
        });

        orderDeliveryHelper.updateOrderDeliveryForActions(order.getId(), ClientInfo.SYSTEM, deliveryUpdate)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STATUS_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("Action is not allowed for order "
                        + order.getId() + " with " +
                        "status CANCELLED"));

    }
}
