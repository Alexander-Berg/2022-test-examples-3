package ru.yandex.market.deepdive.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@ComponentScan("ru.yandex.market.deepdive.domain")
@Import({
        DeepDiveExternalConfiguration.class,
})
@EnableWebMvc
public class TestConfig {
}
