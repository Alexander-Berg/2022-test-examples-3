package ru.yandex.market.tpl.common.logbroker.config;

import java.util.function.Supplier;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.auth.Credentials;
import ru.yandex.market.tpl.common.logbroker.producer.LogbrokerProducerFactory;
import ru.yandex.market.tpl.common.logbroker.producer.log.ControllerLogLogbrokerEventPublisher;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

@Configuration
public class LogbrokerTestExternalConfig {

    @Bean
    public LogbrokerClientFactory lbkxClientFactory() {
        return mock(LogbrokerClientFactory.class, RETURNS_DEEP_STUBS);
    }

    @Bean
    public Supplier<Credentials> logbrokerCredentialsProvider() {
        return Credentials::none;
    }

    @Bean
    public LogbrokerProducerFactory logbrokerProducerFactory() {
        return mock(LogbrokerProducerFactory.class);
    }

    @Bean
    public ControllerLogLogbrokerEventPublisher controllerLogLogbrokerEventPublisher() {
        return mock(ControllerLogLogbrokerEventPublisher.class);
    }

}
