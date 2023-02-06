package ru.yandex.market.fulfillment.stockstorage.configuration;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import ru.yandex.market.fulfillment.stockstorage.domain.exception.json.ErrorCodeException;
import ru.yandex.market.fulfillment.stockstorage.domain.exception.json.ErrorCodeExceptionMixIn;

@Configuration
public class ExceptionsParserConfiguration extends WebMvcConfigurerAdapter {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new StringHttpMessageConverter());
        converters.add(apiJsonConverter());
    }

    @Bean
    protected MappingJackson2HttpMessageConverter apiJsonConverter() {
        return new MappingJackson2HttpMessageConverter(Jackson2ObjectMapperBuilder.json()
                .mixIn(ErrorCodeException.class, ErrorCodeExceptionMixIn.class)
                .build()
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY)
        );
    }
}
