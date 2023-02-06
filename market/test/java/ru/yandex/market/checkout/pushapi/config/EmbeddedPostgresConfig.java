package ru.yandex.market.checkout.pushapi.config;

import java.io.IOException;
import java.time.Duration;

import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
public class EmbeddedPostgresConfig {

    @Bean
    public DataSource dataSource() throws IOException {
        return EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(60))
                .setPort(53198)  // фиксирую порт, чтобы можно было подключаться к базе во время дебага
                .start()
                .getPostgresDatabase();
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource) {
        var liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:changelog.xml");
        return liquibase;
    }

    @Bean
    public DataSourceTransactionManager dataSourceTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
