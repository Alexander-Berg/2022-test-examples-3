package ru.yandex.market.api.partner.controllers;

import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import ru.yandex.market.api.partner.apisupport.ApiSupportConfig;
import ru.yandex.market.api.partner.view.json.Json2HttpMessageConverter;
import ru.yandex.market.api.partner.view.xml.Xml2HttpMessageConverter;
import ru.yandex.market.mbi.jaxb.jackson.ApiObjectMapperFactory;
import ru.yandex.market.mbi.jaxb.jackson.XmlNamingStrategy;

/**
 * @author berest
 */
@Configuration
@Import(ApiSupportConfig.class)
public class TestApiSupportConfig implements WebMvcConfigurer {
    @Bean
    public ObjectMapper xmlMapper() {
        return new ApiObjectMapperFactory().createXmlMapper(new XmlNamingStrategy());
    }

    @Bean
    public ObjectMapper jsonMapper() {
        return new ApiObjectMapperFactory().createJsonMapper();
    }


    @Nonnull
    @Bean
    public Xml2HttpMessageConverter xml2HttpMessageConverter() {
        return new Xml2HttpMessageConverter(xmlMapper());
    }

    @Nonnull
    @Bean
    public Json2HttpMessageConverter json2HttpMessageConverter() {
        return new Json2HttpMessageConverter(jsonMapper());
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(xml2HttpMessageConverter());
        converters.add(json2HttpMessageConverter());
    }
}
