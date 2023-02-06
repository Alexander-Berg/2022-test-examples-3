package ru.yandex.market.tpl.client;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.tpl.client.billing.BillingClient;
import ru.yandex.market.tpl.client.billing.BillingClientImpl;
import ru.yandex.market.tpl.client.dropoff.TplDropoffCargoClient;
import ru.yandex.market.tpl.client.dropoff.TplDropoffCargoClientImpl;
import ru.yandex.market.tpl.client.ff.FulfillmentResponseConsumerClient;
import ru.yandex.market.tpl.client.ff.FulfillmentResponseConsumerClientImpl;

/**
 * @author kukabara
 */
@Configuration
public class TplClientTestConfiguration {

    @Value("${tpl.int.url}")
    private String tplIntUrl;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public FulfillmentResponseConsumerClient tplFulfillmentClient(RestTemplate tplIntRestTemplate) {
        return new FulfillmentResponseConsumerClientImpl(tplIntUrl, tplIntRestTemplate);
    }

    @Bean
    public RestTvmClient restTvmClient(RestTemplate tplIntRestTemplate) {
        return new RestTvmClient(tplIntUrl, tplIntRestTemplate, Optional.empty());
    }

    @Bean
    public BillingClient tplBillingClient(RestTvmClient restTvmClient) {
        return new BillingClientImpl(restTvmClient);
    }

    @Bean
    public TplDropoffCargoClient tplDropoffCargoClient(RestTvmClient restTvmClient) {
        return new TplDropoffCargoClientImpl(restTvmClient);
    }

    @Bean
    public RestTemplate tplIntRestTemplate() {
        return new RestTemplateBuilder()
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create().build()))
                .messageConverters(Arrays.asList(
                        new MappingJackson2HttpMessageConverter(TplClientConfiguration.OBJECT_MAPPER),
                        new StringHttpMessageConverter(StandardCharsets.UTF_8))
                )
                .build();

    }
}
