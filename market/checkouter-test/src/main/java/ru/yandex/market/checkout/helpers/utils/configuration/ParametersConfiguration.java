package ru.yandex.market.checkout.helpers.utils.configuration;

import ru.yandex.market.checkout.checkouter.order.Order;

public class ParametersConfiguration {

    private final ActualizationConfiguration actualizationConfiguration = new ActualizationConfiguration();
    private final CheckoutConfiguration checkoutConfiguration = new CheckoutConfiguration();

    public ActualizationConfiguration cart() {
        return actualizationConfiguration;
    }

    public MockConfiguration cart(Order order) {
        return cart().mocks(order.getLabel());
    }

    public CheckoutConfiguration checkout() {
        return checkoutConfiguration;
    }
}
