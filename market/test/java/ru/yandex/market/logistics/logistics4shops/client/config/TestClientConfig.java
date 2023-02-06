package ru.yandex.market.logistics.logistics4shops.client.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.logistics4shops.client.utils.RestTemplateErrorHandler;
import ru.yandex.market.logistics.util.client.ExternalServiceProperties;
import ru.yandex.market.logistics.util.client.SpringClientUtilsFactory;
import ru.yandex.market.logistics4shops.client.ApiClient;
import ru.yandex.market.logistics4shops.client.api.OrderBoxApi;
import ru.yandex.market.request.trace.Module;

@Configuration
@EnableConfigurationProperties
public class TestClientConfig {

    @Bean
    @ConfigurationProperties("l4s")
    public ExternalServiceProperties clientProperties() {
        return new ExternalServiceProperties();
    }

    @Bean
    public RestTemplate clientRestTemplate(ExternalServiceProperties clientProperties) {
        return SpringClientUtilsFactory.createRestTemplate(
            clientProperties,
            Module.MARKET_LOGISTICS4SHOPS,
            List.of(),
            new RestTemplateErrorHandler(),
            null
        );
    }

    @Bean
    public ApiClient apiClient(
        RestTemplate clientRestTemplate,
        ExternalServiceProperties clientProperties
    ) {
        return new ApiClient(clientRestTemplate)
            .setBasePath(clientProperties.getUrl());
    }

    @Bean
    public OrderBoxApi orderBoxApi(ApiClient apiClient) {
        return new OrderBoxApi(apiClient);
    }
}
