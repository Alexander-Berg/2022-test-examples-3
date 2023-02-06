package ru.yandex.market.checkout.util;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.Order;

public final class OrderUtils {

    private OrderUtils() {
    }

    public static Order firstOrder(MultiCart cart) {
        return cart.getCarts().get(0);
    }
}
