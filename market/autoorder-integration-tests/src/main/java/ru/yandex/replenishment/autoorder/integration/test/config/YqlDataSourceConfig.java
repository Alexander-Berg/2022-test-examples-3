package ru.yandex.replenishment.autoorder.integration.test.config;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.request.datasource.trace.DataSourceTraceUtil;
import ru.yandex.market.request.trace.Module;
import ru.yandex.yql.YqlDataSource;
import ru.yandex.yql.settings.YqlProperties;

@Configuration
@ParametersAreNonnullByDefault
@EnableConfigurationProperties(YqlDataSourcePropertiesConfig.class)
public class YqlDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(YqlDataSourceConfig.class);

    private final YqlDataSourcePropertiesConfig yqlDataSourcePropertiesConfig;

    public YqlDataSourceConfig(YqlDataSourcePropertiesConfig yqlDataSourcePropertiesConfig) {
        this.yqlDataSourcePropertiesConfig = yqlDataSourcePropertiesConfig;
    }

    @Bean(name = "yqlJdbcTemplate")
    public JdbcTemplate yqlJdbcTemplate(@Value("${yql.socket.timeout:30000}") int socketTimeout) {
        log.debug("YQL URL to connect to: " + yqlDataSourcePropertiesConfig.getUrl());
        // Логировать токен за пределами юнит-тестов бессмысленно и опасно
        return new JdbcTemplate(yqlDataSource(socketTimeout));
    }

    // Аннотацию @Bean не повесил, так как в этом случак она начинается обрабатывать всякими hibernate jpa
    // постпроцессорами, а оно нам не надо
    @Bean(name = "yqlDataSource")
    public DataSource yqlDataSource(@Value("${yql.socket.timeout:30000}") int socketTimeout) {
        final YqlProperties yqlProperties = new YqlProperties();
        yqlProperties.setPassword(yqlDataSourcePropertiesConfig.getToken());
        yqlProperties.setSyntaxVersion(1);
        yqlProperties.setSocketTimeout(socketTimeout);
        final YqlDataSource yqlDataSource = new YqlDataSource(yqlDataSourcePropertiesConfig.getUrl(), yqlProperties);
        return DataSourceTraceUtil.wrap(yqlDataSource, Module.YT);
    }

}
