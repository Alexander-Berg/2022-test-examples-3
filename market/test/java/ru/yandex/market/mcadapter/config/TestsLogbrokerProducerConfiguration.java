package ru.yandex.market.mcadapter.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.event.LogbrokerEvent;

/**
 * @author zagidullinri
 * @date 13.07.2022
 */
@Configuration
public class TestsLogbrokerProducerConfiguration {
    @Bean
    public LogbrokerEventPublisher<LogbrokerEvent<?>> logbrokerEventPublisher() {
        return Mockito.mock(LogbrokerEventPublisher.class);
    }
}
