package ru.yandex.market.logistics.tarifficator.client.configuration;

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Charsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClientImpl;
import ru.yandex.market.logistics.util.client.ClientUtilsFactory;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.HttpTemplateImpl;
import ru.yandex.market.logistics.util.client.TvmTicketProvider;

@Configuration
public class TestClientConfig {

    @Value("${tarifficator.api.host}")
    private String host;

    @Bean
    public HttpTemplate httpTemplate(RestTemplate clientRestTemplate) {
        return new HttpTemplateImpl(host, clientRestTemplate, MediaType.APPLICATION_JSON, tvmTicketProvider());
    }

    @Bean
    public RestTemplate clientRestTemplate(ObjectMapper objectMapper) {
        return new RestTemplateBuilder()
            .messageConverters(Arrays.asList(
                new StringHttpMessageConverter(Charsets.UTF_8),
                jsonConverter(objectMapper),
                new FormHttpMessageConverter()
            ))
            .build();
    }

    @Bean
    public TvmTicketProvider tvmTicketProvider() {
        return new TvmTicketProvider() {
            @Override
            public String provideServiceTicket() {
                return "test-service-ticket";
            }

            @Override
            public String provideUserTicket() {
                return "test-user-ticket";
            }
        };
    }

    @Bean
    public ObjectMapper objectMapper() {
        return ClientUtilsFactory.getObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Bean
    public TarifficatorClient tarifficatorClient(RestTemplate clientRestTemplate, ObjectMapper objectMapper) {
        return new TarifficatorClientImpl(httpTemplate(clientRestTemplate), objectMapper);
    }

    private MappingJackson2HttpMessageConverter jsonConverter(ObjectMapper objectMapper) {
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }
}
