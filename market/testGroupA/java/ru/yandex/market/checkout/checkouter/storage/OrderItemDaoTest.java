package ru.yandex.market.checkout.checkouter.storage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.BasicOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.storage.item.OrderItemDao;
import ru.yandex.market.checkout.helpers.OrderInsertHelper;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class OrderItemDaoTest extends AbstractWebTestBase {

    @Autowired
    private OrderItemDao orderItemDao;
    @Autowired
    private OrderInsertHelper orderInsertHelper;


    @Test
    public void fixZeroQuantityToNullTest() {
        LocalDateTime startDate = LocalDateTime.now();

        // orders inside search time range
        Order order1 = createOrderWithZeroQuantity();
        Order order2 = createOrderWithZeroQuantity();
        Order order3 = createOrderWithZeroQuantity();

        LocalDateTime endDate = order3.getCreationDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        // order outside search time range
        Order order4 = createOrderWithZeroQuantity();

        List<Long> orderIds = Stream.of(order1, order2, order3, order4)
                .map(BasicOrder::getId)
                .collect(Collectors.toList());

        // 4 total, 3 inside search time range, 2 should be fixed
        Integer count = transactionTemplate.execute(status ->
                orderItemDao.fixZeroQuantities(2, startDate, endDate));
        assertEquals(2, count);

        Map<Long, Order> orders = orderService.getOrders(orderIds);
        assertEquals(4, orders.keySet().size());

        // 2 should be fixed
        long nullQuantityCount = getNullQuantityCount(orders);
        assertEquals(2, nullQuantityCount);

        // 4 total, 1 "broken" left inside search time range, 1 should be fixed
        count = transactionTemplate.execute(status ->
                orderItemDao.fixZeroQuantities(2, startDate, endDate));
        assertEquals(1, count);

        orders = orderService.getOrders(orderIds);
        assertEquals(4, orders.keySet().size());

        // 3 should be fixed total after second try
        nullQuantityCount = getNullQuantityCount(orders);
        assertEquals(3, nullQuantityCount);

    }

    private long getNullQuantityCount(Map<Long, Order> orders) {
        return orders.values().stream()
                .map(Order::getItems)
                .flatMap(Collection::stream)
                .map(OrderItem::getQuantity)
                .filter(Objects::isNull)
                .count();
    }

    Order createOrderWithZeroQuantity() {
        Order order = OrderProvider.getBlueOrder();

        order.setCreationDate(new Date());
        order.getItems().forEach(oi -> oi.setQuantity(BigDecimal.ZERO));

        order.setId(orderInsertHelper.insertOrder(order));

        return order;
    }


}
