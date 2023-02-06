package ru.yandex.market.wrap.infor.configuration;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;

import static ru.yandex.market.wrap.infor.configuration.DataSourcesConfiguration.WRAP_DATASOURCE;
import static ru.yandex.market.wrap.infor.configuration.IntegrationTestDataSourcesConfiguration.DATABASE_POPULATOR;

@Configuration
public class IntegrationTestLiquibaseConfiguration {

    public static final String LIQUIBASE_BEAN = "liquibase";

    @Bean(name = LIQUIBASE_BEAN)
    @DependsOn(DATABASE_POPULATOR)
    public SpringLiquibase liquibase(@Qualifier(WRAP_DATASOURCE) DataSource dataSource,
                                     LiquibaseProperties liquibaseProperties) {
        return LiquibaseConfiguration.springLiquibase(dataSource, liquibaseProperties);
    }

    @Bean
    @ConfigurationProperties(prefix = "liquibase", ignoreUnknownFields = false)
    protected LiquibaseProperties liquibaseProperties() {
        return new LiquibaseProperties();
    }
}
