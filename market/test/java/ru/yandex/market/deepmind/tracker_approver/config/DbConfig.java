package ru.yandex.market.deepmind.tracker_approver.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.liquibase.DataSourceClosingSpringLiquibase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbo.pgutils.intercept.DelegatingJdbcInterceptorSupplier;
import ru.yandex.market.mbo.pgutils.intercept.InterceptingDataSource;


@Configuration
@PropertySource("classpath:db-test.properties")
public class DbConfig {

    @Value("${sql.driverName}")
    private String jdbcDriver;
    @Value("${sql.url}")
    private String url;
    @Value("${sql.userName}")
    private String username;
    @Value("${sql.password}")
    private String password;

    @Value("${sql.liquibase.tables.schema}")
    private String liquibaseTablesSchema;
    @Value("${sql.liquibase.changelog}")
    private String liquibaseChangelog;
    @Value("${sql.liquibase.enabled}")
    private boolean liquibaseEnabled;

    @Bean
    public DataSourceClosingSpringLiquibase liquibase() {
        var liquibase = new DataSourceClosingSpringLiquibase();
        liquibase.setDataSource(dataSource());
        liquibase.setDefaultSchema(liquibaseTablesSchema);
        liquibase.setChangeLog("classpath:" + liquibaseChangelog);
        liquibase.setShouldRun(liquibaseEnabled);
        return liquibase;
    }

    @Bean
    public DelegatingJdbcInterceptorSupplier delegatingJdbcInterceptorSupplier() {
        return new DelegatingJdbcInterceptorSupplier();
    }

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, username, password);
        dataSource.setDriverClassName(jdbcDriver);
        return new InterceptingDataSource<>(dataSource, delegatingJdbcInterceptorSupplier());
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }

    @Bean
    public TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager());
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(jdbcTemplate());
    }
}
