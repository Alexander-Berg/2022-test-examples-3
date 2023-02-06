package ru.yandex.market.logistics.test.integration.db.cleaner.config;

import java.util.Collection;
import java.util.Collections;

import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider;


public class DefaultSchemaCleanerConfigProvider implements SchemaCleanerConfigProvider {
    private final String schema;

    public DefaultSchemaCleanerConfigProvider(String defaultSchema) {
        this.schema = defaultSchema;

    }

    @Override
    public Collection<SchemaCleanerConfig> getConfigurations() {

        SimpleSchemaCleanerConfig build = new SimpleSchemaCleanerConfigBuilder()
            .setSchemaName(schema)
            .setResetSequences(true)
            .build();
        build.setDefault(true);
        return Collections
            .singletonList(build);


    }
}
