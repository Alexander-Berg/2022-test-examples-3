package ru.yandex.market.javaframework.internal.properties.namemodifier;

import org.junit.jupiter.api.Test;

import static ru.yandex.market.javaframework.internal.properties.namemodifier.PropertyNameModifiersTestUtil.propertyNameModificationTest;

public class MongoPropertyNameModifiersTest {

    @Test
    public void mongoEmbeddedEnabledPropertyPrefixModifierTest() {
        propertyNameModificationTest(
            "modules.mongo.embedded.enabled",
            "mj.mongo.embedded.enabled",
            "yaml-properties/service.yaml"
        );
    }

    @Test
    public void mongoEmbeddedPropertyPrefixModifierTest() {
        propertyNameModificationTest(
            "modules.mongo.embedded",
            "spring.mongodb.embedded",
            "yaml-properties/service.yaml"
        );
    }

    @Test
    public void mongoNetworkPropertyPrefixModifierTest() {
        propertyNameModificationTest(
            "modules.mongo",
            "spring.data.mongodb",
            "yaml-properties/service.yaml"
        );
    }
}
