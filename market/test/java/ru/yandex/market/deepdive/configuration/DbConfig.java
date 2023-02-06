package ru.yandex.market.deepdive.configuration;

import java.io.IOException;
import java.time.Duration;

import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author imelnikov
 */
@Configuration
public class DbConfig {

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource) {
        var liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:liquibase/pg.schema.xml");
        return liquibase;
    }

    @Bean
    public DataSource dataSource() throws IOException {
        DataSource dataSource = EmbeddedPostgres.builder()
                .setPGStartupWait(Duration.ofSeconds(60))
                .setPort(53198)  // фиксирую порт, чтобы можно было подключаться к базе во время дебага
                .start()
                .getPostgresDatabase();

        return dataSource;
    }
}
