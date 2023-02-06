package ru.yandex.market.logistics.dbqueue.config;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.postgresql.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionOperations;

import ru.yandex.market.logistics.dbqueue.model.DbQueueProperties;
import ru.yandex.market.logistics.dbqueue.shard.CustomQueueShard;
import ru.yandex.market.logistics.test.integration.db.TestDatabaseInitializer;
import ru.yandex.market.logistics.test.integration.db.cleaner.DatabaseCleaner;
import ru.yandex.market.logistics.test.integration.db.cleaner.GenericDatabaseCleaner;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.DatabaseCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider;
import ru.yandex.market.logistics.test.integration.db.cleaner.strategy.PostgresDatabaseCleanerStrategy;
import ru.yandex.misc.db.embedded.EmbeddedPostgres;
import ru.yandex.misc.db.embedded.ImportEmbeddedPg;
import ru.yandex.misc.db.embedded.PreparedDbProvider;
import ru.yandex.money.common.dbqueue.config.DatabaseDialect;
import ru.yandex.money.common.dbqueue.config.QueueShard;
import ru.yandex.money.common.dbqueue.config.QueueShardId;
import ru.yandex.money.common.dbqueue.config.QueueTableSchema;

@Configuration
@ImportEmbeddedPg
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class TestDataSourceConfiguration {
    @Autowired
    private DbQueueProperties dbqueueProperties;

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
                        "databasechangeloglock"
                ).schema("dbqueue").truncateAll()
                .build();
    }

    @Bean
    public QueueShard queueShard(DataSource dataSource, TransactionOperations transactionOperations) {
        return new CustomQueueShard(
                DatabaseDialect.POSTGRESQL,
                QueueTableSchema.builder().build(),
                new QueueShardId(dbqueueProperties.getQueueShardId()),
                new JdbcTemplate(dataSource),
                transactionOperations
        );
    }
}
