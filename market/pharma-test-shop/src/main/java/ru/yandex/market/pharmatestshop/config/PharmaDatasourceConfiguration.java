package ru.yandex.market.pharmatestshop.config;

import java.util.Arrays;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.request.trace.Module;

import ru.yandex.market.request.datasource.trace.DataSourceTraceUtil;
import ru.yandex.market.tms.quartz2.spring.config.TmsDataSourceConfig;

@Configuration
public class PharmaDatasourceConfiguration {

    @Value("${postgresql.url}")
    private String url;

    @Value("${postgresql.username}")
    private String username;

    @Value("${postgresql.password}")
    private String password;

    @Value("${postgresql.driver:org.postgresql.Driver}")
    private String driver;

    @Value("${postgresql.max.pool.size:10}")
    private int maxPoolSize;

    @Value("${postgresql.properties}")
    private String properties;

    @Value("${postgresql.schema}")
    private String schema;

    @Bean
    public NamedParameterJdbcTemplate postgresJdbcTemplate() {
        return new NamedParameterJdbcTemplate(dataSource());
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setSchema(schema);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driver);
        config.setMaximumPoolSize(maxPoolSize);

        config.setConnectionTestQuery("SELECT 1");
        config.setConnectionInitSql("CREATE SCHEMA IF NOT EXISTS " + schema);

        Arrays.stream(properties.split("&"))
                .map(p -> p.split("="))
                .forEach(arr -> config.addDataSourceProperty(arr[0], arr[1]));

        HikariDataSource hikariDataSource = new HikariDataSource(config);
        return DataSourceTraceUtil.wrap(hikariDataSource, Module.PGAAS);
    }

    @Bean
    public TmsDataSourceConfig getTmsDataSourceConfig(DataSource dataSource) {

        return new TmsDataSourceConfig() {

            @Override
            public DataSource tmsDataSource() {
                return dataSource;
            }

            @Override
            public JdbcTemplate tmsJdbcTemplate() {
                return new JdbcTemplate(tmsDataSource());
            }

            @Override
            public TransactionTemplate tmsTransactionTemplate() {
                return new TransactionTemplate(tmsTransactionManager());
            }

            @Override
            public PlatformTransactionManager tmsTransactionManager() {
                return new DataSourceTransactionManager(tmsDataSource());
            }

        };
    }

}
