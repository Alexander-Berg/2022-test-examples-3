package ru.yandex.market.rg.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.delivery.dsmclient.util.HttpTemplate;
import ru.yandex.market.rg.config.dsm.DsmHttpRetryableTemplate;

@Configuration
public class TestDsmClientConfig {

    @Bean
    public HttpTemplate testDsmHttpRetryableTemplate() {
        return new DsmHttpRetryableTemplate("http://host", testDsmClientRestTemplate(), MediaType.APPLICATION_JSON);
    }

    @Bean
    public RestTemplate testDsmClientRestTemplate() {
        return Mockito.mock(RestTemplate.class);
    }
}
