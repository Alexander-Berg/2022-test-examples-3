package ru.yandex.market.logistics.lom.configuration;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.kikimr.persqueue.producer.async.AsyncProducerConfig;
import ru.yandex.market.logistics.lom.configuration.properties.LogbrokerProperties;

@Configuration
public class LogbrokerConfiguration {

    @Bean
    @ConfigurationProperties("lom.logbroker")
    public LogbrokerProperties lomLogbrokerProperties() {
        return new LogbrokerProperties();
    }

    @Bean
    public AsyncProducerConfig lomAsyncProducerConfig() {
        return AsyncProducerConfig.defaultConfig("topic", "source".getBytes(StandardCharsets.US_ASCII));
    }
}
