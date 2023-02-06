package ru.yandex.market.checkout.checkouter.client;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CheckouterClientOrderItemSnapshotRequestTest extends AbstractWebTestBase {

    @Autowired
    private OrderServiceHelper orderServiceHelper;

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS_ORDER_ID)
    @DisplayName("/orders/id не должен возвращать order.item.snapshot без флага")
    @Test
    public void doesNotReturnSnapshotForOrderByDefault() {
        Order order = orderServiceHelper.createPostOrder();

        Order result = client.getOrder(order.getId(), ClientRole.SYSTEM, null);
        checkOrderHasNotSnapshot(order, result);
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS_ORDER_ID)
    @DisplayName("/orders/id не должен возвращать order.item.snapshot под флагом для новых заказов")
    @Test
    public void returnSnapshotForOrderWhenShowSnapshotSpecified() {
        Order order = orderServiceHelper.createPostOrder();

        Order result = client.getOrder(order.getId(), ClientRole.SYSTEM, null, true);
        checkOrderHasNotSnapshot(order, result);
    }

    private void checkOrderHasNotSnapshot(Order order, Order result) {
        assertEquals(order.getId(), result.getId());
        assertEquals(order.getShopId(), result.getShopId());
        assertEquals(order.getStatus(), result.getStatus());
        assertEquals(order.getDelivery().getType(), result.getDelivery().getType());
        assertEquals(order.getItems().size(), result.getItems().size());
    }
}
