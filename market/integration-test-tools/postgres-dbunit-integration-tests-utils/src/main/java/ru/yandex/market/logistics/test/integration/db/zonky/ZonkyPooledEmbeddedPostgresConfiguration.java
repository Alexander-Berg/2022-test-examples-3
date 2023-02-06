package ru.yandex.market.logistics.test.integration.db.zonky;

import java.io.IOException;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.logistics.test.integration.db.cleaner.DatabaseCleaner;
import ru.yandex.market.logistics.test.integration.db.cleaner.GenericDatabaseCleaner;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.DatabaseCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider;
import ru.yandex.market.logistics.test.integration.db.cleaner.strategy.PostgresDatabaseCleanerStrategy;

@Configuration
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class ZonkyPooledEmbeddedPostgresConfiguration {

    @Bean
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.builder()
            .setServerConfig("unix_socket_directories", "")
            .start();
    }

    @Bean
    @Primary
    public DataSource dataSource(EmbeddedPostgres embeddedPostgres) {
        HikariConfig config = new HikariConfig();
        config.setDataSource(embeddedPostgres.getPostgresDatabase());
        config.setMaximumPoolSize(20);
        return (DataSource) new HikariDataSource(config);
    }

    @Bean
    public DatabaseCleaner databaseCleaner(DataSource dataSource, DatabaseCleanerConfig config) {
        return new GenericDatabaseCleaner(dataSource, config, new PostgresDatabaseCleanerStrategy());
    }

    @Bean
    public SchemaCleanerConfigProvider customSchemaCleanerConfigProvider() {
        return SchemaCleanerConfigProvider.builder()
            .schema("public")
            .resetSequences()
            .truncateAllExcept("databasechangelog", "databasechangeloglock")
            .build();
    }
}
