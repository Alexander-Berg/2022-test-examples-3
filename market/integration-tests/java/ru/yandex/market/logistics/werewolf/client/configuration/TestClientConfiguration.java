package ru.yandex.market.logistics.werewolf.client.configuration;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.util.client.ClientUtilsFactory;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.HttpTemplateImpl;
import ru.yandex.market.logistics.util.client.TvmTicketProvider;
import ru.yandex.market.logistics.util.client.converter.InputStreamMessageConverter;
import ru.yandex.market.logistics.werewolf.client.WwClient;
import ru.yandex.market.logistics.werewolf.client.WwClientImpl;

@Configuration
public class TestClientConfiguration {

    @Value("${ww.api.url}")
    private String host;

    @Bean
    public HttpTemplate httpTemplate(
        RestTemplate clientRestTemplate,
        TvmTicketProvider tvmTicketProvider
    ) {
        return new HttpTemplateImpl(host, clientRestTemplate, tvmTicketProvider);
    }

    @Bean
    public RestTemplate clientRestTemplate(ObjectMapper objectMapper) {
        return new RestTemplateBuilder()
            .messageConverters(
                new StringHttpMessageConverter(StandardCharsets.UTF_8),
                new InputStreamMessageConverter(MediaType.TEXT_HTML),
                new ByteArrayHttpMessageConverter(),
                new MappingJackson2HttpMessageConverter(objectMapper)
            )
            .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return ClientUtilsFactory.getObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
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
    public WwClient wwClient(HttpTemplate httpTemplate) {
        return new WwClientImpl(httpTemplate);
    }
}
