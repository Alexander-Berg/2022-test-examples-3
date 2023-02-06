package ru.yandex.market.logistics.yard.config;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.postgresql.Driver;
import org.springframework.beans.factory.annotation.Autowired;
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
import ru.yandex.misc.db.embedded.EmbeddedPostgres;
import ru.yandex.misc.db.embedded.ImportEmbeddedPg;
import ru.yandex.misc.db.embedded.PreparedDbProvider;

@Configuration
@ImportEmbeddedPg
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class TestDataSourceConfiguration {

    @Bean
    @Primary
    public DataSource dataSource1(EmbeddedPostgres postgres,
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
                        "databasechangeloglock",
                        "document_template",
                        "job_monitoring_config",
                        "system_property"
                ).schema("dbqueue").truncateAll()
                .build();
    }

    private static BasicDataSource createDataSource(EmbeddedPostgres postgres, TestDatabaseInitializer dbIntializer) {
        PreparedDbProvider provider = PreparedDbProvider.forPreparer("test", postgres);
        PreparedDbProvider.DbInfo dbInfo = provider.createDatabase();
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUrl("jdbc:postgresql://" + dbInfo.getHost() + ":" + dbInfo.getPort() + "/" + dbInfo.getDbName());
        dataSource.setUsername(dbInfo.getUser());
        dataSource.setPassword(dbInfo.getPassword());
        dataSource.setDriverClassName(Driver.class.getName());
        dataSource.setValidationQuery("SELECT 1");
        if (dbIntializer != null) {
            dbIntializer.initializeDatabase(dataSource);
        }

        return dataSource;
    }
}
