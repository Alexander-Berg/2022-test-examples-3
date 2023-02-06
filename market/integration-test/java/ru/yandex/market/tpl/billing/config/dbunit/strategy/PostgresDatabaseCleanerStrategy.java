package ru.yandex.market.tpl.billing.config.dbunit.strategy;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PostgresDatabaseCleanerStrategy implements DatabaseCleanerStrategy {

    private static final Set<String> METADATA_SCHEMAS = Set.of("information_schema", "pg_catalog");

    @Override
    public Set<String> getMetaSchemas() {
        return METADATA_SCHEMAS;
    }

    @Override
    public String getSelectSchemasSQL() {
        return "select schema_name from information_schema.schemata";
    }

    @Override
    public String getFindTablesSQL(String schemaName) {
        return String.format(
            "select table_name from information_schema.tables where table_schema='%s' and  table_type <> 'VIEW';",
            schemaName
        );
    }

    @Override
    public String getTruncateTableSQL(String schemaName, String tableName) {
        return String.format("truncate table %s.%s cascade;", schemaName, tableName);
    }

    @Override
    public Optional<String> getBatchTruncateTablesSQL(String schemaName, Collection<String> tableNames) {
        return Optional.of(
            tableNames.stream()
                .map(tableName -> schemaName + "." + tableName)
                .collect(Collectors.joining(", ", "truncate table ", " cascade;"))
        );
    }

    @Override
    public String getFindSequencesSQL(String schemaName) {
        return String.format(
            "select sequence_name from information_schema.sequences where sequence_schema='%s';",
            schemaName
        );

    }

    @Override
    public String getResetSequenceSQL(String schemaName, String sequenceName) {
        return String.format(
            "alter sequence %s.%s restart with 1;",
            schemaName,
            sequenceName
        );
    }
}
