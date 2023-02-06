package ru.yandex.market.wrap.infor.configuration;

import javax.sql.DataSource;

import com.google.common.collect.ImmutableList;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import ru.yandex.market.logistics.test.integration.db.TestDatabaseInitializer;
import ru.yandex.market.logistics.test.integration.db.cleaner.DatabaseCleaner;
import ru.yandex.market.logistics.test.integration.db.cleaner.GenericDatabaseCleaner;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.base.CompoundDatabaseCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider;
import ru.yandex.market.logistics.test.integration.db.cleaner.strategy.H2DatabaseCleanerStrategy;
import ru.yandex.market.logistics.test.integration.db.cleaner.strategy.PostgresDatabaseCleanerStrategy;
import ru.yandex.misc.db.embedded.EmbeddedPostgres;
import ru.yandex.misc.db.embedded.EmbeddedPostgresConfiguration;
import ru.yandex.misc.db.embedded.PreparedDbProvider;

import static ru.yandex.market.wrap.infor.configuration.DataSourcesConfiguration.WRAP_DATASOURCE;

@Configuration
@Import(value = {
    DataSourcesConfiguration.class,
    EmbeddedPostgresConfiguration.class
})
public class IntegrationTestDataSourcesConfiguration {

    public static final String WRAP_DATASOURCE_SCHEMA = "public";
    private static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";
    public static final String TEST_WMS_FIRST_DATASOURCE = "wmsInforFirstDataSourceForTestOnly";
    public static final String TEST_WMS_SECOND_DATASOURCE = "wmsInforSecondDataSourceForTestOnly";
    public static final String WMS_DATASOURCE_SCHEMA = "WMWHSE1";
    public static final String DATABASE_POPULATOR = "DatabasePopulator";

    @Value("classpath:wms-schema.sql")
    private Resource wmsSchemaSql;

    @Primary
    @Bean(name = WRAP_DATASOURCE)
    public DataSource dataSource(EmbeddedPostgres postgres,
                                 @Autowired(required = false) TestDatabaseInitializer dbIntializer) {
        return createDataSource(postgres, dbIntializer);
    }

    @Bean(name = TEST_WMS_FIRST_DATASOURCE)
    @ConfigurationProperties(prefix = "fulfillment.wrap.infor.wms.test.datasource", ignoreUnknownFields = false)
    public DataSource wmsInforDataSource() {
        return DataSourceBuilder
            .create()
            .build();
    }

    @Bean(name = TEST_WMS_SECOND_DATASOURCE)
    @ConfigurationProperties(prefix = "fulfillment.wrap.infor.wms.test.datasource2", ignoreUnknownFields = false)
    public DataSource wmsInforDataSource2() {
        return DataSourceBuilder
            .create()
            .build();
    }

    @Bean
    public DatabaseCleaner wrapDatabaseCleaner(@Qualifier(WRAP_DATASOURCE) DataSource dataSource) {
        return new GenericDatabaseCleaner(
            dataSource,
            new CompoundDatabaseCleanerConfig(ImmutableList.of(wrapInforDatabaseCleanerConfig())),
            new PostgresDatabaseCleanerStrategy()
        );
    }

    @Bean(name = DATABASE_POPULATOR)
    public DatabasePopulator wrapDatabasePopulator(@Qualifier(WRAP_DATASOURCE) DataSource dataSource) {
        return populatorWithoutSchema(dataSource);
    }

    @Bean
    public DatabaseCleaner wmsDatabaseCleaner(@Qualifier(TEST_WMS_FIRST_DATASOURCE) DataSource dataSource) {
        return new GenericDatabaseCleaner(
            dataSource,
            new CompoundDatabaseCleanerConfig(ImmutableList.of(wmsInforDatabaseCleanerConfig())),
            new H2DatabaseCleanerStrategy()
        );
    }

    @Bean
    protected DatabasePopulator wmsDatabasePopulator(@Qualifier(TEST_WMS_FIRST_DATASOURCE) DataSource dataSource) {
        return populator(dataSource, wmsSchemaSql);
    }

    @Bean
    protected DatabasePopulator wmsDatabasePopulator2(@Qualifier(TEST_WMS_SECOND_DATASOURCE) DataSource dataSource) {
        return populator(dataSource, wmsSchemaSql);
    }

    @Bean
    protected SchemaCleanerConfigProvider wrapInforDatabaseCleanerConfig() {
        return SchemaCleanerConfigProvider
            .builder()
            .schema(WRAP_DATASOURCE_SCHEMA).resetSequences().truncateAllExcept(
                "numbers",
                "databasechangelog",
                "databasechangeloglock",
                "document_template"
            )
            .build();
    }

    @Bean
    protected SchemaCleanerConfigProvider wmsInforDatabaseCleanerConfig() {
        return SchemaCleanerConfigProvider
            .builder()
            .schema(WMS_DATASOURCE_SCHEMA).resetSequences().truncateAll()
            .build();
    }

    private DatabasePopulator populator(DataSource dataSource,
                                        Resource schema) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(schema);
        DatabasePopulatorUtils.execute(populator, dataSource);

        return populator;
    }

    private DatabasePopulator populatorWithoutSchema(DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        DatabasePopulatorUtils.execute(populator, dataSource);

        return populator;
    }

    private static BasicDataSource createDataSource(EmbeddedPostgres postgres,
                                                    TestDatabaseInitializer dbIntializer) {
        PreparedDbProvider provider = PreparedDbProvider.forPreparer("test", postgres);
        PreparedDbProvider.DbInfo dbInfo = provider.createDatabase();
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUrl("jdbc:postgresql://" + dbInfo.getHost() + ":" + dbInfo.getPort() + "/" + dbInfo.getDbName());
        dataSource.setUsername(dbInfo.getUser());
        dataSource.setPassword(dbInfo.getPassword());
        dataSource.setDriverClassName(POSTGRESQL_DRIVER);
        dataSource.setValidationQuery("SELECT 1");
        if (dbIntializer != null) {
            dbIntializer.initializeDatabase(dataSource);
        }
        return dataSource;
    }
}
