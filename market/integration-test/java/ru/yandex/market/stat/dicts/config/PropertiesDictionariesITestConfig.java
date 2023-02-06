package ru.yandex.market.stat.dicts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * @author Ekaterina Lebedeva <kateleb@yandex-team.ru>
 */
@Configuration
@Profile({"integration-tests"})
@PropertySources({
    @PropertySource("classpath:dictionaries-yt.properties"),
    @PropertySource("classpath:testing/dictionaries-yt-testing.properties"),
    @PropertySource("classpath:integration-tests.properties"),
    @PropertySource(value = "classpath:local.properties", ignoreResourceNotFound = true),
})
public class PropertiesDictionariesITestConfig {
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
