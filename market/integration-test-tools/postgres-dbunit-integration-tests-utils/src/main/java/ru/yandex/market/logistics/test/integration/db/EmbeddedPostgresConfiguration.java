package ru.yandex.market.logistics.test.integration.db;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.postgresql.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.logistics.test.integration.db.cleaner.DatabaseCleaner;
import ru.yandex.market.logistics.test.integration.db.cleaner.GenericDatabaseCleaner;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.DatabaseCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.strategy.PostgresDatabaseCleanerStrategy;
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;

/**
 * Конфигурация  для настройки embedded postgres
 *
 * @see EnableEmbeddedPostgres
 */
@Configuration
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
@ConditionalOnClass({BasicDataSource.class, Driver.class})
public class EmbeddedPostgresConfiguration {

    @Bean
    @Primary
    public DataSource dataSource(
        EmbeddedPostgres postgres,
        @Autowired(required = false) TestDatabaseInitializer dbIntializer
    ) {
        return createDataSource(postgres, dbIntializer);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public EmbeddedPostgres embeddedPostgres() {
        return new EmbeddedPostgres();
    }

    /**
     * чистильщик бд для DB unit
     *
     * @param dataSource
     * @param config
     * @return
     * @see ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener
     */
    @Bean
    public DatabaseCleaner databaseCleaner(@Autowired DataSource dataSource,
                                           @Autowired DatabaseCleanerConfig config) {
        return new GenericDatabaseCleaner(dataSource, config, new PostgresDatabaseCleanerStrategy());
    }

    public static BasicDataSource createDataSource(EmbeddedPostgres postgres,
                                                   TestDatabaseInitializer dbIntializer) {
        PostgresConfig postgresConfig = getPostgresConfig(postgres);
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUrl(getConnectionUrl(postgres));
        dataSource.setUsername(postgresConfig.credentials().username());
        dataSource.setPassword(postgresConfig.credentials().password());
        dataSource.setDriverClassName(Driver.class.getName());
        dataSource.setValidationQuery("SELECT 1");
        if (dbIntializer != null) {
            dbIntializer.initializeDatabase(dataSource);
        }

        return dataSource;
    }

    public static PostgresConfig getPostgresConfig(EmbeddedPostgres postgres) {
        return postgres.getConfig()
            .orElseThrow(() -> new IllegalStateException("Unable to extract embedded pg config"));
    }

    public static String getConnectionUrl(EmbeddedPostgres postgres) {
        return postgres.getConnectionUrl()
            .orElseThrow(() -> new IllegalStateException("Failed to start embedded pg database"));
    }
}
