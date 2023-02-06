package ru.yandex.market.logistics.test.integration.db.cleaner.strategy;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface DatabaseCleanerStrategy {

    Set<String> getMetaSchemas();

    String getSelectSchemasSQL();

    String getFindTablesSQL(String schemaName);

    String getTruncateTableSQL(String schemaName, String tableName);

    default Optional<String> getBatchTruncateTablesSQL(String schemaName, Collection<String> tableNames) {
        return Optional.empty();
    }

    String getFindSequencesSQL(String schemaName);

    String getResetSequenceSQL(String schemaName, String sequenceName);
}
