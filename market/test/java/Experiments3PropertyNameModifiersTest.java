package ru.yandex.market.javaframework.internal.properties.namemodifier;

import org.junit.jupiter.api.Test;

import static ru.yandex.market.javaframework.internal.properties.namemodifier.PropertyNameModifiersTestUtil.propertyNameModificationTest;

public class Experiments3PropertyNameModifiersTest {

    @Test
    public void ytPropertyPrefixModifierTest() {
        propertyNameModificationTest(
            "modules.experiments3.port",
            "mj.experiments3.port",
            "yaml-properties/service.yaml"
        );
    }
}
