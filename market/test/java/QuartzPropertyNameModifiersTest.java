package ru.yandex.market.javaframework.internal.properties.namemodifier;

import org.junit.jupiter.api.Test;

import static ru.yandex.market.javaframework.internal.properties.namemodifier.PropertyNameModifiersTestUtil.propertyNameModificationTest;

public class QuartzPropertyNameModifiersTest {

    @Test
    public void mjQuartzPropertyPrefixModifierTest() {
        propertyNameModificationTest(
            "modules.quartz.extensions.jobHistory.logTableName",
            "mj.quartz.jobHistory.logTableName",
            "yaml-properties/service.yaml"
        );
    }

    @Test
    public void springQuartzPropertyPrefixModifierTest() {
        propertyNameModificationTest(
            "modules.quartz.properties.org.quartz.jobStore.isClustered",
            "spring.quartz.properties.org.quartz.jobStore.isClustered",
            "yaml-properties/service.yaml"
        );
    }
}
