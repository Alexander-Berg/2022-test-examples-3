package ru.yandex.market.logistic.gateway.client.config;

import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistic.api.utils.UniqService;
import ru.yandex.market.logistic.gateway.client.LogisticApiRequestsClientFactory;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.request.trace.Module;

@Configuration
public class RestTemplateConfig {

    private static final int HASH_LENGTH = 32;

    @Value("${lgw.api.host}")
    private String lgwHost;

    @Bean
    @Qualifier("xmlHttpTemplate")
    public HttpTemplate xmlHttpTemplate(RestTemplate logisticApiRequestsClientRestTemplate) {
        return new HttpTemplateImpl(lgwHost, logisticApiRequestsClientRestTemplate, MediaType.TEXT_XML);
    }

    @Bean
    public RestTemplate logisticApiRequestsClientRestTemplate() {
        return new RestTemplateBuilder()
            .requestFactory(() -> new BufferingClientHttpRequestFactory(
                new TracingHttpRequestsFactory(Module.LOGISTIC_GATEWAY)))
            .messageConverters(Arrays.asList(
                xmlConverter(),
                new StringHttpMessageConverter(Charset.forName("UTF-8"))))
            .build();
    }

    @Bean
    public UniqService uniqService() {
        return () -> StringUtils.repeat("x", HASH_LENGTH);
    }

    private MappingJackson2HttpMessageConverter xmlConverter() {

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(
            LogisticApiRequestsClientFactory.createXmlMapper());

        converter.setSupportedMediaTypes(Arrays.asList(
            MediaType.TEXT_XML,
            MediaType.APPLICATION_XML)
        );

        return converter;
    }
}
