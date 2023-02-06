package ru.yandex.market.logistics.dbqueue.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObjectMapperConfig {

    @Bean
    ObjectMapper getMapper() {
        return new ObjectMapper();
    }
}
