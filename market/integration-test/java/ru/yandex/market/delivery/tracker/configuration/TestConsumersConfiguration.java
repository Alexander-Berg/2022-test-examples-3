package ru.yandex.market.delivery.tracker.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.delivery.tracker.client.pushing.ConsumersNotificationClient;

import static org.mockito.Mockito.mock;

@Configuration
public class TestConsumersConfiguration {

    @Bean
    @Primary
    public ConsumersNotificationClient testConsumer() {
        return mock(ConsumersNotificationClient.class);
    }

    @Bean
    public ConsumersNotificationClient lom() {
        return mock(ConsumersNotificationClient.class);
    }
}
