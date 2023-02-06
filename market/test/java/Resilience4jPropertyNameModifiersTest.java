package ru.yandex.market.javaframework.internal.properties.namemodifier;

import org.junit.jupiter.api.Test;

import static ru.yandex.market.javaframework.internal.properties.namemodifier.PropertyNameModifiersTestUtil.propertyNameModificationTest;

public class Resilience4jPropertyNameModifiersTest {

    @Test
    public void resilience4jRateLimiterPropertyPrefixModifierTest() {
        propertyNameModificationTest(
            "modules.resilience4j.ratelimiter.instances.backendA.limitForPeriod",
            "resilience4j.ratelimiter.instances.backendA.limitForPeriod",
            "yaml-properties/service.yaml"
        );
    }
}
