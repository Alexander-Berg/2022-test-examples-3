package ru.yandex.market.delivery.transport_manager.config;

import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.kikimr.persqueue.producer.async.AsyncProducerConfig;

@Configuration
public class LogbrokerConfiguration {
    @Bean
    public AsyncProducerConfig asyncProducerConfig() {
        return AsyncProducerConfig.defaultConfig("topic", "source".getBytes(StandardCharsets.UTF_8));
    }
}
