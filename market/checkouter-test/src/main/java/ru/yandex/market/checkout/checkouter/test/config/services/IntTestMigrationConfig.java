package ru.yandex.market.checkout.checkouter.test.config.services;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IntTestMigrationConfig {
    @Bean
    public SpringLiquibase liquibaseErpTdb(
            DataSource erpDataSource,
            @Value("classpath:changelog/create-erp-db.sql")
            String changeLog
    ) {
        SpringLiquibase springLiquibase = new SpringLiquibase();
        springLiquibase.setChangeLog(changeLog);
        springLiquibase.setDataSource(erpDataSource);
        return springLiquibase;
    }
}
