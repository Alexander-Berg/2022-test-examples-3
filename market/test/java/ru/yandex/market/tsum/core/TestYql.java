package ru.yandex.market.tsum.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import ru.yandex.market.tsum.clients.yql.YqlApiClient;

@Configuration
@PropertySource("classpath:test.properties")
public class TestYql {

    @Bean
    public YqlApiClient yqlApiClient(@Value("${tsum.yql.base_uri}") String uri,
                                     @Value("${tsum.yql.token}") String token) {
        return new YqlApiClient(uri, token, null);
    }
}
