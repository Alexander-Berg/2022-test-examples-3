package ru.yandex.market.hc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Created by aproskriakov on 11/2/21
 */
@Configuration
@Import({DatabaseConfig.class,
         EmbeddedPgConfig.class,})
public class DaoTestConfig {
}
