package ru.yandex.market.logistic.api;

import java.nio.charset.Charset;
import java.util.Arrays;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistic.api.client.LogisticApiClientFactory;
import ru.yandex.market.logistic.api.utils.HttpTemplate;
import ru.yandex.market.request.trace.Module;

@Configuration
public class RestTemplateConfiguration {

    @Bean
    public HttpTemplate jsonHttpTemplate() {
        return new HttpTemplateImpl(getJsonRestTemplate(), MediaType.APPLICATION_JSON);
    }

    @Bean
    public HttpTemplate xmlHttpTemplate() {
        return new HttpTemplateImpl(getXmlRestTemplate(xmlConverter()), MediaType.TEXT_XML);
    }

    @Bean
    public RestTemplate getJsonRestTemplate() {

        return new RestTemplateBuilder()
            .requestFactory(this::getRequestFactory)
            .messageConverters(Arrays.asList(
                jsonConverter(),
                new StringHttpMessageConverter(Charset.forName("UTF-8"))))
            .build();
    }

    @Bean
    public RestTemplate getXmlRestTemplate(MappingJackson2HttpMessageConverter converter) {

        return new RestTemplateBuilder()
            .requestFactory(this::getRequestFactory)
            .messageConverters(Arrays.asList(
                jsonConverter(),
                converter,
                new StringHttpMessageConverter(Charset.forName("UTF-8"))))
            .build();
    }

    @Bean
    public BufferingClientHttpRequestFactory getRequestFactory() {
        return new BufferingClientHttpRequestFactory(new TracingHttpRequestsFactory(Module.DELIVERY_DSM));
    }

    private MappingJackson2HttpMessageConverter jsonConverter() {
        return new MappingJackson2HttpMessageConverter(LogisticApiClientFactory.createJsonMapper());
    }

    @Bean
    public MappingJackson2HttpMessageConverter xmlConverter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(
            LogisticApiClientFactory.createXmlMapper()
        );

        converter.setSupportedMediaTypes(Arrays.asList(
            MediaType.TEXT_XML,
            MediaType.APPLICATION_XML)
        );

        return converter;
    }
}
