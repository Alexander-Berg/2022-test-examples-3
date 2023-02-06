package ru.yandex.market.javaframework.healz.test.processors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;

@Order(1)
public class HealzTestEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String MANAGEMENT_PORT_PROPERTY = "management.server.port";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        MapPropertySource source = (MapPropertySource) environment.getPropertySources()
            .get(TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);

        source.getSource().put(MANAGEMENT_PORT_PROPERTY, "0");
    }
}
