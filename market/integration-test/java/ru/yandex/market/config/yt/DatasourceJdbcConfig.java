package ru.yandex.market.config.yt;

import javax.sql.DataSource;

import oracle.jdbc.driver.OracleDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.db.NamedDataSource;

@Configuration
public class DatasourceJdbcConfig {
    @Value("${datasource.url}")
    private String datasourceUrl;

    @Value("${datasource.user}")
    private String user;

    @Value("${datasource.password}")
    private String password;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        final PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setIgnoreUnresolvablePlaceholders(true);
        return configurer;
    }

    @Bean
    public DataSource dataSource() {
        NamedDataSource dataSource = new NamedDataSource();
        dataSource.setDriver(new OracleDriver());
        dataSource.setUrl(datasourceUrl);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        dataSource.setModuleName("mbi-core");
        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }


    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(jdbcTemplate());
    }

    @Bean
    public TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(new DataSourceTransactionManager(dataSource()));
    }
}
