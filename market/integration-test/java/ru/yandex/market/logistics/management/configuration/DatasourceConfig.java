package ru.yandex.market.logistics.management.configuration;

import java.util.List;

import javax.sql.DataSource;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistics.management.configuration.dbunit.LmsDataTypeFactory;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.DatabaseCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.base.CompoundDatabaseCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider;

@TestConfiguration
@SuppressWarnings({"checkstyle:MagicNumber"})
public class DatasourceConfig {

    @Bean
    public DataSource masterDataSource(DataSource dataSource) {
        return dataSource;
    }

    @Bean
    public DataSource replicaDataSource(DataSource dataSource) {
        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "yado.datasource")
    public DataSource yadoDatasource() {
        return Mockito.mock(DataSource.class);
    }

    @Bean(name = "quartz.datasource")
    public DataSource quartzDatasource(DataSource dataSource) {
        return dataSource;
    }

    @Bean
    public DatabaseConfigBean dbUnitDatabaseConfig() {
        DatabaseConfigBean dbConfig = new com.github.springtestdbunit.bean.DatabaseConfigBean();
        dbConfig.setDatatypeFactory(new LmsDataTypeFactory());
        dbConfig.setAllowEmptyFields(true);
        return dbConfig;
    }

    @Bean
    public DatabaseConfigBean dbUnitQualifiedDatabaseConfig() {
        DatabaseConfigBean dbConfig = new com.github.springtestdbunit.bean.DatabaseConfigBean();
        dbConfig.setQualifiedTableNames(true);
        dbConfig.setDatatypeFactory(new LmsDataTypeFactory());
        dbConfig.setAllowEmptyFields(true);
        return dbConfig;
    }

    @Bean
    public DatabaseDataSourceConnectionFactoryBean dbUnitDatabaseConnection(
        DataSource dataSource,
        DatabaseConfigBean dbUnitDatabaseConfig
    ) {
        DatabaseDataSourceConnectionFactoryBean dbConnection =
            new com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean(dataSource);
        dbConnection.setDatabaseConfig(dbUnitDatabaseConfig);
        dbConnection.setSchema("public");
        return dbConnection;
    }

    @Bean
    public DatabaseDataSourceConnectionFactoryBean dbUnitQualifiedDatabaseConnection(
        DataSource dataSource,
        DatabaseConfigBean dbUnitQualifiedDatabaseConfig
    ) {
        DatabaseDataSourceConnectionFactoryBean dbConnection =
            new com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean(dataSource);
        dbConnection.setDatabaseConfig(dbUnitQualifiedDatabaseConfig);
        return dbConnection;
    }

    @Bean
    public DatabaseCleanerConfig databaseCleanerConfig() {
        return new CompoundDatabaseCleanerConfig(
            List.of(
                SchemaCleanerConfigProvider.builder()
                    .schema("public")
                    .resetSequences()
                    .truncateAllExcept("databasechangelog", "databasechangeloglock")
                    .build(),
                SchemaCleanerConfigProvider.builder()
                    .schema("yt")
                    .resetSequences()
                    .truncateAll()
                    .build(),
                SchemaCleanerConfigProvider.builder()
                    .schema("dbqueue")
                    .resetSequences()
                    .truncateAll()
                    .build()
            )
        );
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
}
