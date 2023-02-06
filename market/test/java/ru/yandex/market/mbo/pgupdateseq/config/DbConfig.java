package ru.yandex.market.mbo.pgupdateseq.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbo.pgutils.intercept.DelegatingJdbcInterceptorSupplier;
import ru.yandex.market.mbo.pgutils.intercept.InterceptingDataSource;

/**
 * @author yuramalinov
 * @created 02.08.2019
 */
@Configuration
public class DbConfig {
    @Value("${sql.url}")
    private String url;

    @Value("${sql.userName}")
    private String username;

    @Value("${sql.password}")
    private String password;

    @Bean
    public DelegatingJdbcInterceptorSupplier delegatingJdbcInterceptorSupplier() {
        return new DelegatingJdbcInterceptorSupplier();
    }

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, username, password);
        dataSource.setDriverClassName("org.postgresql.Driver");
        return dataSource;
    }

    @Bean
    public DataSource interceptingDataSource() {
        return new InterceptingDataSource<>(dataSource(), delegatingJdbcInterceptorSupplier());
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
    public JdbcTemplate interceptingJdbcTemplate() {
        return new JdbcTemplate(interceptingDataSource());
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(jdbcTemplate());
    }
}
