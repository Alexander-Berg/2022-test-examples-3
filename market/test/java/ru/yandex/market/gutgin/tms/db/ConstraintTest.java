package ru.yandex.market.gutgin.tms.db;

import org.junit.Test;
import ru.yandex.market.gutgin.tms.base.BaseDbGutGinTest;

import java.util.List;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class ConstraintTest extends BaseDbGutGinTest {
    @Test
    public void whenStartDbThenCheckPK() {
        List<String> tables = dsl().fetch(
            "select relname\n" +
                "from pg_class t \n" +
                "join pg_namespace n on n.oid = t.relnamespace\n" +
                "where relkind = 'r' and n.nspname = 'partner_content' and relname not in ('databasechangelog')\n" +
                "and not exists (\n" +
                "\tselect * \n" +
                "\tfrom pg_constraint c \n" +
                "\tjoin pg_namespace n on n.oid = c.connamespace\n" +
                "\twhere c.contype = 'p' and n.nspname = 'partner_content' and c.conrelid = t.oid\n" +
                ")")
            .getValues("relname", String.class);

        assertSoftly(softAssertions -> {
            for (String table : tables) {
                if (table.endsWith("_backup")) {
                    continue;
                }
                softAssertions.fail("Relation \"%s\" without PRIMARY KEY", table);
            }
        });
    }

    @Test
    public void whenStartDbThenCheckFKIndexesPresent() {
        List<AbsentIndexInfo> absentIndexes = dsl().fetch(
                "select c.conrelid::regclass as table_name,\n" +
                        "    string_agg(col.attname, ', ' order by u.attposition) as columns,\n" +
                        "    c.conname as constraint_name\n" +
                        "from pg_catalog.pg_constraint c\n" +
                        "         join lateral unnest(c.conkey) with ordinality as u(attnum, attposition) on true\n" +
                        "         join pg_catalog.pg_class t on (c.conrelid = t.oid)\n" +
                        "         join pg_catalog.pg_namespace nsp on nsp.oid = t.relnamespace\n" +
                        "         join pg_catalog.pg_attribute col on (col.attrelid = t.oid and col.attnum = u.attnum)\n" +
                        "where c.contype = 'f'\n" +
                        "  and nsp.nspname = 'partner_content'\n" +
                        "  and not exists(\n" +
                        "        select 1\n" +
                        "        from pg_catalog.pg_index pi\n" +
                        "        where pi.indrelid = c.conrelid\n" +
                        "          /* all columns of foreign key have to present in index: */\n" +
                        "          and (c.conkey::int[] <@ pi.indkey::int[]) \n" +
                        "          /* ordering of columns in foreign key and in index is the same: */\n" +
                        "          and array_position(pi.indkey::int[], (c.conkey::int[])[1]) = 0\n" +
                        "    )\n" +
                        "group by c.conrelid, c.conname, c.oid\n" +
                        "order by (c.conrelid::regclass)::text, columns;")
                .map(record -> {
                    String tableName = record.get("table_name", String.class);
                    String columns = record.get("columns", String.class);
                    String constraintName = record.get("constraint_name", String.class);
                    return new AbsentIndexInfo(tableName, columns, constraintName);
                });

        assertSoftly(softAssertions -> {
            for (AbsentIndexInfo absentIndex : absentIndexes) {
                if (absentIndex.getTableName().endsWith("_backup")) {
                    continue;
                }
                softAssertions.fail("Foreign key '%s' (constraint_name = '%s') of relation '%s' doesn't have index",
                        absentIndex.getColumns(), absentIndex.getConstraintName(), absentIndex.getTableName());
            }
        });
    }

    private static class AbsentIndexInfo {
        private String tableName;
        private String columns;
        private String constraintName;

        public AbsentIndexInfo(String tableName, String columns, String constraintName) {
            this.tableName = tableName;
            this.columns = columns;
            this.constraintName = constraintName;
        }

        public String getTableName() {
            return tableName;
        }

        public String getColumns() {
            return columns;
        }

        public String getConstraintName() {
            return constraintName;
        }
    }

}
