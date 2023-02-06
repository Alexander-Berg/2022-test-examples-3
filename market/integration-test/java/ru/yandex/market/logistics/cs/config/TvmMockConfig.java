package ru.yandex.market.logistics.cs.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.passport.tvmauth.TvmClient;

@Configuration
public class TvmMockConfig {
    @Primary
    @Bean
    public Tvm2 tvm2() {
        return Mockito.mock(Tvm2.class);
    }

    @Primary
    @Bean
    public TvmClient tvmClient() {
        return Mockito.mock(TvmClient.class);
    }
}
