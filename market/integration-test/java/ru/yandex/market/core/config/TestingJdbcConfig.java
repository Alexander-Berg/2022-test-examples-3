package ru.yandex.market.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import(DatasourceConfig.class)
@PropertySource({
        "classpath:common-servant-testing.properties",
        "classpath:/ru/yandex/market/core/config/testing-datasource.properties",
})
public class TestingJdbcConfig {
}
