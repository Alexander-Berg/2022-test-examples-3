package ru.yandex.market.deepdive;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import ru.yandex.market.deepdive.configuration.DeepDiveExternalConfiguration;

@Configuration
@ComponentScan("ru.yandex.market.deepdive.domain")
@Import({
        DeepDiveExternalConfiguration.class,
})
@EnableWebMvc
public class TestConfiguration {
}
