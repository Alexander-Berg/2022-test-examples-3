package ru.yandex.market.checkout.checkouter.order.status;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderControllerStatusChangeForCurrentStatusTest extends AbstractWebTestBase {

    private Order order;

    @Test
    public void shouldNotAllowToChangeToCurrentStatus() throws Exception {
        order = orderCreateHelper.createOrder(new Parameters());

        orderStatusHelper.updateOrderStatusForActions(order.getId(), ClientInfo.SYSTEM, order.getStatus(), null)
                .andExpect(status().isBadRequest());
    }
}
