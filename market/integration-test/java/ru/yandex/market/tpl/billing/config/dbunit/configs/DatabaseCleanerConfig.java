package ru.yandex.market.tpl.billing.config.dbunit.configs;

import java.util.Set;

public interface DatabaseCleanerConfig {
    Set<String> getSchemas();

    SchemaCleanerConfig getConfigForSchema(String schema);
}
