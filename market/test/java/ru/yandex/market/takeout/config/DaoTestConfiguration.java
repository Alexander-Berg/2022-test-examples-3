package ru.yandex.market.takeout.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        EmbeddedPgConfig.class,
        DaoTestConfig.class
})
public class DaoTestConfiguration {
}
