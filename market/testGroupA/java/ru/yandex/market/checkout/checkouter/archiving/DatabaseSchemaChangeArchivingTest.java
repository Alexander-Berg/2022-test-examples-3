package ru.yandex.market.checkout.checkouter.archiving;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jooq.Table;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.storage.util.ArchivedTableUtils;
import ru.yandex.market.checkout.util.DatabaseUtils;
import ru.yandex.market.checkouter.jooq.Tables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.application.AbstractArchiveWebTestBase.ARCHIVING_TABLES;
import static ru.yandex.market.checkout.checkouter.storage.util.ArchivedTableUtils.TRACKED_ARCHIVING_TABLES;

public class DatabaseSchemaChangeArchivingTest extends AbstractServicesTestBase {

    private static final int NOT_ARCHIVING_TABLES_COUNT = 29;
    private static final String MESSAGE_PATTERN = "Table %s does not included in archiving process! " +
            "Please, make sure that all new reference tables are added to OrderCopyingDao and OrderDeletingDao";

    @Test
    void foreignKeysConstraintTest() {
        Set<Table<?>> foreignTables = DatabaseUtils.getForeignKeys(masterJdbcTemplate, ARCHIVING_TABLES)
                .stream()
                .map(DatabaseUtils.ForeignKeyInfo::getReferencedTable)
                .collect(Collectors.toSet());

        foreignTables.forEach(table ->
                assertTrue(ARCHIVING_TABLES.contains(table), String.format(MESSAGE_PATTERN, table.getName()))
        );
    }

    @Test
    void archivingTablesTest() {
        int tablesCount = Tables.class.getFields().length;
        int knownArchivingTablesCount = ARCHIVING_TABLES.size() + NOT_ARCHIVING_TABLES_COUNT;

        assertTrue(
                tablesCount == knownArchivingTablesCount || tablesCount < knownArchivingTablesCount,
                "Please, check, whether all new tables need to be archived"
        );
    }

    /**
     * Для каждой таблицы из {@link ArchivedTableUtils#TRACKED_ARCHIVING_TABLES}
     * нужна соответствующая archived_{table}. См. checkouter-db/src/script/changelog/basic/archived_*.sql
     */
    @Test
    void archivedTablesTest() {
        List<String> expectedTables = TRACKED_ARCHIVING_TABLES.stream()
                .map(table -> ArchivedTableUtils.archivedTable(table).getName())
                .collect(Collectors.toList());
        List<String> actualTables = masterJdbcTemplate.queryForList(
                "select table_name" +
                        " from information_schema.tables" +
                        " where table_schema = 'public'" +
                        " and table_name like 'archived\\_%' escape '\\'",
                String.class);
        assertThat(actualTables).containsExactlyInAnyOrderElementsOf(expectedTables);
    }

    @Test
    void archivedTablePrimaryKeysTest() {
        TRACKED_ARCHIVING_TABLES.forEach(table -> {
            assertThat(table).isIn(ARCHIVING_TABLES);
            assertDoesNotThrow(() -> ArchivedTableUtils.getArchivingTableIdField(table));
        });
    }
}
