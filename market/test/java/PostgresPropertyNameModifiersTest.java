package ru.yandex.market.javaframework.internal.properties.namemodifier;

import org.junit.jupiter.api.Test;

import static ru.yandex.market.javaframework.internal.properties.namemodifier.PropertyNameModifiersTestUtil.propertyNameModificationTest;

public class PostgresPropertyNameModifiersTest {

    @Test
    public void postgresEmbeddedEnabledPropertyPrefixModifierTest() {
        propertyNameModificationTest(
            "modules.postgres.embedded.enabled",
            "mj.postgres.embedded.enabled",
            "yaml-properties/service.yaml"
        );
    }

    @Test
    public void postgresEmbeddedTypePropertyPrefixModifierTest() {
        propertyNameModificationTest(
            "modules.postgres.embedded.type",
            "mj.postgres.embedded.type",
            "yaml-properties/service.yaml"
        );
    }

    @Test
    public void postgresMonitoringPropertyPrefixModifierTest() {
        propertyNameModificationTest(
            "modules.postgres.monitoring.enabled",
            "mj.postgres.monitoring.enabled",
            "yaml-properties/service.yaml"
        );
    }

    @Test
    public void springDatasourcePropertyPrefixModifierTest() {
        propertyNameModificationTest(
            "modules.postgres.datasource.url",
            "spring.datasource.url",
            "yaml-properties/service.yaml"
        );
    }

    @Test
    public void routingDatasourcePropertyPrefixModifierTest() {
        propertyNameModificationTest(
            "modules.postgres.datasource.routing.write.url",
            "mj.postgres.datasource.routing.write.url",
            "yaml-properties/service.yaml"
        );
    }

    @Test
    public void springLiquibasePropertyPrefixModifierTest() {
        propertyNameModificationTest(
            "modules.postgres.liquibase.change-log",
            "spring.liquibase.change-log",
            "yaml-properties/service.yaml"
        );
    }

    @Test
    public void zonkyPropertyPrefixModifierTest() {
        propertyNameModificationTest(
            "modules.postgres.embedded.zonky.port",
            "mj.zonky.port",
            "yaml-properties/service.yaml"
        );
    }
}
