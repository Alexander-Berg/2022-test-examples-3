package ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration;

import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.IDatabaseTester;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.yandex.market.logistics.test.integration.db.EnableEmbeddedPostgres;
import ru.yandex.market.logistics.test.integration.db.LiquibaseTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider;

import javax.sql.DataSource;

@Configuration
@EnableEmbeddedPostgres
@Import(LiquibaseTestConfiguration.class)
public class EmbeddedPostgresConfiguration {

    @Bean
    public SchemaCleanerConfigProvider customSchemaCleanerConfigProvider() {
        return SchemaCleanerConfigProvider
            .builder()
            .schema("public").resetSequences().truncateAllExcept("delivery_service_meta")
            .schema("qrtz").resetSequences().truncateAll()
            .build();
    }

    @Bean
    public IDatabaseTester databaseTester(DataSource dataSource) {
        return new DataSourceDatabaseTester(dataSource);
    }
}
