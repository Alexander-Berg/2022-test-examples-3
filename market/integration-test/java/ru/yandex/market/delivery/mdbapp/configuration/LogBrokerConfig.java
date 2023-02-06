package ru.yandex.market.delivery.mdbapp.configuration;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.logbroker.push.PushSession;

@Profile("integration-test")
@Configuration
public class LogBrokerConfig {

    @Bean
    @Primary
    PushSession logBrokerSession() {
        return Mockito.mock(PushSession.class);
    }
}
