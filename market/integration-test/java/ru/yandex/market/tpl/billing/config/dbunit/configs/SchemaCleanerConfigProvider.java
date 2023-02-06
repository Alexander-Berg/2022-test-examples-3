package ru.yandex.market.tpl.billing.config.dbunit.configs;

import java.util.Collection;

public interface SchemaCleanerConfigProvider {
    Collection<SchemaCleanerConfig> getConfigurations();
}
