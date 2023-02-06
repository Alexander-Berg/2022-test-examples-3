package ru.yandex.market.fulfillment.stockstorage.client;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestContextConfiguration {

    public static final String SERVICE_TICKET = "2011222";

    @Bean
    public TvmClient tvmClient() {
        TvmClient tvmClient = Mockito.mock(TvmClient.class);
        when(tvmClient.getServiceTicketFor(anyInt())).thenReturn(SERVICE_TICKET);
        return tvmClient;
    }
}
