package ru.yandex.market.vendors.analytics.core;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiquibaseConfig {

    @Autowired
    private DataSource postgresDataSource;

    @Value("${liquibase.changelog}")
    private String defaultLiquibaseChangelog;

    @Bean
    public SpringLiquibase liquibase() {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(postgresDataSource);
        liquibase.setChangeLog(defaultLiquibaseChangelog);
        return liquibase;
    }
}
