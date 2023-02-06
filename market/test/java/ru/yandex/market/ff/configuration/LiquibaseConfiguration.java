package ru.yandex.market.ff.configuration;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author avetokhin 26/09/17.
 */
@Configuration
public class LiquibaseConfiguration {
    @Bean
    public SpringLiquibase springLiquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:changelog.xml");

        return liquibase;
    }

}
