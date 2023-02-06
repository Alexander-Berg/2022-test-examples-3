package ru.yandex.market.checkout.carter.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class CartItemTest {

    @Test
    public void testEqualsAndHashCode() {
        final CartItem firstItem = new CartItem(CartItem.Type.OFFER, "N5gD8a45Brsl8i_NzJLbfg",
                "Погремушка", "16347cf977b48e977f0c655d9c433f05");
        firstItem.setPrimaryInBundle(true);
        firstItem.setCount(1);
        final CartItem secondItem = new CartItem(CartItem.Type.OFFER, "N5gD8a45Brsl8i_NzJLbfg",
                "Погремушка", null);
        secondItem.setId(9302574);
        secondItem.setCount(9);
        final CartItem thirdItem = new CartItem(CartItem.Type.OFFER, "N5gD8a45Brsl8i_NzJLbfg",
                "Погремушка", null);
        thirdItem.setId(11);
        thirdItem.setCount(8);

        assertNotEquals(firstItem, secondItem);
        assertNotEquals(firstItem.hashCode(), secondItem.hashCode());

        assertNotEquals(secondItem, firstItem);
        assertNotEquals(secondItem.hashCode(), firstItem.hashCode());

        assertNotEquals(firstItem, thirdItem);
        assertNotEquals(firstItem.hashCode(), thirdItem.hashCode());

        assertEquals(secondItem, thirdItem);
        assertEquals(secondItem.hashCode(), thirdItem.hashCode());
    }
}
