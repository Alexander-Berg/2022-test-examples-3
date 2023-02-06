package ru.yandex.market.crm.operatorwindow.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.ocrm.module.checkouter.old.OrderItemComparator;

import static org.hamcrest.core.Is.is;

public class OrderItemComparatorTest {

    @Test
    public void simple() {
        List<OrderItem> items = new ArrayList<>();

        items.add(getOrderItem(x -> x.setOrderItemId(1).setBundleId("123").setPrice(1)));
        items.add(getOrderItem(x -> x.setOrderItemId(2).setBundleId("123").setPrice(10)));
        items.add(getOrderItem(x -> x.setOrderItemId(3).setBundleId("").setPrice(1)));
        items.add(getOrderItem(x -> x.setOrderItemId(4).setBundleId("").setPrice(30)));
        items.add(getOrderItem(x -> x.setOrderItemId(5).setPrice(123)));
        items.add(getOrderItem(x -> x.setOrderItemId(6).setPrice(123)));
        items.add(getOrderItem(x -> x.setOrderItemId(7).setPrice(123)));
        items.add(getOrderItem(x -> x.setOrderItemId(8).setPrice(456)));
        items.add(getOrderItem(x -> x.setOrderItemId(9).setBundleId("456").setPrice(20)));
        items.add(getOrderItem(x -> x.setOrderItemId(10).setBundleId("456").setPrice(1)));

        Collections.sort(items, new OrderItemComparator());

        final List<Long> actual = items.stream()
                .map(OrderItem::getId)
                .collect(Collectors.toList());
        MatcherAssert.assertThat(longList(4, 3, 2, 1, 9, 10, 8, 5, 6, 7), is(actual));
    }

    @Test
    public void canHandleNullFields() {
        List<OrderItem> items = new ArrayList<>();

        items.add(getOrderItem(x -> x.setOrderItemId(1).setBundleId("123").setPrice(1)));
        items.add(getOrderItem(x -> x.setOrderItemId(2).setBundleId("123").setPrice(10)));
        items.add(getOrderItem(x -> {
        }));
        items.add(getOrderItem(x -> {
        }));

        Collections.sort(items, new OrderItemComparator());
    }

    private List<Long> longList(int... values) {
        return Arrays.stream(values)
                .boxed()
                .map(x -> (long) x)
                .collect(Collectors.toList());
    }

    private OrderItem getOrderItem(Consumer<OrderItemBuilder> modifier) {
        final OrderItemBuilder orderItemBuilder = new OrderItemBuilder();
        modifier.accept(orderItemBuilder);
        return orderItemBuilder.build();
    }

    private static class OrderItemBuilder {
        private Long id;
        private String bundleId;
        private BigDecimal price;

        public OrderItemBuilder setOrderItemId(int id) {
            this.id = (long) id;
            return this;
        }

        public OrderItemBuilder setBundleId(String bundleId) {
            this.bundleId = bundleId;
            return this;
        }

        public OrderItemBuilder setPrice(long price) {
            this.price = BigDecimal.valueOf(price);
            return this;
        }

        public OrderItem build() {
            final OrderItem orderItem = new OrderItem();
            orderItem.setId(id);
            orderItem.setBundleId(bundleId);
            orderItem.setBuyerPrice(price);
            return orderItem;
        }
    }
}
