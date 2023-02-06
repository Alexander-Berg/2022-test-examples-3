package ru.yandex.market.checkout.checkouter.order.status;

import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.MapUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.backbone.validation.order.status.graph.OrderStatusGraph;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author mmetlov
 */
public class OrderStatusGraphTest extends AbstractWebTestBase {

    @Autowired
    OrderStatusGraph orderStatusGraph;

    @Test
    public void shouldAcceptShopUserRights() {
        var permissions = orderStatusGraph.getEdgePermissions(OrderStatus.PENDING,
                OrderSubstatus.AWAIT_CONFIRMATION, OrderStatus.CANCELLED);

        assertThat(permissions, Matchers.allOf(
                Matchers.hasEntry(ClientRole.SHOP_USER, Set.of(OrderSubstatus.SHOP_FAILED,
                        OrderSubstatus.SHOP_PENDING_CANCELLED)),
                Matchers.hasEntry(ClientRole.SHOP, Set.of(OrderSubstatus.SHOP_FAILED,
                        OrderSubstatus.SHOP_PENDING_CANCELLED)),
                Matchers.hasEntry(ClientRole.BUSINESS_USER, Set.of(OrderSubstatus.SHOP_FAILED,
                        OrderSubstatus.SHOP_PENDING_CANCELLED)),
                Matchers.hasEntry(ClientRole.BUSINESS, Set.of(OrderSubstatus.SHOP_FAILED,
                        OrderSubstatus.SHOP_PENDING_CANCELLED))
        ));
    }

    @Test
    public void testAfterStatuses() {
        assertTrue(orderStatusGraph.isBefore(OrderStatus.PLACING, OrderStatus.DELIVERED));
        assertTrue(orderStatusGraph.isBefore(OrderStatus.PLACING, OrderStatus.CANCELLED));

        assertTrue(orderStatusGraph.isBefore(OrderStatus.PLACING, OrderStatus.PROCESSING));
        assertTrue(orderStatusGraph.isBefore(OrderStatus.RESERVED, OrderStatus.PROCESSING));
        assertTrue(orderStatusGraph.isBefore(OrderStatus.PENDING, OrderStatus.PROCESSING));
        assertTrue(orderStatusGraph.isBefore(OrderStatus.UNPAID, OrderStatus.PROCESSING));
        assertFalse(orderStatusGraph.isBefore(OrderStatus.PROCESSING, OrderStatus.PROCESSING));
        assertFalse(orderStatusGraph.isBefore(OrderStatus.DELIVERY, OrderStatus.PROCESSING));
        assertFalse(orderStatusGraph.isBefore(OrderStatus.PICKUP, OrderStatus.PROCESSING));
        assertFalse(orderStatusGraph.isBefore(OrderStatus.DELIVERED, OrderStatus.PROCESSING));
        assertFalse(orderStatusGraph.isBefore(OrderStatus.CANCELLED, OrderStatus.PROCESSING));

        assertFalse(orderStatusGraph.isBefore(OrderStatus.PROCESSING, OrderStatus.PLACING));
    }

    @Test
    void checkAntifraudToProcessingPermission() {
        Map<ClientRole, Set<OrderSubstatus>> edgePermissions =
                orderStatusGraph.getEdgePermissions(
                        OrderStatus.PENDING,
                        OrderSubstatus.ANTIFRAUD,
                        OrderStatus.PROCESSING);

        assertTrue(MapUtils.isNotEmpty(edgePermissions));

        edgePermissions =
                orderStatusGraph.getEdgePermissions(
                        OrderStatus.PENDING,
                        OrderSubstatus.ANTIFRAUD,
                        OrderStatus.PENDING);

        assertTrue(MapUtils.isNotEmpty(edgePermissions));
    }
}
