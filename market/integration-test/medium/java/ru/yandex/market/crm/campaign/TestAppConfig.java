package ru.yandex.market.crm.campaign;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import ru.yandex.market.crm.core.test.TestEnvironmentResolver;
import ru.yandex.market.crm.environment.EnvironmentResolver;

/**
 * Created by vivg on 23.06.17.
 */
@Configuration
@ComponentScan(
        value = "ru.yandex.market.crm.campaign",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "ru\\.yandex\\.market\\.crm\\.campaign\\.http\\..*"
        )
)
public class TestAppConfig {

    @Bean
    public static EnvironmentResolver getEnvironmentProvider() {
        return new TestEnvironmentResolver();
    }
}
