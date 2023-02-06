package ru.yandex.market.core.order.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import ru.yandex.market.checkout.checkouter.order.Color;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author snoop
 */
@SuppressWarnings("methodname")
public class MbiOrderTest {
    private static final String VALID_MAX_SKU = StringUtils.repeat("12ABxz", 3);

    @Test
    public void valid_regular_order() {
        final MbiOrderItem item = createItem(null, null);
        MbiOrder order = createOrder().setItems(Collections.singletonList(item)).build();
        assertFalse(order.isFulfilment());
        assertSame(item, order.getItems().iterator().next());
    }

    @Test
    public void valid_regular_order_fulfilment_explicit_set() {
        final MbiOrderItem item = createItem(null, null);
        MbiOrder order = createOrder().setFulfilment(false).setItems(Collections.singletonList(item)).build();
        assertFalse(order.isFulfilment());
        assertSame(item, order.getItems().iterator().next());
    }

    @Test
    public void valid_fulfilment_order() {
        final MbiOrderItem item = createItem(1L, VALID_MAX_SKU);
        MbiOrder order = createOrder().setFulfilment(true).setItems(Collections.singletonList(item)).build();
        assertTrue(order.isFulfilment());
        assertSame(item, order.getItems().iterator().next());
    }

    @Test
    public void valid_fulfilment_order_unset_sku() {
        final MbiOrderItem item = createItem(1L, null);
        MbiOrder order = createOrder().setFulfilment(true).setItems(Collections.singletonList(item)).build();
        assertTrue(order.isFulfilment());
        assertSame(item, order.getItems().iterator().next());
    }


    @Test(expected = IllegalArgumentException.class)
    public void invalid_fulfilment_order_empty_sku() {
        final MbiOrderItem item = createItem(1L, "");
        createOrder().setFulfilment(true).setItems(Collections.singletonList(item)).build();
    }

    @Test(expected = NullPointerException.class)
    public void invalid_fulfilment_order_unset_ff_shop_id() {
        final MbiOrderItem item = createItem(null, VALID_MAX_SKU);
        createOrder().setFulfilment(true).setItems(Collections.singletonList(item)).build();
    }

    @Test(expected = NullPointerException.class)
    public void invalid_fulfilment_order_invalid_second_item() {
        final MbiOrderItem firstItem = createItem(1L, VALID_MAX_SKU);
        final MbiOrderItem secondItem = createItem(null, VALID_MAX_SKU);
        createOrder().setFulfilment(true).setItems(Arrays.asList(firstItem, secondItem)).build();
    }

    @Test
    public void fulfilment_free_order() {
        //make fulfilment's orders free for a while
        assertTrue(createOrder().setFulfilment(true).build().isFree());
        //and check that false value does not affect the state of free property for non-fulfilment orders
        assertEquals(createOrder().setFulfilment(false).build().isFree(), createOrder().build().isFree());
    }

    @Nonnull
    private MbiOrderItem createItem(Long ffSupplierId, String sku) {
        return MbiOrderItem.builder()
                .setOrderId(1L)
                .setPromos(Collections.emptyList())
                .setFfSupplierId(ffSupplierId)
                .setSku(sku)
                .build();
    }

    @Nonnull
    private MbiOrderBuilder createOrder() {
        final Date now = new Date();
        return new MbiOrderBuilder().
                setId(1).
                setCreationDate(now).
                setStatus(MbiOrderStatus.DELIVERY).
                setTrantime(now).
                setColor(Color.BLUE);
    }
}

