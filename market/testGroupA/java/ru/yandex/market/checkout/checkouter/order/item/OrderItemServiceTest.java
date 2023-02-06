package ru.yandex.market.checkout.checkouter.order.item;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.utils.Parameters;

public class OrderItemServiceTest extends AbstractWebTestBase {

    @Autowired
    private OrderItemService orderItemService;

    @Test
    public void testFindItemById() {
        Order order = orderCreateHelper.createOrder(new Parameters());

        OrderItem orderItem = order.getItems().iterator().next();
        List<OrderItem> orderItemsByIds = orderItemService.findOrderItemsByIds(
                Collections.singletonList(orderItem.getId()));

        Assertions.assertEquals(1, orderItemsByIds.size());
        Assertions.assertEquals(order.getId(), orderItemsByIds.get(0).getOrderId());
    }
}
