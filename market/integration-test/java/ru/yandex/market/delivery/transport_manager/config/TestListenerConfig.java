package ru.yandex.market.delivery.transport_manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.delivery.transport_manager.listener.TestLisneter;

@Configuration
public class TestListenerConfig {
    @Bean
    public TestLisneter testLisneter() {
        return new TestLisneter();
    }
}
