package ru.yandex.market.abo.core.ticket;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.ticket.order.Order;
import ru.yandex.market.abo.core.ticket.order.OrderService;
import ru.yandex.market.abo.core.ticket.order.OrderState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author imelnikov
 * @date 01.06.17
 */
class OrderServiceTest extends EmptyTest {
    private static final int HYP_ID = -11;

    @Autowired
    private OrderService orderService;

    @Test
    void testOrderStoreLoad() {
        Order order = createOrder();
        orderService.storeOrder(order);
        Order createdOrder = orderService.loadOrder(order.getHypId());
        assertEquals(order, createdOrder);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void testSaveOrUpdate() {
        Order order = createOrder();
        orderService.saveOrUpdateByHypId(order);

        order.setDeliveryPrice(BigDecimal.valueOf(124));
        order.setState(OrderState.DELIVERY);
        order.setPhoneNum("+7-777-777-77-77");
        order.setEmployeeName("EmployeeName");
        orderService.saveOrUpdateByHypId(order);

        Order updatedByPkOrder = orderService.loadOrder(HYP_ID);
        assertEquals(order, updatedByPkOrder);
        assertEquals(order.getId(), updatedByPkOrder.getId());
    }

    @Test
    void testSeveralOrdersForHyp() {
        Order order = createOrder();
        orderService.storeOrder(order);

        Order anotherOrderForThisHypId = new Order(HYP_ID, "AAA", "+7-666-555-77-77", "Unit Test");
        orderService.storeOrder(anotherOrderForThisHypId);

        List<Order> orders = orderService.loadOrders(HYP_ID);
        assertEquals(2, orders.size());
        assertNotEquals(orders.get(0).getId(), orders.get(1).getId());
        assertTrue(orders.contains(order));
        assertTrue(orders.contains(anotherOrderForThisHypId));
    }

    @Test
    void testOrderLegal() {
        long id = orderService.addLegal(HYP_ID, "ооо рога и копыта", "2344 23423 234");
        assertFalse(orderService.getLegal(HYP_ID).isEmpty());

        orderService.deleteLegal(id);
        assertTrue(orderService.getLegal(HYP_ID).isEmpty());
    }

    static Order createOrder() {
        Order order = new Order(HYP_ID, "C-UNIT-22", "+7-666-555-77-77", "Unit Test");
        order.setDeliveryPrice(BigDecimal.valueOf(33.7));
        return order;
    }
}
