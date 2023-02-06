package ru.yandex.market.partner.test.context;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.support.ResourcePropertySource;

public class FunctionalTestPropertiesInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            applicationContext.getEnvironment().getPropertySources().addLast(
                    new ResourcePropertySource(
                            "classpath:ru/yandex/market/partner/test/context/functional-test-config.properties")
            );
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
