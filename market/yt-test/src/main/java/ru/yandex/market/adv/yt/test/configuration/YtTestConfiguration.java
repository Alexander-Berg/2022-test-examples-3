package ru.yandex.market.adv.yt.test.configuration;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.adv.yt.test.jackson.deserializer.CustomBeanDeserializerModifier;

/**
 * Конфигурация для тестирования YT.
 * Date: 13.01.2022
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
@SuppressWarnings("SpringFacetCodeInspection")
@Configuration
@ParametersAreNonnullByDefault
public class YtTestConfiguration {

    @Bean
    public ObjectMapper ytObjectMapper(ObjectMapper objectMapper) {
        SimpleModule module = new SimpleModule();
        module.setDeserializerModifier(new CustomBeanDeserializerModifier());

        objectMapper.registerModule(module);
        return objectMapper;
    }
}
