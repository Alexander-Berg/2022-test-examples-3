package ru.yandex.market.communication.proxy.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.passport.tvmauth.TvmClient;

/**
 * @author i-shunkevich
 * @date 28.06.2022
 */
@Configuration
public class TvmConfig {

    @Bean("tvmClient")
    public TvmClient tvmClientDevelopment() {
        return Mockito.mock(TvmClient.class);
    }
}
