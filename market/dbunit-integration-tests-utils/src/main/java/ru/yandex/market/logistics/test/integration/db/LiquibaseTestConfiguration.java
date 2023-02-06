package ru.yandex.market.logistics.test.integration.db;

import java.util.Optional;
import java.util.function.Supplier;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.logistics.test.integration.db.cleaner.DataSourceUtils;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.liquibase.LiquibaseSchemaCleanerConfigProvider;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider;

import static ru.yandex.market.logistics.test.integration.db.cleaner.config.liquibase.LiquibaseSchemaCleanerConfigProvider.LIQUIBASE_BEAN_TYPE;

@Configuration
@ConditionalOnBean(type = LIQUIBASE_BEAN_TYPE)
@ConditionalOnClass(SpringLiquibase.class)
public class LiquibaseTestConfiguration {
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
            .orElseGet(() -> DataSourceUtils.getDefaultSchemaFromConnection(liquibase.getDataSource()));
    }
}
