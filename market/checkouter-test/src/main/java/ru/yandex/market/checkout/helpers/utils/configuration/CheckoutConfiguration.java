package ru.yandex.market.checkout.helpers.utils.configuration;

import java.util.HashMap;
import java.util.Map;

public class CheckoutConfiguration {

    private final Map<String, MockConfiguration> mockConfigurations = new HashMap<>();
    private final Map<String, CheckoutOptionParameters> checkoutOptionParameters = new HashMap<>();
    private final CheckoutRequestConfiguration checkoutRequestConfiguration = new CheckoutRequestConfiguration();
    private final CheckoutResponseConfiguration checkoutResponseConfiguration = new CheckoutResponseConfiguration();

    public MockConfiguration mocks(String label) {
        return mockConfigurations.computeIfAbsent(label, k -> new MockConfiguration());
    }

    public MockConfiguration addMockConfiguration(String label, MockConfiguration mockConfiguration) {
        return mockConfigurations.putIfAbsent(label, mockConfiguration);
    }

    public Map<String, MockConfiguration> mockConfigurations() {
        return mockConfigurations;
    }

    public CheckoutRequestConfiguration request() {
        return checkoutRequestConfiguration;
    }

    public CheckoutResponseConfiguration response() {
        return checkoutResponseConfiguration;
    }

    public CheckoutOptionParameters orderOption(String label) {
        return checkoutOptionParameters.computeIfAbsent(label, k -> new CheckoutOptionParameters());
    }

    public CheckoutOptionParameters addOrderOptions(String label, CheckoutOptionParameters parameters) {
        return checkoutOptionParameters.put(label, parameters);
    }

    public Map<String, CheckoutOptionParameters> orderOptions() {
        return checkoutOptionParameters;
    }
}
