package ru.yandex.market.analytics.platform.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

/**
 * @author fbokovikov
 */
@Configuration
public class PropertiesConfig {

    @Bean
    public PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        var configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setLocation(new ClassPathResource("resources/functional-test-config.properties"));
        configurer.setOrder(-1);
        configurer.setIgnoreResourceNotFound(false);
        return configurer;
    }
}
