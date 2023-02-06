package ru.yandex.market.hrms.core.config;

import java.io.IOException;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.postgresql.Driver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

@Configuration
public class TestDatabaseConfig {

    @Bean
    public SimpleDriverDataSource dataSource(
            @Value("${market-hrms.postgresql.url}") String url,
            @Value("${market-hrms.postgresql.username}") String username,
            @Value("${market-hrms.postgresql.password}") String password
    ) {
        SimpleDriverDataSource simpleDriverDataSource = new SimpleDriverDataSource();
        simpleDriverDataSource.setDriverClass(Driver.class);
        simpleDriverDataSource.setUrl(url);
        simpleDriverDataSource.setUsername(username);
        simpleDriverDataSource.setPassword(password);
        return simpleDriverDataSource;
    }

    @Bean
    public SpringLiquibase springLiquibase(DataSource dataSource) throws IOException {
        SpringLiquibase springLiquibase = new SpringLiquibase();
        springLiquibase.setChangeLog("classpath:changelog.xml");
        springLiquibase.setDataSource(dataSource);
        return springLiquibase;
    }

    @Bean
    public NamedParameterJdbcTemplate postgresJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
