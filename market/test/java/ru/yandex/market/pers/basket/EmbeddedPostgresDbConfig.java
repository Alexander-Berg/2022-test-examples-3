package ru.yandex.market.pers.basket;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.pers.test.db.EmbeddedPostgreFactory;

/**
 * @author ifilippov5
 */
@Configuration
public class EmbeddedPostgresDbConfig {

    public EmbeddedPostgresDbConfig() throws IOException {
    }

    @Bean(destroyMethod = "close")
    public Object embeddedPostgres() {
        return EmbeddedPostgreFactory.embeddedPostgres(x -> x);
    }

    @Bean
    public DataSource pgDatasource() {
        return EmbeddedPostgreFactory.embeddedDatasource(embeddedPostgres(), Map.of());
    }

    @Bean
    public DataSource tmsDatasource() {
        return pgDatasource();
    }

    @Bean
    public JdbcTemplate pgaasJdbcTemplate() {
        return new JdbcTemplate(pgDatasource());
    }

    @Bean
    public JdbcTemplate tmsJdbcTemplate() {
        return new JdbcTemplate(tmsDatasource());
    }

    @Bean
    public SpringLiquibase pgaasLiquibase(){
        SpringLiquibase result = new SpringLiquibase();
        result.setDataSource(pgDatasource());
        result.setChangeLog("classpath:sql/v2/db-changelog-main.xml");
        result.setChangeLogParameters(Collections.singletonMap("is-unit-testing", "true"));
        return result;
    }

    @Bean("transactionTemplate")
    public TransactionTemplate transactionTemplate(PlatformTransactionManager pgaasTransactionManager) {
        return new TransactionTemplate(pgaasTransactionManager);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource pgDatasource) {
        return new DataSourceTransactionManager(pgDatasource);
    }

}
