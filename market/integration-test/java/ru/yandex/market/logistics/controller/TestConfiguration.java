package ru.yandex.market.logistics.controller;

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import ru.yandex.market.logistics.configuration.PageMatchConfig;

@EnableWebMvc
@Configuration
@Import({
    MockMvcAutoConfiguration.class,
    PageMatchConfig.class,
})
public class TestConfiguration {

    @Bean
    public SomeTestController someTestController() {
        return new SomeTestController();
    }
}
