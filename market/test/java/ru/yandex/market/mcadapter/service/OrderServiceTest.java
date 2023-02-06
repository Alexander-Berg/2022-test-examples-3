package ru.yandex.market.mcadapter.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mcadapter.AbstractFunctionalTest;
import ru.yandex.market.mcadapter.model.Order;
import ru.yandex.market.mcadapter.provider.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author zagidullinri
 * @date 11.07.2022
 */
public class OrderServiceTest extends AbstractFunctionalTest {

    @Autowired
    private OrderService orderService;

    @Test
    public void OrderDaoGetOrdersShouldReturnOrders() {
        Order order = OrderProvider.getDefaultOrder();
        orderService.saveOrder(order);

        List<Order> orders = orderService.getOrders(order.getMultiOrderId());

        assertThat(orders, hasSize(1));
        Order fetchedOrder = orders.get(0);
        assertThat(fetchedOrder.getOrderItems(), hasSize(2));
    }
}
