package ru.yandex.market.common.test.spring;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Фабрика для создания экземпляров RestTemplate. Все работают с UTF-8.
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
public class RestTemplateFactory {

    private RestTemplateFactory() {
        throw new UnsupportedOperationException("Can not instantiate util class");
    }

    /**
     * Создать экземпляр restTemplate, который работает с UTF-8.
     */
    public static RestTemplate createRestTemplate() {
        // Берем все дефолтные конверторы и заменяем экземпляр StringHttpMessageConverter на UTF-8-совместимый
        // На основе этих конверторов и создаем restTemplate
        RestTemplate standardRestTemplate = new RestTemplate();
        List<HttpMessageConverter<?>> converters = standardRestTemplate.getMessageConverters().stream()
                .map(RestTemplateFactory::makeUTF8Compatible)
                .collect(Collectors.toList());
        return new RestTemplate(converters);
    }

    private static HttpMessageConverter<?> makeUTF8Compatible(HttpMessageConverter<?> converter)  {
        if (converter.getClass().equals(StringHttpMessageConverter.class)) {
            return new StringHttpMessageConverter(StandardCharsets.UTF_8);
        }
        return converter;
    }

    /**
     * Создать экземпляр restTemplate, который работает с json.
     */
    public static RestTemplate createJsonRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(
                Arrays.asList(
                        new ByteArrayHttpMessageConverter(),
                        new StringHttpMessageConverter(StandardCharsets.UTF_8),
                        createJacksonConverter(),
                        new FormHttpMessageConverter()
                )
        );
        return restTemplate;
    }

    private static MappingJackson2HttpMessageConverter createJacksonConverter() {
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
        jacksonConverter.setObjectMapper(createObjectMapper());
        return jacksonConverter;
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()));
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }
}
