package ru.yandex.market.logistics.test.integration.db.cleaner.config;

import java.util.Set;

import ru.yandex.market.logistics.test.integration.db.cleaner.config.base.CompoundDatabaseCleanerConfig;

/**
 * @see CompoundDatabaseCleanerConfig
 */
public interface SchemaCleanerConfig {

    String getSchemaName();

    Set<String> shouldBeTruncated();

    Set<String> shouldNotBeIgnored();

    Boolean resetSequences();

    boolean isDefault();
}
