package ru.yandex.market.stat.dicts.integration.conf;

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
  //  @PropertySource("classpath:dictionaries-yt.properties"),
    @PropertySource("classpath:dictionaries-yt-testing.properties"),
    @PropertySource(value = "classpath:local.properties", ignoreResourceNotFound = true),
})
public class PropertiesDictionariesUTestConfig {
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
