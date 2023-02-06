package ru.yandex.market.javaframework.internal.properties.namemodifier;

import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class PropertyNameModifiersTestUtil {

    public static void propertyNameModificationTest(String originalPropName,
                                                    String expectedPropName,
                                                    String propertySourceName) {
        PropertyNameModifierProcessor processor = new PropertyNameModifierProcessor();

        final String propVal = "someVal";

        ConfigurableEnvironment environment = createEnvWithPropertySourceAndValues(
            propertySourceName,
            Map.of(originalPropName, propVal)
        );

        processor.postProcessEnvironment(environment, mock(SpringApplication.class));

        assertThat(environment.getProperty(originalPropName)).isNull();
        assertThat(environment.getProperty(expectedPropName)).isEqualTo(propVal);
    }

    public static ConfigurableEnvironment createEnvWithPropertySourceAndValues(String propertySourceName,
                                                                               Map<String, Object> props) {
        MockEnvironment env = new MockEnvironment();
        final MapPropertySource propertySource = new MapPropertySource(propertySourceName, props);
        env.getPropertySources().addFirst(propertySource);
        return env;
    }
}
