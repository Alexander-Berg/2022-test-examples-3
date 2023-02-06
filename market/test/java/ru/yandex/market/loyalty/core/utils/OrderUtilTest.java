package ru.yandex.market.loyalty.core.utils;

import java.util.Set;

import java.math.BigDecimal;

import org.junit.Test;

import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.loyalty.core.model.order.Item;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class OrderUtilTest {
    private final FeedOfferId defaultFeedOfferId = new FeedOfferId("feedOfferId", 666L);

    @Test
    public void testNormalizePhoneNumber() {
        assertEquals("+79091544255", OrderUtil.normalizePhoneNumber("89091544255"));
        assertEquals("+79091544255", OrderUtil.normalizePhoneNumber("8 (909) 154-42-55"));
    }
    @Test
    public void testNormalizeOrderIdsString() {
        assertEquals("1", OrderUtil.normalizeOrderIdsString(Set.of(1L)));
        assertEquals("1-2", OrderUtil.normalizeOrderIdsString(Set.of(1L, 2L)));
        assertEquals("1, 3", OrderUtil.normalizeOrderIdsString(Set.of(1L, 3L)));
        assertEquals("1-3", OrderUtil.normalizeOrderIdsString(Set.of(1L, 2L, 3L)));
        assertEquals("1-2, 4", OrderUtil.normalizeOrderIdsString(Set.of(1L, 2L, 4L)));
        assertEquals("1, 3-4", OrderUtil.normalizeOrderIdsString(Set.of(1L, 3L, 4L)));
        assertEquals("1, 3, 5", OrderUtil.normalizeOrderIdsString(Set.of(1L, 3L, 5L)));
        assertEquals("1-4, 6", OrderUtil.normalizeOrderIdsString(Set.of(6L, 4L, 1L, 3L, 2L))); // unsorted
    }

    @Test
    public void shouldUseCountPriceIfQuantityIsMissing() {
        int count = 6;
        BigDecimal quantity = null;
        BigDecimal buyerPrice = BigDecimal.valueOf(115);
        BigDecimal quantPrice = BigDecimal.valueOf(44.2);

        OrderItem item = buildItem(count, quantity, buyerPrice, quantPrice);

        Item convertOrderItem = OrderUtil.convertOrderItem(item);
        assertThat(convertOrderItem.getQuantity(), is(BigDecimal.valueOf(count)));
        assertThat(convertOrderItem.getPrice(), is(buyerPrice));
    }

    @Test
    public void shouldUseCountPriceIfQuantPriceIsMissing() {
        int count = 6;
        BigDecimal quantity = BigDecimal.valueOf(4.2);
        BigDecimal buyerPrice = BigDecimal.valueOf(115);
        BigDecimal quantPrice = null;

        OrderItem item = buildItem(count, quantity, buyerPrice, quantPrice);

        Item convertOrderItem = OrderUtil.convertOrderItem(item);
        assertThat(convertOrderItem.getQuantity(), is(BigDecimal.valueOf(count)));
        assertThat(convertOrderItem.getPrice(), is(buyerPrice));
    }

    @Test
    public void shouldUseQuantityAndQuantPrice() {
        int count = 6;
        BigDecimal quantity = BigDecimal.valueOf(13);
        BigDecimal buyerPrice = BigDecimal.valueOf(115);
        BigDecimal quantPrice = BigDecimal.valueOf(32);

        OrderItem item = buildItem(count, quantity, buyerPrice, quantPrice);

        Item convertOrderItem = OrderUtil.convertOrderItem(item);
        assertThat(convertOrderItem.getQuantity(), is(quantity));
        assertThat(convertOrderItem.getPrice(), is(quantPrice));
    }

    private OrderItem buildItem(int count, BigDecimal quantity, BigDecimal buyerPrice,
                                BigDecimal quantPrice) {
        OrderItem item = new OrderItem();
        item.setFeedOfferId(defaultFeedOfferId);
        item.setCount(count);
        item.setBuyerPrice(buyerPrice);
        item.setQuantity(quantity);
        item.setQuantPrice(quantPrice);
        return item;
    }
}
