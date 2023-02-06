package ru.yandex.market.tpl.billing.config.dbunit.configs;

import java.util.Set;

public interface SchemaCleanerConfig {

    String getSchemaName();

    Set<String> shouldBeTruncated();

    Set<String> shouldNotBeIgnored();

    Boolean resetSequences();

    boolean isDefault();
}
