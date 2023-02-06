package ru.yandex.market.wms.navigator.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@TestConfiguration
public class TestYtConfig {

    @Bean("yqlDataSourceProperties")
    @ConfigurationProperties("spring.yql-datasource.connection")
    public DataSourceProperties yqlSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean("ytDataSource")
    public DataSource ytDataSource() {
        return yqlSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean("yqlTemplate")
    public NamedParameterJdbcTemplate yqlTemplate(@Qualifier("ytDataSource") DataSource ytDataSource) {
        return new NamedParameterJdbcTemplate(ytDataSource);
    }

    @Bean("ytTransactionManager")
    public DataSourceTransactionManager ytTransactionManager(@Qualifier("ytDataSource") DataSource ytDataSource) {
        return new DataSourceTransactionManager(ytDataSource);
    }
}
