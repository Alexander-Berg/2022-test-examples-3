package ru.yandex.market.checkout.pushapi.shop;

import ru.yandex.market.checkout.pushapi.client.entity.*;
import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;
import ru.yandex.market.checkout.pushapi.shop.entity.ExternalCart;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrder;

public interface ShopApi {
    
    ShopApiResponse<CartResponse> cart(long shopId, Settings settings, boolean sandbox, ExternalCart cart);
    
    ShopApiResponse<OrderResponse> orderAccept(long shopId, Settings settings, boolean sandbox, ShopOrder order);
    
    ShopApiResponse<Void> changeOrderStatus(long shopId, Settings settings, boolean sandbox, ShopOrder statusChange);

}
