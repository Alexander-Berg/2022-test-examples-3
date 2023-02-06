package ru.yandex.market.logistics.management.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.client.LMSClientFactory;
import ru.yandex.market.logistics.management.client.LmsHttpClient;
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
    public LMSClient lmsClient(
        ExternalServiceProperties testLmsProperties,
        MappingJackson2HttpMessageConverter lmsJsonConverter
    ) {
        HttpTemplate httpTemplate = httpTemplate(testLmsProperties, lmsJsonConverter);
        return new LmsHttpClient(httpTemplate, lmsJsonConverter.getObjectMapper());
    }

    @Bean
    public MappingJackson2HttpMessageConverter lmsJsonConverter() {
        return LMSClientFactory.createLmsJsonConverter();
    }

    @Bean
    public HttpTemplate httpTemplate(
        ExternalServiceProperties testLmsProperties,
        MappingJackson2HttpMessageConverter lmsJsonConverter
    ) {
        return LMSClientFactory.createHttpTemplate(testLmsProperties, lmsJsonConverter, clientId -> "test-ticket");
    }
}
