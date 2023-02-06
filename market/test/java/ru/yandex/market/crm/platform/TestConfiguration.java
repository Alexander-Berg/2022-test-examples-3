package ru.yandex.market.crm.platform;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.crm.environment.Environment;
import ru.yandex.market.crm.environment.EnvironmentResolver;
import ru.yandex.market.crm.platform.config.PlatformConfiguration;

/**
 * @author zilzilok
 */
@Configuration
@Import(PlatformConfiguration.class)
public class TestConfiguration {

    @Bean
    public EnvironmentResolver environmentResolver() {
        return () -> Environment.PRODUCTION;
    }
}
