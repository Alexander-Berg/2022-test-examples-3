package ru.yandex.market.logistics.util.client.tvm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.util.client.tvm.client.DetailedTvmClient;
import ru.yandex.market.logistics.util.client.tvm.client.DetailedTvmClientImpl;

@Configuration
public class TestClientConfiguration {

    @Bean
    public RestTemplate clientRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public DetailedTvmClient detailedTvmClient(RestTemplate restTemplate, @Value("${tvm.detailed.url}") String url) {
        return new DetailedTvmClientImpl(restTemplate, url);
    }

}

