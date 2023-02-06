package ru.yandex.market.antifraud.orders.external.volva;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.AsyncRestTemplate;

import ru.yandex.market.volva.serializer.VolvaJsonUtils;

@Configuration
public class VolvaTestConfig {

    @Bean
    public AsyncRestTemplate volvaRestTemplate() {
        var restTemplate = new AsyncRestTemplate();
        restTemplate.setMessageConverters(List.of(new MappingJackson2HttpMessageConverter(VolvaJsonUtils.OBJECT_MAPPER)));
        return restTemplate;
    }

    @Bean
    public HttpVolvaClient volvaHttpClient(AsyncRestTemplate volvaRestTemplate) {
        return new HttpVolvaClient(volvaRestTemplate);
    }
}
