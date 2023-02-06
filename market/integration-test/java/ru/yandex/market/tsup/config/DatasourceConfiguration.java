package ru.yandex.market.tsup.config;

import javax.sql.DataSource;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider;

@Configuration
public class DatasourceConfiguration {
    @Bean
    public DatabaseDataSourceConnectionFactoryBean dbUnitDatabaseConnectionQrtz(
        DataSource dataSource,
        DatabaseConfigBean dbUnitDatabaseConfig
    ) {
        DatabaseDataSourceConnectionFactoryBean dbConnection =
            new DatabaseDataSourceConnectionFactoryBean(dataSource);
        dbConnection.setDatabaseConfig(dbUnitDatabaseConfig);
        dbConnection.setSchema("qrtz");
        return dbConnection;
    }

    @Bean
    public DatabaseDataSourceConnectionFactoryBean dbUnitDatabaseConnectionDbQueue(
        DataSource dataSource,
        DatabaseConfigBean dbUnitDatabaseConfig
    ) {
        DatabaseDataSourceConnectionFactoryBean dbConnection =
            new DatabaseDataSourceConnectionFactoryBean(dataSource);
        dbConnection.setDatabaseConfig(dbUnitDatabaseConfig);
        dbConnection.setSchema("dbqueue");
        return dbConnection;
    }

    @Bean
    public SchemaCleanerConfigProvider publicCleanerConfigProvider() {
        return SchemaCleanerConfigProvider.builder()
            .schema("public")
            .resetSequences()
            .truncateAllExcept("databasechangelog", "databasechangeloglock")
            .build();
    }

    @Bean
    public SchemaCleanerConfigProvider qrtzCleanerConfigProvider() {
        return SchemaCleanerConfigProvider.builder()
            .schema("qrtz")
            .resetSequences()
            .truncateAll()
            .build();
    }

    @Bean
    public SchemaCleanerConfigProvider dbqueueCleanerConfigProvider() {
        return SchemaCleanerConfigProvider.builder()
            .schema("dbqueue")
            .resetSequences()
            .truncateAll()
            .build();
    }
}
