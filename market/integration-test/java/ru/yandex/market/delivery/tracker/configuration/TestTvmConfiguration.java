package ru.yandex.market.delivery.tracker.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.logistics.util.client.TvmTicketProvider;

@Configuration
public class TestTvmConfiguration {
    private static final String SERVICE_TICKET = "SERVICE_TICKET";

    @Bean
    public TvmTicketProvider testTvmTicketProvider() {
        return () -> SERVICE_TICKET;
    }
}
