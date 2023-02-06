package ru.yandex.market.crm.operatorwindow.checkout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.common.rest.GenericPage;
import ru.yandex.market.checkout.common.rest.Pager;

public class CheckouterItemsIteratorTest {

    private static final int PAGE_SIZE = 50;

    @Test
    public void totalLessThenPageSize() {
        Iterator<Order> ordersIterator = new CheckouterItemsIterator<>(
                pager -> getOrders(pager, PAGE_SIZE - 1)
        );
        assertOrders(ordersIterator, PAGE_SIZE - 1);
    }

    @Test
    public void totalGreaterThenPageSize() {
        Iterator<Order> ordersIterator = new CheckouterItemsIterator<>(
                pager -> getOrders(pager, PAGE_SIZE + 1)
        );
        assertOrders(ordersIterator, PAGE_SIZE + 1);
    }

    @Test
    public void totalEqualToPageSize() {
        Iterator<Order> ordersIterator = new CheckouterItemsIterator<>(
                pager -> getOrders(pager, PAGE_SIZE)
        );
        assertOrders(ordersIterator, PAGE_SIZE);
    }

    private void assertOrders(Iterator<Order> ordersIterator, int totalCount) {
        List<Order> result = new ArrayList<>();
        ordersIterator.forEachRemaining(result::add);

        Assertions.assertEquals(totalCount, result.size());
        for (int i = 0; i < result.size(); i++) {
            Assertions.assertEquals(Long.valueOf(i), result.get(i).getId());
        }
    }

    private GenericPage<Order> getOrders(Pager pager, int totalCount) {
        return new GenericPage<>(
                Pager.build(pager, totalCount),
                LongStream.range(
                                (pager.getCurrentPage() - 1) * pager.getPageSize(),
                                Integer.min(pager.getCurrentPage() * pager.getPageSize(), totalCount)
                        )
                        .mapToObj(this::createOrder)
                        .collect(Collectors.toList())
        );
    }

    private Order createOrder(long id) {
        Order order = new Order();
        order.setId(id);
        return order;
    }
}
