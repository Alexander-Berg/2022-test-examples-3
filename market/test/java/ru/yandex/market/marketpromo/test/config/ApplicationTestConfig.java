package ru.yandex.market.marketpromo.test.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.marketpromo.config.ApplicationWebConfig;
import ru.yandex.market.marketpromo.core.test.config.ApplicationCoreTestConfig;


@Configuration
@Import({
        ApplicationWebConfig.class,
        ApplicationCoreTestConfig.class
})
@ComponentScan(
        value = "ru.yandex.market.marketpromo.test",
        excludeFilters = @ComponentScan.Filter(Configuration.class)
)
@PropertySource("classpath:ciface-promo-test.properties")
public class ApplicationTestConfig {

}
