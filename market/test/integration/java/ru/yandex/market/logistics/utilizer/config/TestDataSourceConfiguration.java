package ru.yandex.market.logistics.utilizer.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.logistics.test.integration.db.TestDatabaseInitializer;
import ru.yandex.market.logistics.test.integration.db.cleaner.DatabaseCleaner;
import ru.yandex.market.logistics.test.integration.db.cleaner.GenericDatabaseCleaner;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.DatabaseCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider;
import ru.yandex.market.logistics.test.integration.db.cleaner.strategy.PostgresDatabaseCleanerStrategy;
import ru.yandex.market.logistics.utilizer.config.db.DatabaseProperties;
import ru.yandex.misc.db.embedded.EmbeddedPostgres;
import ru.yandex.misc.db.embedded.ImportEmbeddedPg;
import ru.yandex.misc.db.embedded.PreparedDbProvider;

@Configuration
@ImportEmbeddedPg
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class TestDataSourceConfiguration {

    @Bean
    public DatabaseProperties databaseProperties(@Value("fulfillment-utilizer.postgresql.properties") String properties) {
        DatabaseProperties dbProperties = new DatabaseProperties();
        dbProperties.setProperties(properties);
        return dbProperties;
    }

    @Bean
    public DataSource dataSource(EmbeddedPostgres postgres,
                                 @Autowired(required = false) TestDatabaseInitializer dbIntializer) {
        return createDataSource(postgres, dbIntializer);
    }

    @Bean
    public DatabaseCleaner databaseCleaner(@Autowired DataSource dataSource,
                                           @Autowired DatabaseCleanerConfig config) {
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
                .schema("dbqueue").truncateAll()
                .build();
    }

    private static DataSource createDataSource(EmbeddedPostgres postgres, TestDatabaseInitializer dbIntializer) {
        PreparedDbProvider provider = PreparedDbProvider.forPreparer("test", postgres);
        var dataSource = provider.createDataSource();
        if (dbIntializer != null) {
            dbIntializer.initializeDatabase(dataSource);
        }
        return dataSource;
    }
}
