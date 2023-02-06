package ru.yandex.market.tpl.internal.client.configuration;

import java.nio.charset.StandardCharsets;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.util.client.ClientUtilsFactory;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.HttpTemplateImpl;
import ru.yandex.market.logistics.util.client.TvmTicketProvider;
import ru.yandex.market.tpl.internal.client.TplInternalClient;
import ru.yandex.market.tpl.internal.client.TplInternalClientImpl;

@Configuration
public class TestConfiguration {

    @Bean
    public HttpTemplate httpTemplate(
        @Value("${tpl-internal.api.url}") String host,
        RestTemplate restTemplate,
        TvmTicketProvider tvmTicketProvider
    ) {
        return new HttpTemplateImpl(host, restTemplate, tvmTicketProvider);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder()
            .messageConverters(ImmutableList.of(
                new MappingJackson2HttpMessageConverter(ClientUtilsFactory.getObjectMapper()),
                new StringHttpMessageConverter(StandardCharsets.UTF_8)
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
    public TplInternalClient tplInternalClient(HttpTemplate httpTemplate) {
        return new TplInternalClientImpl(httpTemplate);
    }

}
