package ru.yandex.chemodan.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.jdbc.core.ColumnMapRowMapper;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;
import ru.yandex.misc.test.Assert;

import static org.junit.Assert.assertTrue;

public class DatabaseValidationUtils {
    private static final Pattern COLUMNS_SEPARATOR = Pattern.compile(", ");

    public static void checkIndexesExistsForAllFK(JdbcTemplate3 shard, SetF<String> exclusion) {
        ListF<String> indexes = shard.queryForList(
                "select indexdef from pg_indexes where tablename not like 'pg%'",
                String.class
        );


        Pattern pattern = Pattern.compile("CREATE (UNIQUE | )?INDEX [\\S]+ ON ([\\S]+) USING [\\S]+ \\(([\\S ,]+)\\)");

        ListF<IndexDefinition> indexDefinitions = indexes
                .map(index -> {
                    Matcher matcher = pattern.matcher(index);
                    assertTrue("Index " + index + " not match pattern " + pattern.pattern(), matcher.find());
                    String table = matcher.group(2);
                    if (!table.contains(".")) {
                        table = "public." + table;
                    }
                    String columnDefinitions = matcher.group(3);
                    return new IndexDefinition(
                            table,
                            Cf.list(COLUMNS_SEPARATOR.split(columnDefinitions))
                                    .map(definition -> definition.split(" ")[0])
                    );
                });

        ListF<FkDefinition> foreignKeys = shard.query("SELECT DISTINCT" +
                        "       tc.constraint_name, " +
                        "       tc.table_schema || '.' || tc.table_name as table_name, " +
                        "       kcu.column_name " +
                        "FROM " +
                        "     information_schema.table_constraints AS tc " +
                        "       JOIN information_schema.key_column_usage AS kcu " +
                        "         ON tc.constraint_name = kcu.constraint_name " +
                        "              AND tc.table_schema = kcu.table_schema " +
                        "WHERE constraint_type = 'FOREIGN KEY'",
                new ColumnMapRowMapper()
        )
                .groupBy(row -> Tuple2.tuple((String) row.get("constraint_name"), (String) row.get("table_name")))
                .mapEntries((tuple, rows) -> new FkDefinition(
                        tuple.get1(),
                        tuple.get2(),
                        rows.map(row -> (String) row.get("column_name"))
                ));

        ListF<FkDefinition> missedIndexesForForeignKey = foreignKeys
                .filter(fk -> indexDefinitions.stream().noneMatch(index -> matches(fk, index)));

        Assert.assertEmpty(missedIndexesForForeignKey.filterNot(fk -> exclusion.containsTs(fk.getName())));
    }

    private static boolean matches(FkDefinition fk, IndexDefinition index)
    {
        return index.getTable().equals(fk.getRefToTable())
                && index.getColumns().size() >= fk.getRefToColumns().size()
                && index.getColumns().subList(0, fk.getRefToColumns().size()).unique()
                .equals(fk.getRefToColumns().unique());
    }

    @Getter
    @RequiredArgsConstructor
    private static class IndexDefinition {
        private final String table;
        private final ListF<String> columns;
    }

    @ToString
    @Getter
    @RequiredArgsConstructor
    private static class FkDefinition {
        private final String name;
        private final String refToTable;
        private final ListF<String> refToColumns;
    }
}
