package ru.yandex.market.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import(DatasourceConfig.class)
@PropertySource({
        "classpath:common-servant-production.properties",
        "classpath:/ru/yandex/market/core/config/production-datasource.properties",
})
public class ProductionJdbcConfig {
}
