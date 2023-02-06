package ru.yandex.market.ocrm.module.order.arbitrage;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.mbi.api.client.MbiApiClient;

public class MbiApiTestConfiguration {
    @Bean
    @Primary
    public MbiApiClient mbiApiClient() {
        return Mockito.mock(MbiApiClient.class);
    }
}
