package ru.yandex.market.ff4shops.config;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author fbokovikov
 */
@Configuration
public class LiquibaseConfig {

    @Bean(name = "liquibase")
    public SpringLiquibase liquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:changelog.xml");
        liquibase.setDropFirst(true);

        return liquibase;
    }
}
