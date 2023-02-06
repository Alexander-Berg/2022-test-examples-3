package ru.yandex.market.logistics.test.integration.db.cleaner.config.liquibase;

import java.util.Collection;
import java.util.Collections;

import com.google.common.collect.ImmutableSet;

import ru.yandex.market.logistics.test.integration.db.cleaner.config.SchemaCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.SimpleSchemaCleanerConfigBuilder;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider;

public class LiquibaseSchemaCleanerConfigProvider implements SchemaCleanerConfigProvider {

    public static final String LIQUIBASE_BEAN_TYPE = "liquibase.integration.spring.SpringLiquibase";

    public static final ImmutableSet<String> DO_NOT_DELETE_TABLES = ImmutableSet.of(
        "document_template",
        "databasechangelog",
        "databasechangeloglock"
    );

    private final String schema;

    public LiquibaseSchemaCleanerConfigProvider(String schema) {
        this.schema = schema;
    }

    @Override
    public Collection<SchemaCleanerConfig> getConfigurations() {

        return Collections
            .singletonList(
                new SimpleSchemaCleanerConfigBuilder()
                    .setSchemaName(schema)
                    .setIgnore(DO_NOT_DELETE_TABLES)
                    .build()
            );


    }


}
