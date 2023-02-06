package ru.yandex.market.pers.pay;

import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.pers.test.db.EmbeddedPostgreFactory;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 05.03.2021
 */
@Configuration
public class TestDbConfig {
    @Bean(destroyMethod = "close")
    public Object embeddedPostgres() {
        return EmbeddedPostgreFactory.embeddedPostgres(x -> x);
    }

    @Bean
    public DataSource embeddedDatasource() {
        return EmbeddedPostgreFactory.embeddedDatasource(embeddedPostgres(), Map.of());
    }

    @Bean
    public SpringLiquibase pgLiquibase() {
        SpringLiquibase result = new SpringLiquibase();
        result.setDataSource(payDataSource());
        result.setChangeLog("classpath:changelog.xml");
        result.setChangeLogParameters(Collections.singletonMap("is-unit-testing", "true"));
        return result;
    }

    @Bean
    public DataSource payDataSource() {
        return embeddedDatasource();
    }

    @Bean
    public DataSource tmsDataSource() {
        return payDataSource();
    }

    @Bean
    public DataSource tmsReplicaDataSource() {
        return payDataSource();
    }

}
