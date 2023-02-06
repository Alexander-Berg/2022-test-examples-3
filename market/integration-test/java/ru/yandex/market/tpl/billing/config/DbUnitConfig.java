package ru.yandex.market.tpl.billing.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.tpl.billing.config.dbunit.cleaner.DatabaseCleaner;
import ru.yandex.market.tpl.billing.config.dbunit.cleaner.GenericDatabaseCleaner;
import ru.yandex.market.tpl.billing.config.dbunit.configs.CompoundDatabaseCleanerConfig;
import ru.yandex.market.tpl.billing.config.dbunit.configs.DatabaseCleanerConfig;
import ru.yandex.market.tpl.billing.config.dbunit.configs.LiquibaseSchemaCleanerConfigProvider;
import ru.yandex.market.tpl.billing.config.dbunit.configs.SchemaCleanerConfigProvider;
import ru.yandex.market.tpl.billing.config.dbunit.strategy.PostgresDatabaseCleanerStrategy;

@Configuration
public class DbUnitConfig {
    @Bean
    public SchemaCleanerConfigProvider liquibaseCleanerConfig(
        @Qualifier("liquibaseSchemaSupplier") Supplier<String> supplier
    ) {
        return new LiquibaseSchemaCleanerConfigProvider(supplier.get());
    }

    @Bean
    @Qualifier("liquibaseSchemaSupplier")
    protected Supplier<String> defaultLiquibaseSchemaSupplier(SpringLiquibase liquibase) {
        return () -> Optional.ofNullable(liquibase.getDefaultSchema())
            .orElseGet(() -> getDefaultSchemaFromDataSource(liquibase.getDataSource()));
    }

    private String getDefaultSchemaFromDataSource(DataSource dataSource) {
        try (Connection c = dataSource.getConnection()) {
            return c.getSchema();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    @Primary
    public DatabaseCleanerConfig cleanerConfig(@Autowired List<SchemaCleanerConfigProvider> configList) {
        return new CompoundDatabaseCleanerConfig(configList);
    }

    @Bean
    public DatabaseCleaner databaseCleaner(
        @Autowired DataSource dataSource,
        @Autowired DatabaseCleanerConfig config
    ) {
        return new GenericDatabaseCleaner(dataSource, config, new PostgresDatabaseCleanerStrategy());
    }

}
