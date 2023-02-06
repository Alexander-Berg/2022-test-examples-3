package ru.yandex.market.logistics.test.integration.db.cleaner.config;

import java.util.Set;

import ru.yandex.market.logistics.test.integration.db.cleaner.config.base.CompoundDatabaseCleanerConfig;

/**
 * @see CompoundDatabaseCleanerConfig
 */
public interface DatabaseCleanerConfig {

    Set<String> getSchemas();

    SchemaCleanerConfig getConfigForSchema(String schema);
}
