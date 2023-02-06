package ru.yandex.market.javaframework.main;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;
import ru.yandex.market.javaframework.main.processors.PropertyPlaceholderArraysProcessor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class PropertyPlaceholderArraysProcessorTest {

    @Test
    public void arrayTest() {
        final String propSourceName = "yaml-properties/service.yaml";
        String prop1 = "test.property[0]";
        String prop2 = "test.property[1]";
        String val1 = "qwerty";
        String val2 = "asdfg";
        final Map<String, Object> propMap = Map.of(
            prop1, val1,
            prop2, val2
        );

        final PropertyPlaceholderArraysProcessor processor =
            new PropertyPlaceholderArraysProcessor();
        ConfigurableEnvironment environment = createEnvWithPropertySourceAndValues(
            propSourceName,
            propMap
        );

        processor.postProcessEnvironment(environment, mock(SpringApplication.class));
        final HashSet<String> res = new HashSet<>(Arrays.asList(
            environment.getProperty("test.property").split(",")
        ));
        assertTrue(res.contains(val1));
        assertTrue(res.contains(val2));
    }

    public static ConfigurableEnvironment createEnvWithPropertySourceAndValues(String propertySourceName,
                                                                               Map<String, Object> props) {
        MockEnvironment env = new MockEnvironment();
        final MapPropertySource propertySource = new MapPropertySource(propertySourceName, props);
        env.getPropertySources().addFirst(propertySource);
        return env;
    }
}
