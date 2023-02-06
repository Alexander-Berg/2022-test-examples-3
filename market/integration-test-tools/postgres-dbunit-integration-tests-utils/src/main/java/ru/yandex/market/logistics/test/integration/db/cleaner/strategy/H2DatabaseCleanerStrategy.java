package ru.yandex.market.logistics.test.integration.db.cleaner.strategy;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class H2DatabaseCleanerStrategy implements DatabaseCleanerStrategy {

    private static final Set<String> META_SCHEMAS = ImmutableSet.of("INFORMATION_SCHEMA");

    @Override
    public Set<String> getMetaSchemas() {
        return META_SCHEMAS;
    }

    @Override
    public String getSelectSchemasSQL() {
        return "SHOW SCHEMAS";
    }

    @Override
    public String getFindTablesSQL(String schemaName) {
        return String.format("SHOW TABLES FROM %s", schemaName);
    }

    @Override
    public String getTruncateTableSQL(String schemaName, String tableName) {
        return String.format("TRUNCATE TABLE %s.%s", schemaName, tableName);
    }

    @Override
    public String getFindSequencesSQL(String schemaName) {
        return String.format(
            "SELECT SEQUENCE_NAME FROM INFORMATION_SCHEMA.SEQUENCES WHERE SEQUENCE_SCHEMA = '%s'",
            schemaName
        );
    }

    @Override
    public String getResetSequenceSQL(String schemaName, String sequenceName) {
        return
            String.format("ALTER SEQUENCE %s.%s " +
                    "RESTART WITH ( " +
                    "SELECT MIN_VALUE " +
                    "FROM INFORMATION_SCHEMA.SEQUENCES " +
                    "WHERE SEQUENCE_SCHEMA = '%s' " +
                    "AND SEQUENCE_NAME = '%s')",
                schemaName,
                sequenceName,
                schemaName,
                sequenceName
            );
    }
}
