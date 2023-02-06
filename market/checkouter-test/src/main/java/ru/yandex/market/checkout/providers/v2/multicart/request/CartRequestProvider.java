package ru.yandex.market.checkout.providers.v2.multicart.request;

import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.request.CartRequest;

public final class CartRequestProvider {

    private CartRequestProvider() {
    }

    public static CartRequest.Builder buildCart() {
        return CartRequest.builder()
                .withLabel("label1")
                .withShopId(123L)
                .withItems(List.of(CartItemRequestProvider.buildItem().build()));
    }

    public static CartRequest fromCart(Order cart) {
        return CartRequest.builder()
                .withLabel(cart.getLabel())
                .withShopId(cart.getShopId())
                .withItems(cart.getItems()
                        .stream()
                        .map(CartItemRequestProvider::fromItem)
                        .collect(Collectors.toList()))
                .build();
    }
}
