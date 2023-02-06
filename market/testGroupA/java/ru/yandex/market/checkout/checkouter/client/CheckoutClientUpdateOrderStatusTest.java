package ru.yandex.market.checkout.checkouter.client;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;

public class CheckoutClientUpdateOrderStatusTest extends AbstractWebTestBase {

    @Autowired
    private OrderServiceHelper orderServiceHelper;

    @Test
    public void canUpdateOrderStatus() {
        Order order = orderServiceHelper.prepareOrder();
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        OrderStatus targetStatus = OrderStatus.CANCELLED;
        OrderSubstatus targetSubstatus = OrderSubstatus.USER_CHANGED_MIND;

        Order updatedOrder = client.updateOrderStatus(
                order.getId(),
                ClientRole.SHOP,
                order.getShopId(),
                order.getShopId(),
                targetStatus,
                targetSubstatus
        );

        assertEquals(order.getId(), updatedOrder.getId());
        assertEquals(order.getShopId(), updatedOrder.getShopId());
        assertEquals(targetStatus, updatedOrder.getStatus());
        assertEquals(targetSubstatus, updatedOrder.getSubstatus());
    }

    /**
     * Проверяет обновление статуса заказа с множественным clientId
     */
    @Test
    public void canUpdateOrderStatusMultiClientId() {
        Order order = orderServiceHelper.prepareOrder();
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        OrderStatus targetStatus = OrderStatus.CANCELLED;
        OrderSubstatus targetSubstatus = OrderSubstatus.USER_CHANGED_MIND;

        Order updatedOrder = client.updateOrderStatus(
                order.getId(),
                RequestClientInfo.builder(ClientRole.SHOP)
                        .withClientIds(Set.of(1L, order.getShopId(), 2L))
                        .build(),
                targetStatus,
                targetSubstatus
        );

        assertEquals(order.getId(), updatedOrder.getId());
        assertEquals(order.getShopId(), updatedOrder.getShopId());
        assertEquals(targetStatus, updatedOrder.getStatus());
        assertEquals(targetSubstatus, updatedOrder.getSubstatus());
    }

    @Test
    public void canUpdateOrderStatusBusiness() {
        Order order = orderServiceHelper.prepareOrder();
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        OrderStatus targetStatus = OrderStatus.CANCELLED;
        OrderSubstatus targetSubstatus = OrderSubstatus.USER_CHANGED_MIND;

        Order updatedOrder = client.updateOrderStatus(
                order.getId(),
                ClientRole.BUSINESS,
                order.getBusinessId(),
                order.getShopId(),
                targetStatus,
                targetSubstatus
        );

        assertEquals(order.getId(), updatedOrder.getId());
        assertEquals(order.getShopId(), updatedOrder.getShopId());
        assertEquals(targetStatus, updatedOrder.getStatus());
        assertEquals(targetSubstatus, updatedOrder.getSubstatus());
    }

    @Test
    public void canUpdateOrderStatusWithCancellationByShopRuleEnabled() {
        Order order = orderServiceHelper.prepareOrder();
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        OrderStatus targetStatus = OrderStatus.CANCELLED;
        OrderSubstatus targetSubstatus = OrderSubstatus.SHOP_FAILED;

        Order updatedOrder = client.updateOrderStatus(
                order.getId(),
                ClientRole.SHOP,
                order.getShopId(),
                order.getShopId(),
                targetStatus,
                targetSubstatus
        );

        assertEquals(order.getId(), updatedOrder.getId());
        assertEquals(order.getShopId(), updatedOrder.getShopId());
        assertEquals(targetStatus, updatedOrder.getStatus());
        assertEquals(targetSubstatus, updatedOrder.getSubstatus());
    }

    /**
     * Проверяет обновление статуса заказа с множественным clientId
     */
    @Test
    public void canUpdateOrderStatusMultiClientIdWithCancellationByShopRuleEnabled() {
        Order order = orderServiceHelper.prepareOrder();
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        OrderStatus targetStatus = OrderStatus.CANCELLED;
        OrderSubstatus targetSubstatus = OrderSubstatus.SHOP_FAILED;

        Order updatedOrder = client.updateOrderStatus(
                order.getId(),
                RequestClientInfo.builder(ClientRole.SHOP)
                        .withClientIds(Set.of(1L, order.getShopId(), 2L))
                        .build(),
                targetStatus,
                targetSubstatus
        );

        assertEquals(order.getId(), updatedOrder.getId());
        assertEquals(order.getShopId(), updatedOrder.getShopId());
        assertEquals(targetStatus, updatedOrder.getStatus());
        assertEquals(targetSubstatus, updatedOrder.getSubstatus());
    }

    @Test
    public void canUpdateOrderStatusBusinessWithCancellationByShopRuleEnabled() {
        Order order = orderServiceHelper.prepareOrder();
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        OrderStatus targetStatus = OrderStatus.CANCELLED;
        OrderSubstatus targetSubstatus = OrderSubstatus.SHOP_FAILED;

        Order updatedOrder = client.updateOrderStatus(
                order.getId(),
                ClientRole.BUSINESS,
                order.getBusinessId(),
                order.getShopId(),
                targetStatus,
                targetSubstatus
        );

        assertEquals(order.getId(), updatedOrder.getId());
        assertEquals(order.getShopId(), updatedOrder.getShopId());
        assertEquals(targetStatus, updatedOrder.getStatus());
        assertEquals(targetSubstatus, updatedOrder.getSubstatus());
    }

    @Test
    public void canNotUpdateToWrongOrderStatus() {
        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            Order order = orderServiceHelper.prepareOrder();
            OrderStatus targetStatus = OrderStatus.DELIVERED;

            Order updatedOrder = client.updateOrderStatus(order.getId(),
                    ClientRole.SHOP, order.getShopId(), order.getShopId(), targetStatus, null);

            assertEquals(order.getId(), updatedOrder.getId());
            assertEquals(order.getShopId(), updatedOrder.getShopId());
            assertEquals(targetStatus, updatedOrder.getStatus());
        });
    }
}
