package ru.yandex.market.common.sentryerrorbooster;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@Import(SentryErrorBoosterConfig.class)
@PropertySource(value = {"functional-test.properties"})
public class SentryErrorBoosterTestConfig {

    protected SentryErrorBoosterTestConfig()  {
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
