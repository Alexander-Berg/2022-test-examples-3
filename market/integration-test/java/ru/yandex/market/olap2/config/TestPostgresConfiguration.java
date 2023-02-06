package ru.yandex.market.olap2.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import ru.yandex.market.olap2.dao.LoggingJdbcTemplate;

/**
 * Конфигурация бинов для самого приложения Коко, эквивалент основного {@link PostgresConfig}.
 * {@link EmbeddedPostgresConfiguration} - это другое.
 */
@Configuration
public class TestPostgresConfiguration {

    @Bean
    public NamedParameterJdbcTemplate postgresJdbcTemplate(@Qualifier("metadataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean(name = "metadataTransactionManager")
    @Autowired
    public DataSourceTransactionManager metadataTransactionManager(@Qualifier("metadataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "metadataLoggingJdbcTemplate")
    @Autowired
    public LoggingJdbcTemplate metadataLoggingJdbcTemplate(@Qualifier("metadataSource") DataSource metadataDataSource) {
        return new LoggingJdbcTemplate(new NamedParameterJdbcTemplate(metadataDataSource), "integration-test");
    }
}
