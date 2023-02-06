package ru.yandex.market.checkout.providers.v2.multicart.request;

import java.math.BigDecimal;

import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.request.CartItemRequest;

public final class CartItemRequestProvider {

    private CartItemRequestProvider() {
    }

    public static CartItemRequest.Builder buildItem() {
        return CartItemRequest.builder()
                .withLabel("labelItem1")
                .withBuyerPrice(BigDecimal.valueOf(200))
                .withFeedId(444L)
                .withOfferId("555L")
                .withBundleId("111")
                .withCount(2);
    }

    public static CartItemRequest fromItem(OrderItem item) {
        return CartItemRequest.builder()
                .withLabel(item.getLabel())
                .withBuyerPrice(item.getBuyerPrice())
                .withFeedId(item.getFeedId())
                .withOfferId(item.getOfferId())
                .withBundleId(item.getBundleId())
                .withCount(item.getCount())
                .build();
    }
}
