package ru.yandex.market.fintech.banksint.config;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"functionalTest"})
public class LiquibaseConfig {

    @Bean
    public SpringLiquibase liquibaseLocal(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setDefaultSchema("public");
        liquibase.setChangeLog("classpath:changelog.xml");
        return liquibase;
    }

}
