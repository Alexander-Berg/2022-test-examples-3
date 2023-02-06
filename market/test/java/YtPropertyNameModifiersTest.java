package ru.yandex.market.javaframework.internal.properties.namemodifier;

import org.junit.jupiter.api.Test;

import static ru.yandex.market.javaframework.internal.properties.namemodifier.PropertyNameModifiersTestUtil.propertyNameModificationTest;

public class YtPropertyNameModifiersTest {

    @Test
    public void ytPropertyPrefixModifierTest() {
        propertyNameModificationTest(
            "modules.yt.rpc.custom",
            "mj.yt.rpc.custom",
            "yaml-properties/service.yaml"
        );
    }
}
