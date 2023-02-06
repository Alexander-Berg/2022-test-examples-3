package ru.yandex.market.logistics.iris.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.logistics.iris.configuration.logbroker.LogBrokerProperties;

@Configuration
public class LogbrokerPropertiesConfiguration {

    @Bean
    @ConfigurationProperties(value = "iris.logbroker", prefix = "iris.logbroker")
    public LogBrokerProperties logBrokerProperties() {
        return new LogBrokerProperties();
    }
}
