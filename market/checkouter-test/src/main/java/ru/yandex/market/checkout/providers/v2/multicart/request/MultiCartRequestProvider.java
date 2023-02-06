package ru.yandex.market.checkout.providers.v2.multicart.request;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackOption;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.request.BnplInfoRequest;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.request.MultiCartRequest;
import ru.yandex.market.checkout.helpers.utils.configuration.ActualizationRequestConfiguration;

public final class MultiCartRequestProvider {

    private MultiCartRequestProvider() {
    }

    public static MultiCartRequest.Builder buildRequest() {
        return MultiCartRequest.builder()
                .withBuyer(BuyerRequestProvider.buildBuyer().build())
                .withCoinIdsToUse(List.of(555L))
                .withSelectedCashbackOption(CashbackOption.EMIT)
                .withBnplInfo(BnplInfoRequest.builder().withSelected(true).build())
                .withCarts(List.of(CartRequestProvider.buildCart().build()));
    }

    @Nonnull
    public static MultiCartRequest fromMultiCart(
            @Nonnull ActualizationRequestConfiguration requestConfiguration,
            @Nonnull MultiCart multiCart) {
        return MultiCartRequest.builder()
                .withBuyer(BuyerRequestProvider.fromBuyer(requestConfiguration, multiCart))
                .withCoinIdsToUse(multiCart.getCoinIdsToUse())
                .withSelectedCashbackOption(multiCart.getSelectedCashbackOption())
                .withBnplInfo(multiCart.getBnplInfo() == null ? null :
                        BnplInfoRequest.builder()
                                .withSelected(multiCart.getBnplInfo().isSelected())
                                .build())
                .withCarts(multiCart.getCarts()
                        .stream()
                        .map(CartRequestProvider::fromCart)
                        .collect(Collectors.toList()))
                .withSelectedCashbackOption(multiCart.getSelectedCashbackOption())
                .build();
    }
}
