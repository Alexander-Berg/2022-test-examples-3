package ru.yandex.market.logistics.test.integration.db.container;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;

import ru.yandex.market.logistics.test.integration.db.cleaner.DatabaseCleaner;
import ru.yandex.market.logistics.test.integration.db.cleaner.GenericDatabaseCleaner;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.DatabaseCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider;
import ru.yandex.market.logistics.test.integration.db.cleaner.strategy.PostgresDatabaseCleanerStrategy;

@Configuration
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class TestDatasourceConfiguration {

    @Bean
    @Primary
    public DataSource dataSource(PostgreSQLContainer postgres) {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUsername(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        dataSource.setValidationQuery("SELECT 1");
        return dataSource;
    }

    @Bean
    public DatabaseCleaner databaseCleaner(
        DataSource dataSource,
        DatabaseCleanerConfig config
    ) {
        return new GenericDatabaseCleaner(dataSource, config, new PostgresDatabaseCleanerStrategy());
    }

    @Bean
    public SchemaCleanerConfigProvider customSchemaCleanerConfigProvider() {
        return SchemaCleanerConfigProvider
            .builder()
            .schema("public").resetSequences().truncateAllExcept(
                "databasechangelog",
                "databasechangeloglock"
            )
            .build();
    }
}
