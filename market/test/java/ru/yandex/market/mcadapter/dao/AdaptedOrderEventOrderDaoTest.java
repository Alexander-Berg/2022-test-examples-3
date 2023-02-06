package ru.yandex.market.mcadapter.dao;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mcadapter.AbstractFunctionalTest;
import ru.yandex.market.mcadapter.model.Order;
import ru.yandex.market.mcadapter.provider.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

/**
 * @author zagidullinri
 * @date 05.07.2022
 */
public class AdaptedOrderEventOrderDaoTest extends AbstractFunctionalTest {

    @Autowired
    private OrderDao orderDao;
    @Autowired
    private OrderItemDao orderItemDao;

    @Test
    public void OrderDaoGetOrdersShouldReturnOrders() {
        Order order = OrderProvider.getDefaultOrder();
        orderDao.insertIfNotExists(order);
        orderItemDao.insertIfNotExists(order.getOrderItems());

        List<Order> orders = orderDao.getOrders(order.getMultiOrderId());

        assertThat(orders, hasSize(1));
        Order fetchedOrder = orders.get(0);
        assertThat(fetchedOrder.getOrderItems(), hasSize(2));
    }
    @Test
    public void OrderDaoUpdateOrderShouldWorkProperly() {
        Order order = OrderProvider.getDefaultOrder();
        orderDao.insertIfNotExists(order);
        orderItemDao.insertIfNotExists(order.getOrderItems());
        BigDecimal newPrice = BigDecimal.valueOf(100000);
        assertThat(order.getPrice(), not(comparesEqualTo(newPrice)));
        order.setPrice(newPrice);

        orderDao.updateOrder(order);

        List<Order> orders = orderDao.getOrders(order.getMultiOrderId());
        assertThat(orders, hasSize(1));
        assertThat(orders.get(0).getPrice(), comparesEqualTo(newPrice));
    }
}
