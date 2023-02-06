package ru.yandex.market.helpers;

import ru.yandex.market.providers.OrderAcceptRequestProvider;
import ru.yandex.market.shopadminstub.model.OrderAcceptRequest;

public class OrderAcceptParameters {
    public static final long DEFAULT_SHOP_ID = 242102L;

    private long shopId;
    private OrderAcceptRequest orderAcceptRequest;

    public OrderAcceptParameters() {
        this(OrderAcceptRequestProvider.buildOrderAcceptRequest());
    }

    public OrderAcceptParameters(OrderAcceptRequest orderAcceptRequest) {
        this.shopId = DEFAULT_SHOP_ID;
        this.orderAcceptRequest = orderAcceptRequest;
    }

    public long getShopId() {
        return shopId;
    }

    public OrderAcceptRequest getOrderAcceptRequest() {
        return orderAcceptRequest;
    }
}
