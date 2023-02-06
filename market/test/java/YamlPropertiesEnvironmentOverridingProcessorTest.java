package ru.yandex.market.javaframework.main;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import ru.yandex.market.application.properties.utils.Environments;
import ru.yandex.market.javaframework.internal.environment.test.EnvironmentExtension;
import ru.yandex.market.javaframework.internal.environment.test.TestEnvironment;
import ru.yandex.market.javaframework.main.processors.YamlPropertiesEnvironmentOverridingProcessor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(EnvironmentExtension.class)
public class YamlPropertiesEnvironmentOverridingProcessorTest {

    @Test
    @TestEnvironment(Environments.PRODUCTION)
    public void envOverridingTest() {
        final String propSourceName = "yaml-properties/service.yaml";
        String prop = "java_service.service_name";
        String val1 = "qwerty";
        String val2 = "asdfg";
        final Map<String, Object> propMap = Map.of(
            prop, val1,
            "java_service.env." + Environments.PRODUCTION + ".service_name", val2
        );

        final YamlPropertiesEnvironmentOverridingProcessor processor =
            new YamlPropertiesEnvironmentOverridingProcessor();
        ConfigurableEnvironment environment = createEnvWithPropertySourcesAndValues(
            Map.of(
                propSourceName,
                propMap
            )
        );

        processor.postProcessEnvironment(environment, mock(SpringApplication.class));

        assertEquals(val2, environment.getProperty(prop));
    }

    @Test
    @TestEnvironment(Environments.PRODUCTION)
    public void clientEnvResolving_WithDstEnv_Test() {
        String clientId = "test";
        final String clientPropSourceName = "applicationConfig: [classpath:yaml-properties/client_" + clientId + ".yaml]";
        String prodClientVal = "qwerty";
        String testingClientVal = "asdfg";

        final Map<String, Object> clientPropMap = Map.of(
            "client-services-properties."
                + clientId + ".service-yaml.java_service.env." + Environments.PRODUCTION + ".service_name",
            prodClientVal,
            "client-services-properties."
                + clientId + ".service-yaml.java_service.env." + Environments.TESTING + ".service_name",
            testingClientVal
        );

        final String serviceYamlPropSourceName = "yaml-properties/service.yaml";
        String dstEnvProp = "clients.list.test.dstEnv";
        String clientDstEnv = Environments.TESTING;

        final Map<String, Object> serviceYamlPropMap = Map.of(
            dstEnvProp, clientDstEnv
        );

        final YamlPropertiesEnvironmentOverridingProcessor processor =
            new YamlPropertiesEnvironmentOverridingProcessor();
        ConfigurableEnvironment environment = createEnvWithPropertySourcesAndValues(
            Map.of(
                serviceYamlPropSourceName,
                serviceYamlPropMap,
                clientPropSourceName,
                clientPropMap
            )
        );

        processor.postProcessEnvironment(environment, mock(SpringApplication.class));

        assertEquals(testingClientVal, environment.getProperty("client-services-properties."
            + clientId + ".service-yaml.java_service.service_name"));
    }

    @Test
    @TestEnvironment(Environments.PRODUCTION)
    public void clientEnvResolving_WithoutDstEnv_Test() {
        String clientId = "test";
        final String clientPropSourceName = "applicationConfig: [classpath:yaml-properties/client_" + clientId + ".yaml]";
        String prodClientVal = "qwerty";
        String testingClientVal = "asdfg";

        final Map<String, Object> clientPropMap = Map.of(
            "client-services-properties."
                + clientId + ".service-yaml.java_service.env." + Environments.PRODUCTION + ".service_name",
            prodClientVal,
            "client-services-properties."
                + clientId + ".service-yaml.java_service.env." + Environments.TESTING + ".service_name",
            testingClientVal
        );

        final String serviceYamlPropSourceName = "yaml-properties/service.yaml";
        String clientProp = "clients.list.test.openapi_spec_path";
        String clientPropVal = "fsdfsd/dfsfs/sdfsd/api.yaml";

        final Map<String, Object> serviceYamlPropMap = Map.of(
            clientProp, clientPropVal
        );

        final YamlPropertiesEnvironmentOverridingProcessor processor =
            new YamlPropertiesEnvironmentOverridingProcessor();
        ConfigurableEnvironment environment = createEnvWithPropertySourcesAndValues(
            Map.of(
                serviceYamlPropSourceName,
                serviceYamlPropMap,
                clientPropSourceName,
                clientPropMap
            )
        );

        processor.postProcessEnvironment(environment, mock(SpringApplication.class));

        assertEquals(prodClientVal, environment.getProperty("client-services-properties."
            + clientId + ".service-yaml.java_service.service_name"));
    }

    public static ConfigurableEnvironment createEnvWithPropertySourcesAndValues(
        Map<String, Map<String, Object>> propsWithPropSourceNames
    ) {
        MockEnvironment env = new MockEnvironment();
        for (Map.Entry<String, Map<String, Object>> entry : propsWithPropSourceNames.entrySet()) {
            final MapPropertySource propertySource = new MapPropertySource(entry.getKey(), entry.getValue());
            env.getPropertySources().addFirst(propertySource);
        }
        return env;
    }
}
