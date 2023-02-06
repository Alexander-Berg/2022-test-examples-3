package ru.yandex.market.mcrp_request.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import ru.yandex.market.mcrp_request.clients.AbcApiClient;

@Import({FormsConfig.class})
public class McrpTestConfig {
    @Bean
    public AbcApiClient abcApiClient() {
        return Mockito.mock(AbcApiClient.class);
    }
}
