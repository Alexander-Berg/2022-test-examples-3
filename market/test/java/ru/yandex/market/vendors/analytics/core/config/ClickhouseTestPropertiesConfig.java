package ru.yandex.market.vendors.analytics.core.config;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import ru.yandex.clickhouse.settings.ClickHouseProperties;
import ru.yandex.market.vendors.analytics.core.datasource.clickhouse.ClickhousePropertiesConfig;

/**
 * @author antipov93.
 */
@Configuration
public class ClickhouseTestPropertiesConfig implements ClickhousePropertiesConfig {

    @Value("${analytics.clickhouse.database}")
    private String database;

    @Value("${analytics.clickhouse.socketTimeout:30000}")
    private int socketTimeout;

    public String getUrl() {
        String port = Optional.ofNullable(System.getenv("RECIPE_CLICKHOUSE_HTTP_PORT")).orElse("8123");
        return "jdbc:clickhouse://localhost:" + port;
    }

    public ClickHouseProperties getClickHouseProperties() {
        ClickHouseProperties properties = new ClickHouseProperties();
        properties.setDatabase(database);
        properties.setSocketTimeout(socketTimeout);
        return properties;
    }
}
