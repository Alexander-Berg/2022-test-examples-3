package ru.yandex.market.checkout.checkouter.monitoring.db.maintenance;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DatabaseStructureHealthTest extends AbstractServicesTestBase {

    private static final String COLUMNS_WITH_NUMERIC_20 =
            "select table_name, c.column_name, c.numeric_scale\n" +
                    "from pg_catalog.pg_statio_all_tables as st\n" +
                    "         inner join pg_catalog.pg_description pgd on (pgd.objoid=st.relid)\n" +
                    "         right outer join information_schema.columns c\n" +
                    "             on (pgd.objsubid=c.ordinal_position and  c.table_schema=st.schemaname " +
                    "and c.table_name=st.relname)\n" +
                    "where table_schema = 'public' and\n" +
                    "      c.data_type = 'numeric' and c.numeric_precision = 20;";

    @DisplayName("Тест на наличие в БД столбцов с типом numeric(20). Такие столбцы добавлять нельзя!!!")
    @Test
    public void newColumnsWithNumeric20AreProhibited() {
        final List<ColumnInfo> columnInfo = masterJdbcTemplate.query(COLUMNS_WITH_NUMERIC_20,
                (rs, rn) -> new ColumnInfo(rs.getString("table_name"), rs.getString("column_name")));
        assertNotNull(columnInfo);
        assertEquals(5, columnInfo.size());
        final Set<String> tables = columnInfo.stream()
                .map(c -> c.tableName)
                .collect(Collectors.toSet());
        assertEquals(3, tables.size(), "Don't use numeric(20)! Use bigint instead");
        assertThat(tables, containsInAnyOrder("delivery_track", "delivery_track_checkpoint",
                "delivery_track_checkpoint_history"));
        final Set<String> columns = columnInfo.stream()
                .map(c -> c.columnName)
                .collect(Collectors.toSet());
        assertEquals(3, columns.size());
        assertThat(columns, containsInAnyOrder("tracker_id", "delivery_track_id", "id"));
    }

    private static class ColumnInfo {

        private final String tableName;
        private final String columnName;

        private ColumnInfo(String tableName, String columnName) {
            this.tableName = tableName;
            this.columnName = columnName;
        }

        @Override
        public String toString() {
            return ColumnInfo.class.getSimpleName() + "{" +
                    "tableName=" + tableName +
                    ", columnName=" + columnName +
                    "}";
        }
    }
}
