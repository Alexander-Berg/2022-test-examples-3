package ru.yandex.market.logistics.management.client.async.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import ru.yandex.market.logistics.management.client.async.LmsLgwCallbackClient;
import ru.yandex.market.logistics.management.client.async.LmsLgwCallbackClientFactory;
import ru.yandex.market.logistics.management.client.async.LmsLgwCallbackRestClient;
import ru.yandex.market.logistics.util.client.ExternalServiceProperties;
import ru.yandex.market.logistics.util.client.HttpTemplate;

@Configuration
public class TestClientConfig {

    @Bean
    @ConfigurationProperties("lms")
    public ExternalServiceProperties testLmsProperties() {
        return new ExternalServiceProperties();
    }

    @Bean
    public LmsLgwCallbackClient lmsAsyncClient(HttpTemplate httpTemplate) {
        return new LmsLgwCallbackRestClient(httpTemplate);
    }

    @Bean
    public MappingJackson2HttpMessageConverter lmsJsonConverter() {
        return LmsLgwCallbackClientFactory.createLmsJsonConverter();
    }

    @Bean
    public HttpTemplate httpTemplate(
        ExternalServiceProperties testLmsProperties,
        MappingJackson2HttpMessageConverter lmsJsonConverter
    ) {
        return LmsLgwCallbackClientFactory.createHttpTemplate(
            testLmsProperties,
            lmsJsonConverter,
            clientId -> "test-ticket"
        );
    }
}
