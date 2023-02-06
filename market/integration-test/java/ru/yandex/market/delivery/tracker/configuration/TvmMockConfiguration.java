package ru.yandex.market.delivery.tracker.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.logistics.util.client.TvmTicketProvider;

@Configuration
public class TvmMockConfiguration {
    public static final String SERVICE_TICKET = "serviceTicket";

    @Bean
    public TvmTicketProvider tvmTicketProvider() {
        return () -> SERVICE_TICKET;
    }
}
