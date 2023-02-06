package ru.yandex.market.core.test.context;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.support.ResourcePropertySource;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class TestPropertiesInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            applicationContext.getEnvironment().getPropertySources().addLast(
                    new ResourcePropertySource("classpath:test-context.properties")
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
