package ru.yandex.market.pharmatestshop.config;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

@Configuration
@Import(PharmaDatasourceConfiguration.class)
public class PharmaLiquibaseConfiguration {

    @Autowired
    Environment environment;

    @Bean
    SpringLiquibase liquibase(@Value("${postgresql.schema}") String schema,
                              DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setChangeLog("classpath:changelog.xml");
        liquibase.setDataSource(dataSource);
        liquibase.setContexts(String.join(",", environment.getActiveProfiles()));
        if (dataSource instanceof HikariDataSource) {
            liquibase.setDefaultSchema(((HikariDataSource) dataSource).getSchema());
        } else {
            liquibase.setDefaultSchema(schema);
        }
        return liquibase;
    }

}

