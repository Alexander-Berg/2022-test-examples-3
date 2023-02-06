package ru.yandex.market.checkout.pushapi.service.shop;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;

public interface ApiService {

    public static final String RESOURCE_CART = "/cart";
    public static final String RESOURCE_CART_WRONG_TOKEN = "/cart/wrong-token";
    public static final String RESOURCE_ORDER_ACCEPT = "/order/accept";
    public static final String RESOURCE_ORDER_STATUS = "/order/status";

    CartResponse cart(long shopId, long uid, Cart cart, boolean sandbox);
    OrderResponse orderAccept(long shopId, Order order, boolean sandbox);
    void orderStatus(long shopId, Order statusChange, boolean sandbox);
    void settings(long shopId, Settings settings);
    Settings getSettings(long shopId);
    void wrongTokenCart(long shopId, long uid, Cart cart, boolean sandbox);
    
}
