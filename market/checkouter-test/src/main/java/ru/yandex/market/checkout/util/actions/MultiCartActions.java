package ru.yandex.market.checkout.util.actions;

import java.util.function.Consumer;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

/**
 * @author : poluektov
 * date: 17.08.17.
 */
@SuppressWarnings("checkstyle:VisibilityModifier")
public final class MultiCartActions {

    public static Consumer<MultiCart> setCartsNull = multiCart -> {
        multiCart.setCarts(null);
    };

    public static Consumer<MultiCart> setDeliveryBuyerAddressNull = multiCart -> {
        multiCart.getCarts().get(0).getDelivery().setBuyerAddress(null);
    };

    public static Consumer<MultiCart> setDeliveryOutletIdNull = multiCart -> {
        multiCart.getCarts().get(0).getDelivery().setOutletId(null);
    };

    public static Consumer<MultiCart> setDeliveryBuyerAddressDefault = multiCart -> {
        multiCart.getCarts().get(0).getDelivery().setBuyerAddress(AddressProvider.getAddress());
    };

    public static Consumer<MultiCart> setDeliveryOutletIdDefault = multiCart -> {
        multiCart.getCarts().get(0).getDelivery().setOutletId(DeliveryProvider.FREE_MARKET_OUTLET_ID);
    };

    public static Consumer<MultiCart> setPaymentMethodNull = multiCart -> {
        multiCart.setPaymentMethod(null);
    };

    private MultiCartActions() {
    }

    public static Consumer<MultiCart> setRegionId(Long regionId) {
        return multiCart -> multiCart.getCarts().get(0).getDelivery().setRegionId(regionId);
    }
}
