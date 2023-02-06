package ru.yandex.market.fulfillment.stockstorage.configuration;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;

import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.apache.commons.dbcp2.BasicDataSource;
import org.postgresql.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.solomon.jobs.pghealth.CloseableDatabaseHealthFactory;
import ru.yandex.market.logistics.test.integration.db.LiquibaseTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.TestDatabaseInitializer;
import ru.yandex.market.logistics.test.integration.db.cleaner.DatabaseCleaner;
import ru.yandex.market.logistics.test.integration.db.cleaner.GenericDatabaseCleaner;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.DatabaseCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider;
import ru.yandex.market.logistics.test.integration.db.cleaner.strategy.PostgresDatabaseCleanerStrategy;

@Configuration
@Import({LiquibaseTestConfiguration.class})
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
@ConditionalOnClass({BasicDataSource.class, Driver.class})
public class StockStorageEmbeddedPostgresConfiguration {

    private static final Duration STARTUP_WAIT = Duration.ofSeconds(45);

    public StockStorageEmbeddedPostgresConfiguration() {
    }

    @Bean
    public EmbeddedPostgres postgres() throws IOException {
        return EmbeddedPostgres.builder().setPGStartupWait(STARTUP_WAIT).start();
    }

    @Bean
    @Primary
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
                        "databasechangeloglock",
                        "document_template",
                        "job_monitoring_config"
                )
                .schema("qrtz").resetSequences().truncateAll()
                .build();
    }

    @Bean
    public DataSource qrtzDataSource(EmbeddedPostgres postgres,
                                     @Autowired(required = false) TestDatabaseInitializer dbIntializer) {
        BasicDataSource dataSource = createDataSource(postgres, dbIntializer);
        dataSource.setConnectionInitSqls(Collections.singletonList("SET search_path TO qrtz;"));

        return dataSource;
    }

    @Bean(name = "qrtzDataSource")
    public DataSource qrtzDataSource(DataSource qrtzDataSource) {
        return qrtzDataSource;
    }

    @Bean(name = "replicaDataSource")
    public DataSource replicaDatasource(EmbeddedPostgres postgres,
                                        @Autowired(required = false) TestDatabaseInitializer dbIntializer) {
        return createDataSource(postgres, dbIntializer);
    }

    @Bean
    public CloseableDatabaseHealthFactory databaseHealthFactory(EmbeddedPostgres postgres) {
        String jdbcUrl = postgres.getJdbcUrl("postgres", "postgres");
        return new CloseableDatabaseHealthFactory(jdbcUrl, "postgres", "postgres");
    }

    public BasicDataSource createDataSource(EmbeddedPostgres postgres,
                                            TestDatabaseInitializer dbIntializer) {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUrl(postgres.getJdbcUrl("postgres", "postgres"));
        dataSource.setDriverClassName(Driver.class.getName());
        dataSource.setValidationQuery("SELECT 1");
        if (dbIntializer != null) {
            dbIntializer.initializeDatabase(dataSource);
        }

        return dataSource;
    }
}
