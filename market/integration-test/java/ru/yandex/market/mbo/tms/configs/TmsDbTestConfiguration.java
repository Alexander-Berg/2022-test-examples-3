package ru.yandex.market.mbo.tms.configs;

import java.util.Properties;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.market.mbo.utils.DbTestConfiguration;

/**
 * @author galaev@yandex-team.ru
 * @since 06/12/2018.
 */
@Configuration
@Import(DbTestConfiguration.class)
public class TmsDbTestConfiguration {

    @Bean
    PropertyPlaceholderConfigurer placeholderConfigurer(ApplicationContext applicationContext) {
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setLocations(
            new ClassPathResource("/db-test.properties")
        );
        // Hack required for Spring3 tests to work with embedded Postgresql
        Properties properties = new Properties();
        properties.put("mbo.pg.url",
            applicationContext.getEnvironment().getProperty("mbo.pg.url"));
        properties.put("mbo.pg.userName",
            applicationContext.getEnvironment().getProperty("mbo.pg.userName"));
        properties.put("mbo.pg.password",
            applicationContext.getEnvironment().getProperty("mbo.pg.password"));
        properties.put("liquibase.tables.schema",
            applicationContext.getEnvironment().getProperty("liquibase.tables.schema"));
        properties.put("liquibase.context",
            applicationContext.getEnvironment().getProperty("liquibase.context"));
        properties.put("mbo.pg.pgRootCert", applicationContext.getEnvironment().getProperty("mbo.pg.pgRootCert"));
        configurer.setProperties(properties);
        return configurer;
    }
}
