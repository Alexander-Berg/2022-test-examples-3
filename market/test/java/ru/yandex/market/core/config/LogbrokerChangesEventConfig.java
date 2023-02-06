package ru.yandex.market.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.core.state.event.BusinessChangesProtoLBEvent;
import ru.yandex.market.core.state.event.ContactChangesProtoLBEvent;
import ru.yandex.market.core.state.event.PartnerAppChangesProtoLBEvent;
import ru.yandex.market.core.state.event.PartnerChangesProtoLBEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;

import static org.mockito.Mockito.mock;

@Configuration
public class LogbrokerChangesEventConfig {
    @Bean
    public LogbrokerEventPublisher<BusinessChangesProtoLBEvent> logbrokerBusinessChangesEventPublisher() {
        return mock(LogbrokerEventPublisher.class);
    }

    @Bean
    public LogbrokerEventPublisher<PartnerChangesProtoLBEvent> logbrokerPartnerChangesEventPublisher() {
        return mock(LogbrokerEventPublisher.class);
    }

    @Bean
    public LogbrokerEventPublisher<PartnerAppChangesProtoLBEvent> logbrokerPartnerAppChangesEventPublisher() {
        return mock(LogbrokerEventPublisher.class);
    }

    @Bean
    public LogbrokerEventPublisher<ContactChangesProtoLBEvent> logbrokerContactChangesEventPublisher() {
        return mock(LogbrokerEventPublisher.class);
    }
}
