package ru.yandex.market.partner.notification.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.passport.tvmauth.TvmClient;

@Configuration
public class TestTVMConfig {

    @Bean
    public TvmClient tvmClient() {
        return Mockito.mock(TvmClient.class);
    }
}
