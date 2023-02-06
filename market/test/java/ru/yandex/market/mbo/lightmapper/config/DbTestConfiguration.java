package ru.yandex.market.mbo.lightmapper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.postgres.spring.configs.PGDatabaseConfig;
import ru.yandex.market.mboc.common.infrastructure.sql.SavePointTransactionHelper;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;

@Import(PGDatabaseConfig.class)
@Configuration
public class DbTestConfiguration {
    private final PGDatabaseConfig config;

    public DbTestConfiguration(PGDatabaseConfig config) {
        this.config = config;
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(config.dataSource());
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(config.dataSource());
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(jdbcTemplate());
    }

    @Bean
    public TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager());
    }

    @Bean
    public TransactionHelper transactionHelper() {
        return new SavePointTransactionHelper(transactionTemplate());
    }
}
