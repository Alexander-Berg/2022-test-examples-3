package ru.yandex.market.providers;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.shopadminstub.model.CartRequest;
import ru.yandex.market.shopadminstub.model.Item;

public abstract class CartRequestProvider {
    public static final int DEFAULT_REGION = 213;
    public static final int DEFAULT_REGION_2 = 2;

    private static final Color DEFAULT_COLOR = Color.BLUE;
    private static final String DEFAULT_CURRENCY = "RUR";
    private static final Currency DEFAULT_DELIVERY_CURRENCY = null;
    private static final boolean DEFAULT_FULFILMENT = false;

    public static CartRequest buildCartRequest() {
        return buildCartRequest(ItemProvider.buildDefaultItem());
    }

    public static CartRequest buildCartRequest(Item... items) {
        CartRequest cartRequest = new CartRequest();

        cartRequest.setCurrency(DEFAULT_CURRENCY);
        cartRequest.setDeliveryCurrency(DEFAULT_DELIVERY_CURRENCY);

        cartRequest.setFulfilment(DEFAULT_FULFILMENT);
        cartRequest.setRgb(DEFAULT_COLOR);

        cartRequest.setRegionId(DEFAULT_REGION);

        for(Item item: items) {
            cartRequest.getItems().put(item.getOfferItemKey(), item);
        }

        return cartRequest;
    }
}
