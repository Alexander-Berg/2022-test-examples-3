package ru.yandex.market.logistics.tarifficator.configuration;

import javax.sql.DataSource;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.logistics.test.integration.db.PostgresqlDataTypeFactoryExt;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider;

@Configuration
public class DbUnitConfiguration {
    @Bean
    public DatabaseConfigBean dbUnitQualifiedDatabaseConfig() {
        DatabaseConfigBean dbConfig = new DatabaseConfigBean();
        dbConfig.setQualifiedTableNames(true);
        dbConfig.setAllowEmptyFields(true);
        dbConfig.setDatatypeFactory(new PostgresqlDataTypeFactoryExt());
        return dbConfig;
    }

    @Bean
    public DatabaseDataSourceConnectionFactoryBean dbUnitQualifiedDatabaseConnection(
        DataSource dataSource,
        DatabaseConfigBean dbUnitQualifiedDatabaseConfig
    ) {
        DatabaseDataSourceConnectionFactoryBean dbConnection = new DatabaseDataSourceConnectionFactoryBean(dataSource);
        dbConnection.setDatabaseConfig(dbUnitQualifiedDatabaseConfig);

        return dbConnection;
    }

    @Bean
    public SchemaCleanerConfigProvider tplSchemaCleanerConfigProvider() {
        return SchemaCleanerConfigProvider.builder()
            .schema("tpl")
            .resetSequences()
            .truncateAll()
            .build();
    }

    @Bean
    public SchemaCleanerConfigProvider marketShopTariffSchemaCleanerConfigProvider() {
        return SchemaCleanerConfigProvider.builder()
            .schema("market_shop_tariff")
            .resetSequences()
            .truncateAll()
            .build();
    }
}
