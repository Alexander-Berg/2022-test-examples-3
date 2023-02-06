package ru.yandex.market.checkout.util.balance;


import java.util.function.Consumer;

public class TrustParameters {

    private Consumer<TrustMockConfigurer> customTrustMockConfiguration = null;

    public Consumer<TrustMockConfigurer> getCustomTrustMockConfiguration() {
        return customTrustMockConfiguration;
    }

    public TrustParameters setCustomTrustMockConfiguration(Consumer<TrustMockConfigurer> customTrustMockConfiguration) {
        this.customTrustMockConfiguration = customTrustMockConfiguration;
        return this;
    }
}
