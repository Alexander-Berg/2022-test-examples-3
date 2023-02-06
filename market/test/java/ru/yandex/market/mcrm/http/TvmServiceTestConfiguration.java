package ru.yandex.market.mcrm.http;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.mcrm.http.internal.TvmServiceMockImpl;
import ru.yandex.market.mcrm.http.tvm.TvmService;

@Configuration
public class TvmServiceTestConfiguration {

    @Bean
    public TvmService tvmService() {
        return new TvmServiceMockImpl();
    }
}
