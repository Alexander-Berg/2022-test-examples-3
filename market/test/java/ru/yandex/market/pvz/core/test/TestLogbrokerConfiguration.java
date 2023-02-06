package ru.yandex.market.pvz.core.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tpl.common.logbroker.producer.LogbrokerProducerFactory;

import static org.mockito.Mockito.mock;

/**
 * @author valeriashanti
 */
@Configuration
public class TestLogbrokerConfiguration {

    @Bean
    public LogbrokerProducerFactory logbrokerProducerFactory() {
        return mock(LogbrokerProducerFactory.class);
    }

}
