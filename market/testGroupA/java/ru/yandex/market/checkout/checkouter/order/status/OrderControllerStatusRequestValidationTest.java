package ru.yandex.market.checkout.checkouter.order.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderControllerStatusRequestValidationTest extends AbstractWebTestBase {

    private Order order;

    @BeforeEach
    public void setUp() {
        if (order != null) {
            return;
        }
        order = orderCreateHelper.createOrder(new Parameters());
    }

    @Test
    public void shouldFailOnInvalidClientRole() throws Exception {
        mockMvc.perform(
                post("/orders/{orderId}/status", order.getId())
                        .param("status", OrderStatus.DELIVERY.name())
                        .param("clientRole", "INVALID")
        ).andExpect(status().isBadRequest());
    }

    @Test
    public void shouldFailOnOrderOfAnotherShop() throws Exception {
        orderStatusHelper.updateOrderStatusForActions(
                order.getId(),
                new ClientInfo(ClientRole.SHOP, order.getShopId() + 1),
                OrderStatus.DELIVERY,
                null
        ).andExpect(status().isNotFound());
    }

    @Test
    public void shouldFailOnOrderOfAnotherUser() throws Exception {
        orderStatusHelper.updateOrderStatusForActions(
                order.getId(),
                new ClientInfo(ClientRole.USER, order.getBuyer().getUid() + 1),
                OrderStatus.DELIVERY,
                null
        ).andExpect(status().isNotFound());
    }

    @Test
    public void shouldFailIfClientIdIsZeroButRoleIsUser() throws Exception {
        orderStatusHelper.updateOrderStatusForActions(
                order.getId(),
                new ClientInfo(ClientRole.USER, 0L),
                OrderStatus.DELIVERY,
                null
        ).andExpect(status().isNotFound());
    }

    @Test
    public void shouldFailIfClientIdIsShopIdButRoleIsUser() throws Exception {
        orderStatusHelper.updateOrderStatusForActions(
                order.getId(),
                new ClientInfo(ClientRole.USER, order.getShopId()),
                OrderStatus.DELIVERY,
                null
        ).andExpect(status().isNotFound());
    }

    @Test
    public void shouldFailIfClientIdIsNullButRoleIsUser() throws Exception {
        orderStatusHelper.updateOrderStatusForActions(
                order.getId(),
                new ClientInfo(ClientRole.USER, null),
                OrderStatus.DELIVERY,
                null
        ).andExpect(status().isBadRequest());
    }

    @Test
    public void shouldFailIfClientIdIsNotNumericButRoleIsUser() throws Exception {
        mockMvc.perform(
                post("/orders/{orderId}/status", order.getId())
                        .param("status", OrderStatus.DELIVERY.name())
                        .param("clientRole", ClientRole.USER.name())
                        .param("clientId", "not a number")
        ).andExpect(status().isBadRequest());
    }

    @Test
    public void shouldFailIfClientIdIsNotIntegerButRoleIsUser() throws Exception {
        mockMvc.perform(
                post("/orders/{orderId}/status", order.getId())
                        .param("status", OrderStatus.DELIVERY.name())
                        .param("clientRole", ClientRole.USER.name())
                        .param("clientId", "666.66")
        ).andExpect(status().isBadRequest());
    }

    @Test
    public void shouldFailIfInvalidStatus() throws Exception {
        mockMvc.perform(
                post("/orders/{orderId}/status", order.getId())
                        .param("status", "INVALID")
                        .param("clientRole", ClientRole.SYSTEM.name())
        ).andExpect(status().isBadRequest());
    }

    @Test
    public void shouldFailIfWithoutStatus() throws Exception {
        mockMvc.perform(
                post("/orders/{orderId}/status", order.getId())
                        .param("clientRole", ClientRole.SYSTEM.name())
        ).andExpect(status().isBadRequest());
    }

    @Test
    public void shouldFailIfWithInvalidSubstatus() throws Exception {
        mockMvc.perform(
                post("/orders/{orderId}/status", order.getId())
                        .param("status", OrderStatus.DELIVERY.name())
                        .param("substatus", "INVALID_SUBSTATUS")
                        .param("clientRole", ClientRole.SYSTEM.name())
        ).andExpect(status().isBadRequest());
    }

    @Test
    public void shouldFailWithSubstatusNotMatchingStatusFactory() throws Exception {
        mockMvc.perform(
                post("/orders/{orderId}/status", order.getId())
                        .param("status", OrderStatus.DELIVERY.name())
                        .param("substatus", OrderSubstatus.USER_CHANGED_MIND.name())
                        .param("clientRole", ClientRole.SYSTEM.name())
        ).andExpect(status().isBadRequest());
    }
}
