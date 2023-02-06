package ru.yandex.market.checkout.checkouter.database;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;

public class ColumnCommentExistsTest extends AbstractServicesTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(ColumnCommentExistsTest.class);
    @Value("${market.checkouter.oms.service.tms.prefix}")
    private String tmsPrefix;

    @Test
    public void commentShouldExistsForEveryColumn() {
        //Внимание: Локально тест всегда зеленый
        List<ColumnDefinition> columnDefinitions = masterJdbcTemplate.query(
                "select cols.table_name, cols.column_name, pg_catalog.col_description(c.oid, cols" +
                        ".ordinal_position::int) " +
                        "from information_schema.columns cols " +
                        "     join pg_catalog.pg_class c on c.oid = cols.table_name::regclass::oid and c.relname = " +
                        "cols.table_name " +
                        "where cols.table_schema = 'public' " +
                        "       and coalesce(pg_catalog.col_description(c.oid, cols.ordinal_position::int), '') = " +
                        "'' " +
                        "       and cols.table_name not in ('databasechangelog', 'databasechangeloglock') " +
                        "       and cols.table_name not like 'pg_%' " +
                        "       and cols.table_name not like '" + tmsPrefix + "_%' " +
                        "and c.relkind = 'r' " +
                        "order by cols.table_name, ordinal_position",
                (rs, index) -> new ColumnDefinition(rs.getString(1), rs.getString(2))
        );

        if (!columnDefinitions.isEmpty()) {
            columnDefinitions.forEach(cd -> {
                LOG.error("No comment for column {}.{}", cd.getTableName(), cd.getColumnName());
            });

            Assertions.fail("Found " + columnDefinitions.size() + " columns without comment");
        }
    }

    static class ColumnDefinition {

        private String tableName;
        private String columnName;

        ColumnDefinition(String tableName, String columnName) {
            this.tableName = tableName;
            this.columnName = columnName;
        }

        public String getTableName() {
            return tableName;
        }

        public String getColumnName() {
            return columnName;
        }
    }
}
