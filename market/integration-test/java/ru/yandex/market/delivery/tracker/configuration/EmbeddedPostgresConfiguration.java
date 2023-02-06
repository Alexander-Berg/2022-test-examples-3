package ru.yandex.market.delivery.tracker.configuration;

import java.io.IOException;
import java.util.List;

import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistics.test.integration.db.cleaner.DatabaseCleaner;
import ru.yandex.market.logistics.test.integration.db.cleaner.GenericDatabaseCleaner;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.DatabaseCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.base.CompoundDatabaseCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider;
import ru.yandex.market.logistics.test.integration.db.cleaner.strategy.PostgresDatabaseCleanerStrategy;

@Configuration
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class EmbeddedPostgresConfiguration {

    @Bean
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.builder()
            .setServerConfig("unix_socket_directories", "")
            .start();
    }

    @Bean
    @Primary
    public DataSource dataSource(EmbeddedPostgres embeddedPostgres) {
        DataSource dataSource = embeddedPostgres.getPostgresDatabase();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS DELIVERY_TRACKER;");
        jdbcTemplate.execute("CREATE ROLE marketstat_delivery_tracker;");
        ((PGSimpleDataSource) dataSource).setCurrentSchema("DELIVERY_TRACKER");

        return dataSource;
    }

    @Bean
    public DatabaseCleaner databaseCleaner(DataSource dataSource, DatabaseCleanerConfig config) {
        return new GenericDatabaseCleaner(dataSource, config, new PostgresDatabaseCleanerStrategy());
    }

    @Bean
    public SchemaCleanerConfigProvider customSchemaCleanerConfigProvider() {
        return SchemaCleanerConfigProvider.builder()
            .schema("DELIVERY_TRACKER")
            .resetSequences()
            .truncateAllExcept("databasechangelog", "databasechangeloglock")
            .build();
    }

    @Bean
    @Primary
    public DatabaseCleanerConfig cleanerConfig(@Autowired List<SchemaCleanerConfigProvider> configList) {
        return new CompoundDatabaseCleanerConfig(configList);
    }
}
