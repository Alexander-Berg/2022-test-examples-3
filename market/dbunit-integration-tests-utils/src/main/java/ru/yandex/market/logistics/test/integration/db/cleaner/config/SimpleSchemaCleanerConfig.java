package ru.yandex.market.logistics.test.integration.db.cleaner.config;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import org.assertj.core.util.Preconditions;

public class SimpleSchemaCleanerConfig implements SchemaCleanerConfig {
    private final String schemaName;
    private Set<String> truncate = Collections.emptySet();
    private Set<String> ignore = Collections.emptySet();
    private Boolean resetSequences;
    private boolean isDefault = false;

    public SimpleSchemaCleanerConfig(
        String schemaName,
        Set<String> truncate,
        Set<String> ignore,
        Boolean resetSequences
    ) {
        this.schemaName = schemaName;
        this.truncate = truncate;
        this.ignore = ignore;
        this.resetSequences = resetSequences;
    }

    public SimpleSchemaCleanerConfig(String schema) {
        this.schemaName = schema;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public SimpleSchemaCleanerConfig setShouldBeTruncated(Set<String> shouldBetruncated) {
        this.truncate = shouldBetruncated;
        return this;
    }


    public SimpleSchemaCleanerConfig setShouldNotBeTruncated(Set<String> shouldNotBetruncated) {
        this.ignore = shouldNotBetruncated;
        return this;
    }


    public SimpleSchemaCleanerConfig setResetSequences(Boolean resetSequences) {
        this.resetSequences = resetSequences;
        return this;
    }

    @Override
    public String getSchemaName() {
        return schemaName;
    }

    @Override
    public Set<String> shouldBeTruncated() {
        return truncate;
    }

    @Override
    public Set<String> shouldNotBeIgnored() {
        return ignore;
    }

    @Override
    public Boolean resetSequences() {
        return resetSequences;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }


    public static SchemaCleanerConfig merge(SchemaCleanerConfig first, SchemaCleanerConfig second) {
        Preconditions.checkArgument(
            oneIsNullOrBothEquals(first, second),
            "Configs %s andSchema %s has different reset sequence value",
            first,
            second
        );

        return new SimpleSchemaCleanerConfigBuilder()
            .setSchemaName(first.getSchemaName())
            .setTruncate(Sets.union(first.shouldBeTruncated(), second.shouldBeTruncated()))
            .setIgnore(Sets.union(first.shouldNotBeIgnored(), second.shouldNotBeIgnored()))
            .setResetSequences(evaluateResetSequencesFlag(first, second))
            .build();
    }

    public static void validate(SchemaCleanerConfig config) {
        if (config.shouldBeTruncated().isEmpty() || config.shouldNotBeIgnored().isEmpty()) {
            return;
        }
        Sets.SetView<String> intersection = Sets.intersection(config.shouldBeTruncated(), config.shouldNotBeIgnored());
        if (!intersection.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                "Final Cleaner configuration for schema %s has conflicts. " +
                    "Tables %s should be truncated and should be not truncated simultaneously. Config name %s",
                config.getSchemaName(),
                intersection,
                config
            ));
        }
    }

    private static boolean oneIsNullOrBothEquals(SchemaCleanerConfig first, SchemaCleanerConfig second) {
        return (first.resetSequences() == null || second.resetSequences() == null)
            || Stream.of(first, second).filter(e -> !e.isDefault())
            .map(SchemaCleanerConfig::resetSequences).distinct().count() <= 1;
    }

    private static Boolean evaluateResetSequencesFlag(SchemaCleanerConfig first, SchemaCleanerConfig second) {
        return Stream.of(first.resetSequences(), second.resetSequences())
            .filter(Objects::nonNull).findFirst().orElse(null);
    }

}
