package ru.yandex.market.bidding.model.config;

import java.util.Properties;

import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestPlaceholderConfig {
    @Bean
    public PlaceholderConfigurerSupport placeholderConfigurerSupport() {
        final PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setProperties(testProperties());
        configurer.setIgnoreUnresolvablePlaceholders(true);
        return configurer;
    }

    private Properties testProperties() {
        Properties properties = new Properties();
        properties.setProperty("bid.max", "8400");
        properties.setProperty("bid.min", "1");
        return properties;
    }

}
