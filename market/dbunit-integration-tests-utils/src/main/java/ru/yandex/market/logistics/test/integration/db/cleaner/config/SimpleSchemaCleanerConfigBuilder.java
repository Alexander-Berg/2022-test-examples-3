package ru.yandex.market.logistics.test.integration.db.cleaner.config;

import java.util.Collections;
import java.util.Set;

public class SimpleSchemaCleanerConfigBuilder {
    private String schemaName;
    private Set<String> truncate = Collections.emptySet();
    private Set<String> ignore = Collections.emptySet();
    private Boolean resetSequences;

    public SimpleSchemaCleanerConfigBuilder setSchemaName(String schemaName) {
        this.schemaName = schemaName;
        return this;
    }

    public SimpleSchemaCleanerConfigBuilder setTruncate(Set<String> truncate) {
        this.truncate = truncate;
        return this;
    }

    public SimpleSchemaCleanerConfigBuilder setIgnore(Set<String> ignore) {
        this.ignore = ignore;
        return this;
    }

    public SimpleSchemaCleanerConfigBuilder setResetSequences(Boolean resetSequences) {
        this.resetSequences = resetSequences;
        return this;
    }


    public SimpleSchemaCleanerConfig build() {
        return new SimpleSchemaCleanerConfig(schemaName, truncate, ignore, resetSequences);
    }
}
