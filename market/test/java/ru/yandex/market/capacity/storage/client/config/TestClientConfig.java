package ru.yandex.market.capacity.storage.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import ru.yandex.market.capacity.storage.client.CapacityStorageClient;
import ru.yandex.market.capacity.storage.client.CapacityStorageClientFactory;
import ru.yandex.market.capacity.storage.client.CapacityStorageHttpClient;
import ru.yandex.market.logistics.util.client.ExternalServiceProperties;
import ru.yandex.market.logistics.util.client.HttpTemplate;

@Configuration
public class TestClientConfig {

    @Bean
    @ConfigurationProperties("cs")
    public ExternalServiceProperties testCsProperties() {
        return new ExternalServiceProperties();
    }

    @Bean
    public CapacityStorageClient csClient(
        ExternalServiceProperties testCsProperties,
        MappingJackson2HttpMessageConverter csJsonConverter
    ) {
        HttpTemplate httpTemplate = httpTemplate(testCsProperties, csJsonConverter);
        return new CapacityStorageHttpClient(httpTemplate, csJsonConverter.getObjectMapper());
    }

    @Bean
    public MappingJackson2HttpMessageConverter csJsonConverter() {
        return CapacityStorageClientFactory.createCsJsonConverter();
    }

    @Bean
    public HttpTemplate httpTemplate(
        ExternalServiceProperties testCsProperties,
        MappingJackson2HttpMessageConverter csJsonConverter
    ) {
        return CapacityStorageClientFactory.createHttpTemplate(
            testCsProperties,
            csJsonConverter,
            clientId -> "test-ticket"
        );
    }

}
