package ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.builder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.assertj.core.util.Preconditions;

import ru.yandex.market.logistics.test.integration.db.cleaner.config.SchemaCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.SimpleSchemaCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.SimpleSchemaCleanerConfigBuilder;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SchemaCleanerConfigProvider;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.providers.SimpleSchemaCleanerConfigProvider;


public class SchemaCleanerConfigProviderBuilder {
    private final Map<String, SchemaCleanerConfig> readyConfigs = new HashMap<>();

    public SchemaCleanerConfigProviderBuilder() {
    }

    private void checkAlreadyExists(String schema) {
        if (readyConfigs.containsKey(schema)) {
            throw new IllegalStateException("Schema " + schema + " already defined");
        }
    }

    public SchemaCleanerConfigBuilder schema(String schema) {
        checkAlreadyExists(schema);
        return new SchemaCleanerConfigBuilder(schema);
    }

    public SchemaCleanerConfigProvider build() {
        Preconditions.checkArgument(!readyConfigs.isEmpty(), "No configurations specified");
        return new SimpleSchemaCleanerConfigProvider(readyConfigs.values());
    }

    public class SchemaCleanerConfigBuilder {
        private final String schemaName;
        private Set<String> truncate = Collections.emptySet();
        private Set<String> ignore = Collections.emptySet();
        private Boolean resetSequences;


        public SchemaCleanerConfigBuilder(String schemaName) {
            this.schemaName = schemaName;
        }

        public SchemaCleanerConfigBuilder dontResetSequences() {
            this.resetSequences = false;
            return this;
        }

        public SchemaCleanerConfigBuilder resetSequences() {
            this.resetSequences = true;
            return this;
        }

        public SchemaCleanerConfigProviderBuilder truncateAll() {
            ignore = Collections.emptySet();
            truncate = Collections.emptySet();
            return finish();
        }

        public SchemaCleanerConfigProviderBuilder truncateAllExcept(String... tables) {
            ignore = ImmutableSet.copyOf(tables);
            truncate = Collections.emptySet();
            return finish();
        }

        public SchemaCleanerConfigProviderBuilder truncateOnly(String... tables) {
            truncate = ImmutableSet.copyOf(tables);
            ignore = Collections.emptySet();
            return finish();
        }

        private SchemaCleanerConfigProviderBuilder finish() {
            SimpleSchemaCleanerConfig config = new SimpleSchemaCleanerConfigBuilder()
                .setSchemaName(schemaName)
                .setIgnore(ignore)
                .setResetSequences(resetSequences)
                .setTruncate(truncate).build();

            SchemaCleanerConfigProviderBuilder
                .this.readyConfigs.put(schemaName, config);
            return SchemaCleanerConfigProviderBuilder.this;
        }

    }
}
