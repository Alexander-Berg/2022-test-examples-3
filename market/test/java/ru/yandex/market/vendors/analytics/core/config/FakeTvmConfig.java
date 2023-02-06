package ru.yandex.market.vendors.analytics.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ru.yandex.market.vendors.analytics.core.tvm.FakeTvmClient;
import ru.yandex.passport.tvmauth.TvmClient;

@Configuration
@Profile("functionalTest")
public class FakeTvmConfig {
    @Bean
    public TvmClient tvmClient() {
        return new FakeTvmClient();
    }
}
