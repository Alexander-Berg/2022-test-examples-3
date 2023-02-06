package ru.yandex.market.logistics.test.integration.db.cleaner.config.providers;

import java.util.Collection;

import ru.yandex.market.logistics.test.integration.db.cleaner.config.SchemaCleanerConfig;

public class SimpleSchemaCleanerConfigProvider implements SchemaCleanerConfigProvider {

    private final Collection<SchemaCleanerConfig> configs;

    public SimpleSchemaCleanerConfigProvider(Collection<SchemaCleanerConfig> configs) {
        this.configs = configs;
    }

    @Override
    public Collection<SchemaCleanerConfig> getConfigurations() {
        return configs;
    }
}
