import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.ir.uee.config.LocalPgInitializer;
import ru.yandex.market.ir.uee.config.SqlDatasourceConfig;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = {LocalPgInitializer.class})
@Import({
        SqlDatasourceConfig.class,
        LiquibaseAutoConfiguration.class
})
@PropertySource(value = "application.properties")
public class DBTest {

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @Test
    public void whenStartDbThenCheckPK() {
        List<String> tables = new ArrayList<>();
        namedJdbcTemplate.query("select relname\n" +
                        "                from pg_class t\n" +
                        "                join pg_namespace n on n.oid = t.relnamespace\n" +
                        "                where relkind = 'r'\n" +
                        "                and n.nspname = 'uee'\n" +
                        "                and relname not in\n" +
                        "                        ('databasechangelog')\n" +
                        "                and not exists(\n" +
                        "                        select *\n" +
                        "                                from pg_constraint c\n" +
                        "                        join pg_namespace n on n.oid = c.connamespace\n" +
                        "                        where c.contype = 'p'\n" +
                        "                        and n.nspname = 'uee'\n" +
                        "                        and c.conrelid = t.oid);",
                rs -> {
                    String resname = rs.getString("relname");
                    tables.add(resname);
                });
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
        List<AbsentIndexInfo> absentIndexes = namedJdbcTemplate.query(
                "select c.conrelid::regclass as table_name,\n" +
                        "    string_agg(col.attname, ', ' order by u.attposition) as columns,\n" +
                        "    c.conname as constraint_name\n" +
                        "from pg_catalog.pg_constraint c\n" +
                        "         join lateral unnest(c.conkey) with ordinality as u(attnum, attposition) on true\n" +
                        "         join pg_catalog.pg_class t on (c.conrelid = t.oid)\n" +
                        "         join pg_catalog.pg_namespace nsp on nsp.oid = t.relnamespace\n" +
                        "         join pg_catalog.pg_attribute col on (col.attrelid = t.oid and col.attnum = u" +
                        ".attnum)\n" +
                        "where c.contype = 'f'\n" +
                        "  and nsp.nspname = 'uee'\n" +
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
                        "order by (c.conrelid::regclass)::text, columns;",
                (rs, rowNum) -> {
                    String tableName = rs.getString("table_name");
                    String columns = rs.getString("columns");
                    String constraintName = rs.getString("constraint_name");
                    return new AbsentIndexInfo(tableName, columns, constraintName);
                });

        assertSoftly(softAssertions -> {
            for (AbsentIndexInfo absentIndex : absentIndexes) {
                if (absentIndex.tableName.endsWith("_backup")) {
                    continue;
                }
                softAssertions.fail("Foreign key '%s' (constraint_name = '%s') of relation '%s' doesn't have index",
                        absentIndex.columns, absentIndex.constraintName, absentIndex.tableName);
            }
        });
    }

    private static class AbsentIndexInfo {
        private String tableName;
        private String columns;
        private String constraintName;

        private AbsentIndexInfo(String tableName, String columns, String constraintName) {
            this.tableName = tableName;
            this.columns = columns;
            this.constraintName = constraintName;
        }
    }
}
