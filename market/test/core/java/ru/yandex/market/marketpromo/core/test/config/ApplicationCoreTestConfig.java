package ru.yandex.market.marketpromo.core.test.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.marketpromo.core.config.ApplicationCoreInternalConfig;
import ru.yandex.market.marketpromo.core.config.ApplicationSecurityConfig;

@TestConfiguration
@Import({
        ApplicationCoreInternalConfig.class,
        ApplicationCoreExternalTestConfig.class,
        ApplicationSecurityConfig.class
})
@ComponentScan(
        basePackages = "ru.yandex.market.marketpromo.core.test",
        excludeFilters = @ComponentScan.Filter(Configuration.class)
)
@PropertySource("classpath:ciface-promo-core.properties")
public class ApplicationCoreTestConfig {

}
