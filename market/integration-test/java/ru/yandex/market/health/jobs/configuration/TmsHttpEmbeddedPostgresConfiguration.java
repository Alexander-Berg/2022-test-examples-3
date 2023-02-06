package ru.yandex.market.health.jobs.configuration;

import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.IDatabaseTester;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
public class TmsHttpEmbeddedPostgresConfiguration {

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws Exception {
        return EmbeddedPostgres.start();
    }

    @Primary
    @Bean
    public DataSource dataSource(EmbeddedPostgres embeddedPostgres) {
        return embeddedPostgres.getPostgresDatabase();
    }

    @Bean
    public IDatabaseTester databaseTester(DataSource dataSource) {
        return new DataSourceDatabaseTester(dataSource);
    }
}
