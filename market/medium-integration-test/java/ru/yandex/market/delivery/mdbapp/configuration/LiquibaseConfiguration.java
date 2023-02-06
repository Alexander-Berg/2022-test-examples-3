package ru.yandex.market.delivery.mdbapp.configuration;

import java.util.Optional;
import java.util.function.Supplier;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.logistics.test.integration.db.cleaner.DataSourceUtils;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.liquibase.LiquibaseSchemaCleanerConfigProvider;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider;

@Configuration
@EnableAutoConfiguration(exclude = LiquibaseAutoConfiguration.class)
public class LiquibaseConfiguration {

    @Bean
    public EntityManagerFactoryDependsOnPostProcessor emDepends() {
        return new EntityManagerFactoryDependsOnPostProcessor("liquibase");
    }

    @Bean
    @Primary
    public SpringLiquibase liquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:config/changelog.xml");
        return liquibase;
    }

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
