package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstance;
import ru.yandex.market.checkout.checkouter.util.OrderItemInstancesUtil;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DivideItemsPredicateTest extends AbstractServicesTestBase {

    @Autowired
    private DivideItemsPredicate divideItemsPredicate;

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    public void testSplitSingleItemWithInstances(int count) {
        OrderItem orderItem = itemWithCount(count);
        orderItem.setInstances(OrderItemInstancesUtil.convertToNode(List.of(new OrderItemInstance("cis"))));
        assertTrue(divideItemsPredicate.test(orderItem));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    public void testSplitItem(int count) {
        assertTrue(divideItemsPredicate.test(itemWithCount(count)));
    }

    @Test
    public void testSplitBulkItem() {
        assertFalse(divideItemsPredicate.test(bulkItem()));
    }

    private OrderItem itemWithCount(int count) {
        return OrderItemProvider.orderItemBuilder()
                .count(count)
                .quantity(BigDecimal.valueOf(count))
                .build();
    }

    private OrderItem bulkItem() {
        return OrderItemProvider.orderItemBuilder()
                .quantity(new BigDecimal("0.98"))
                .build();
    }
}
