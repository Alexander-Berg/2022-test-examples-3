package ru.yandex.market.checkout.helpers.utils.configuration;

import java.util.HashMap;
import java.util.Map;

public class ActualizationConfiguration {

    private final Map<String, MockConfiguration> mockConfigurations = new HashMap<>();
    private final ActualizationRequestConfiguration actualizationRequestConfiguration =
            new ActualizationRequestConfiguration();
    private final MultiCartMockConfiguration multiCartMockConfiguration = new MultiCartMockConfiguration();
    private final ActualizationResponseConfiguration actualizationResponseConfiguration =
            new ActualizationResponseConfiguration();
    private final ActualizationBodyConfiguration actualizationBodyConfiguration = new ActualizationBodyConfiguration();

    public MockConfiguration mocks(String label) {
        return mockConfigurations.computeIfAbsent(label, k -> new MockConfiguration());
    }

    public MockConfiguration addMockConfiguration(String label, MockConfiguration mockConfiguration) {
        return mockConfigurations.putIfAbsent(label, mockConfiguration);
    }

    public Map<String, MockConfiguration> mockConfigurations() {
        return mockConfigurations;
    }

    public MultiCartMockConfiguration multiCartMocks() {
        return multiCartMockConfiguration;
    }

    public ActualizationRequestConfiguration request() {
        return actualizationRequestConfiguration;
    }

    public ActualizationBodyConfiguration body() {
        return actualizationBodyConfiguration;
    }

    public ActualizationResponseConfiguration response() {
        return actualizationResponseConfiguration;
    }
}
