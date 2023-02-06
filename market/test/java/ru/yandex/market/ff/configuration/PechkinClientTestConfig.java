package ru.yandex.market.ff.configuration;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;

import static org.mockito.Mockito.mock;

@Configuration
public class PechkinClientTestConfig {

    @Bean
    public PechkinHttpClient pechkinHttpClient() {
        return mock(PechkinHttpClient.class);
    }

}
