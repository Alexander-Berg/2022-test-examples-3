package ru.yandex.market.tpl.billing.config.dbunit.configs;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class LiquibaseSchemaCleanerConfigProvider implements SchemaCleanerConfigProvider {

    private static final Set<String> DO_NOT_DELETE_TABLES = Set.of(
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
        return List.of(
            new SimpleSchemaCleanerConfigBuilder()
                .setSchemaName(schema)
                .setIgnore(DO_NOT_DELETE_TABLES)
                .setResetSequences(true)
                .build()
        );
    }

}
