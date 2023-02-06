package ru.yandex.market.mbi.bot;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
@Profile("integrationTest")
public class IntegrationTestConfig {

    private static final int READ_TIMEOUT = 20000;
    private static final int CONNECT_TIMEOUT = 3000;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public ClientHttpRequestFactory partnerBotHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setReadTimeout(READ_TIMEOUT);
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        return requestFactory;
    }

    @Bean
    public ObjectMapper partnerBotObjectMapper() {
        return new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Bean
    public RestTemplate partnerBotRestTemplate(
            ObjectMapper partnerBotObjectMapper,
            ClientHttpRequestFactory partnerBotHttpRequestFactory
    ) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(List.of(
                new StringHttpMessageConverter(StandardCharsets.UTF_8),
                new MappingJackson2HttpMessageConverter(partnerBotObjectMapper)
        ));
        restTemplate.setRequestFactory(partnerBotHttpRequestFactory);
        return restTemplate;
    }

    @Bean
    public IntegrationTestClient integrationTestClient(
            RestTemplate partnerBotRestTemplate,
            @Value("${mbi-partner-bot.telegram.url}") String botUrl
    ) {
        return new IntegrationTestClient(partnerBotRestTemplate, botUrl);
    }
}
