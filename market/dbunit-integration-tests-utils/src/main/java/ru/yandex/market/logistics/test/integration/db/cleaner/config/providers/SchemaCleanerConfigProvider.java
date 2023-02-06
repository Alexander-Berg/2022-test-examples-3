package ru.yandex.market.logistics.test.integration.db.cleaner.config.providers;

import java.util.Collection;

import ru.yandex.market.logistics.test.integration.db.cleaner.config.SchemaCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.base.CompoundDatabaseCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.builder.SchemaCleanerConfigProviderBuilder;

/**
 * @see CompoundDatabaseCleanerConfig
 */
public interface SchemaCleanerConfigProvider {
    Collection<SchemaCleanerConfig> getConfigurations();

    static SchemaCleanerConfigProviderBuilder builder() {
        return new SchemaCleanerConfigProviderBuilder();
    }
}
