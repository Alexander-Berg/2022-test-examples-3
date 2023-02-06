package ru.yandex.market.logistics.test.integration.db.cleaner.config.base;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import ru.yandex.market.logistics.test.integration.db.cleaner.config.DatabaseCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.SchemaCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.SimpleSchemaCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider;

/**
 * Основная имплементация дб клинера
 */
public final class CompoundDatabaseCleanerConfig implements DatabaseCleanerConfig {


    private final Map<String, SchemaCleanerConfig> configs;


    public CompoundDatabaseCleanerConfig(List<SchemaCleanerConfigProvider> configs) {
        this.configs = createConfigsMap(configs);

    }

    private Map<String, SchemaCleanerConfig> createConfigsMap(List<SchemaCleanerConfigProvider> configs) {
        return configs.stream()
            .flatMap(e -> e.getConfigurations().stream())
            .collect(Collectors.toMap(
                SchemaCleanerConfig::getSchemaName,
                e -> e,
                SimpleSchemaCleanerConfig::merge
            ));
    }


    @Override
    public Set<String> getSchemas() {
        return configs.keySet();
    }

    @Override
    public SchemaCleanerConfig getConfigForSchema(String schema) {
        return Optional.ofNullable(configs.get(schema))
            .orElseThrow(() -> new IllegalArgumentException(String.format("Schema %s not configured", schema)));
    }


}
