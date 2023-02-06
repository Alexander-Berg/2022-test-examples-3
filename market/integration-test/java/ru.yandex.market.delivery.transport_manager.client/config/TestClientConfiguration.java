package ru.yandex.market.delivery.transport_manager.client.config;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.delivery.transport_manager.client.TransportManagerClientImpl;
import ru.yandex.market.logistics.util.client.ClientUtilsFactory;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.HttpTemplateImpl;
import ru.yandex.market.logistics.util.client.TvmTicketProvider;

@Configuration
@EnableConfigurationProperties
public class TestClientConfiguration {
    @Bean
    public TmApiProperties tmApiProperties() {
        return new TmApiProperties();
    }

    @Bean
    public RestTemplate clientRestTemplate(ObjectMapper objectMapper) {
        return new RestTemplateBuilder()
            .messageConverters(Arrays.asList(
                new StringHttpMessageConverter(StandardCharsets.UTF_8),
                jsonConverter(objectMapper)
                )
            )
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
    public HttpTemplate httpTemplate(
        RestTemplate clientRestTemplate,
        TvmTicketProvider tvmTicketProvider,
        TmApiProperties tmApiProperties
    ) {
        return new HttpTemplateImpl(
            tmApiProperties.getUrl(),
            clientRestTemplate,
            MediaType.APPLICATION_JSON,
            tvmTicketProvider
        );
    }

    @Bean
    public TransportManagerClient transportManagerClient(HttpTemplate httpTemplate, ObjectMapper objectMapper) {
        return new TransportManagerClientImpl(httpTemplate, objectMapper);
    }

    private MappingJackson2HttpMessageConverter jsonConverter(ObjectMapper objectMapper) {
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }
}
