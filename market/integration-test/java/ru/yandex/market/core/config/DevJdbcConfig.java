package ru.yandex.market.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.core.database.MdbMbiConfig;

@Configuration
@Import({
        DatasourceConfig.class,
        MdbMbiConfig.class
})
@PropertySource({
        "classpath:common-servant-development.properties",
        "classpath:/ru/yandex/market/core/config/dev-datasource.properties",
})
public class DevJdbcConfig {
}
