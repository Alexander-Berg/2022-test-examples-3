package ru.yandex.market.tpl.billing.config.dbunit.configs;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
                Function.identity(),
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
