package ru.yandex.market.checkout.checkouter.test.config.services;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;

import ru.yandex.market.checkout.common.TestHelper;

@Configuration
@ComponentScan(
        value = "ru.yandex.market.checkout.helpers",
        includeFilters = {
                @ComponentScan.Filter(TestHelper.class)
        }
)
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class IntTestCommonConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer =
                new PropertySourcesPlaceholderConfigurer();
        propertySourcesPlaceholderConfigurer.setLocalOverride(true);
        propertySourcesPlaceholderConfigurer.setIgnoreResourceNotFound(false);
        propertySourcesPlaceholderConfigurer.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return propertySourcesPlaceholderConfigurer;
    }
}
