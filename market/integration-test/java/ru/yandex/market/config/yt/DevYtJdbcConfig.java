package ru.yandex.market.config.yt;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import(DatasourceConfig.class)
@PropertySource("classpath:/ru/yandex/market/config/dev-datasource.properties")
public class DevYtJdbcConfig {
}
