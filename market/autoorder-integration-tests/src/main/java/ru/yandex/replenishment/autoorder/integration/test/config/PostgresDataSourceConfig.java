package ru.yandex.replenishment.autoorder.integration.test.config;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import ru.yandex.market.mbo.pgutils.intercept.DelegatingJdbcInterceptorSupplier;
import ru.yandex.market.mbo.pgutils.intercept.InterceptingDataSource;
import ru.yandex.market.request.datasource.trace.DataSourceTraceUtil;
import ru.yandex.market.request.trace.Module;

@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
@Configuration
@EnableTransactionManagement
public class PostgresDataSourceConfig {

    @Primary
    @Bean(name = "dataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = "dataSource")
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public DataSource dataSource(DelegatingJdbcInterceptorSupplier delegatingJdbcInterceptorSupplier) {
        final HikariDataSource datasource = dataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        datasource.setRegisterMbeans(true);
        return DataSourceTraceUtil.wrap(
                new InterceptingDataSource<>(datasource, delegatingJdbcInterceptorSupplier), Module.PGAAS);
    }

    @Primary
    @Bean(name = "jdbcTemplate")
    public JdbcTemplate jdbcTemplate(@Qualifier("dataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Primary
    @Bean(name = "namedParameterJdbcTemplate")
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Bean
    public DelegatingJdbcInterceptorSupplier delegatingJdbcInterceptorSupplier() {
        return new DelegatingJdbcInterceptorSupplier();
    }
}
