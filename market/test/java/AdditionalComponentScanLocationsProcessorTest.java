package ru.yandex.market.javaframework.main;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;
import ru.yandex.market.javaframework.main.processors.AdditionalComponentScanLocationsProcessor;
import ru.yandex.market.javaframework.yamlproperties.serviceyaml.javaservice.JavaService;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.javaframework.main.processors.AdditionalComponentScanLocationsProcessor.PACKAGES_TO_SCAN_PROP_NAME;

public class AdditionalComponentScanLocationsProcessorTest {

    @Test
    public void packagesToScanTest() {
        final String propSourceName = "yaml-properties/service.yaml";
        String prop1 = PACKAGES_TO_SCAN_PROP_NAME + "[0]";
        String prop2 = PACKAGES_TO_SCAN_PROP_NAME + "[1]";
        String val1 = "qwerty";
        String val2 = "asdfg";
        final Map<String, Object> propMap = Map.of(
            "java_service.service_name", "test_service",
            prop1, val1,
            prop2, val2
        );

        final AdditionalComponentScanLocationsProcessor processor =
            new AdditionalComponentScanLocationsProcessor();
        ConfigurableEnvironment environment = createEnvWithPropertySourceAndValues(
            propSourceName,
            propMap
        );

        processor.postProcessEnvironment(environment, mock(SpringApplication.class));

        final Binder binder = new Binder(
            ConfigurationPropertySources.from(
                environment.getPropertySources().get(propSourceName)
            )
        );
        final JavaService javaService = binder.bind("java-service", JavaService.class).get();
        final Set<String> packagesToScan = javaService.getPackagesToScan();
        assertEquals(3, packagesToScan.size());
        assertTrue(packagesToScan.contains(val1));
        assertTrue(packagesToScan.contains(val2));
        assertTrue(packagesToScan.contains("ru.yandex.market.test.service.api"));
    }

    @Test
    public void rootPackageOnlyTest() {
        final String propSourceName = "yaml-properties/service.yaml";
        final Map<String, Object> propMap = Map.of(
            "java_service.service_name", "test_service"
        );

        final AdditionalComponentScanLocationsProcessor processor =
            new AdditionalComponentScanLocationsProcessor();
        ConfigurableEnvironment environment = createEnvWithPropertySourceAndValues(
            propSourceName,
            propMap
        );

        processor.postProcessEnvironment(environment, mock(SpringApplication.class));

        final Binder binder = new Binder(
            ConfigurationPropertySources.from(
                environment.getPropertySources().get(propSourceName)
            )
        );
        final JavaService javaService = binder.bind("java-service", JavaService.class).get();
        final Set<String> packagesToScan = javaService.getPackagesToScan();
        assertEquals(1, packagesToScan.size());
        assertTrue(packagesToScan.contains("ru.yandex.market.test.service"));
    }

    public static ConfigurableEnvironment createEnvWithPropertySourceAndValues(String propertySourceName,
                                                                               Map<String, Object> props) {
        MockEnvironment env = new MockEnvironment();
        final MapPropertySource propertySource = new MapPropertySource(propertySourceName, props);
        env.getPropertySources().addFirst(propertySource);
        return env;
    }
}
