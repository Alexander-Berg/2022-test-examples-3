package ru.yandex.market.ff.configuration;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.abo.api.client.AboAPI;

@Configuration
public class AboIntegrationTestConfiguration {

    @Bean
    public AboAPI getAboApi() {
        return Mockito.mock(AboAPI.class);
    }
}
