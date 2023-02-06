package ru.yandex.market.fulfillment.stockstorage.configuration;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MockMvcConfiguration {
    @Bean
    public MockMvcBuilderCustomizer defaultResponseCharsetCustomizer() {
        return (builder) -> builder.defaultResponseCharacterEncoding(StandardCharsets.UTF_8);
    }
}
