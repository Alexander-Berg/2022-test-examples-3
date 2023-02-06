package ru.yandex.market.core.database;

import java.sql.SQLException;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.core.database.OracleToPostgresJdbcTemplate.ExpQueryRouting;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
public class JdbcBoilerplateConfig {
    private static final boolean EXP_DATASOURCE_READ_ONLY = false;
    private static final Supplier<ExpQueryRouting> EXP_QUERY_ROUTING = () -> ExpQueryRouting.ENABLED;

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager txManager) {
        return new TransactionTemplate(txManager);
    }

    @Bean
    @Primary
    public PlatformTransactionManager txManager(DataSource dataSource) {
        return new OracleToPostgresDataSourceTransactionManager(
                faultyDataSource(),
                dataSource,
                EXP_DATASOURCE_READ_ONLY,
                EXP_QUERY_ROUTING
        );
    }

    @Bean
    @Primary
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(JdbcTemplate jdbcTemplate) {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource dataSource) throws SQLException {
        return new OracleToPostgresJdbcTemplate(
                faultyDataSource(),
                dataSource,
                EXP_DATASOURCE_READ_ONLY,
                EXP_QUERY_ROUTING,
                null
        );
    }

    @Bean
    DataSource faultyDataSource() {
        var dataSource = mock(DataSource.class, "faultyDataSource");
        try {
            when(dataSource.getConnection()).thenThrow(new CannotGetJdbcConnectionException("should not get here"));
        } catch (SQLException ignored) {
        }
        return dataSource;
    }

}
