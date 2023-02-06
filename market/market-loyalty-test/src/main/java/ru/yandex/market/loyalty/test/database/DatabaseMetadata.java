package ru.yandex.market.loyalty.test.database;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.jdbc.core.JdbcTemplate;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

public class DatabaseMetadata {
    private static final String GET_INDICES =
            "select " +
                    "c.relname, " +
                    "i.indisunique," +
                    "i.indisprimary, " +
                    "ARRAY( " +
                    "select t.attname " +
                    "from (select row_number() over () rn, k as key from unnest(i.indkey) k) keys " +
                    "left join pg_attribute t on keys.key = t.attnum " +
                    "where t.attrelid = c.oid " +
                    "order by keys.rn " +
                    ") as columns, " +
                    "pg_get_expr(i.indexprs, i.indrelid) as index_expression " +
                    "from pg_class c inner join pg_index i on c.oid = i.indrelid";
    private static final String GET_TABLE_COLUMNS =
            "SELECT " +
                    "t.tablename, " +
                    "a.attname " +
                    "FROM " +
                    "pg_class c, " +
                    "pg_attribute a, " +
                    "pg_tables t " +
                    "WHERE " +
                    "a.attrelid=c.oid AND " +
                    "c.relname=t.tablename";

    private JdbcTemplate jdbcTemplate;
    private Map<String, List<TableIndex>> indices;
    private Map<String, List<String>> columnDict;

    public DatabaseMetadata(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @VisibleForTesting
    DatabaseMetadata(Map<String, List<TableIndex>> indices, Map<String, List<String>> columnDict) {
        this.indices = indices;
        this.columnDict = columnDict;
    }

    void load() {
        if (!isLoaded()) {
            indices = jdbcTemplate.query(GET_INDICES, (rs, i) -> {
                Array columns = rs.getArray("columns");
                try {
                    return new TableIndex(
                            rs.getString("relname"),
                            rs.getBoolean("indisunique"),
                            rs.getBoolean("indisprimary"),
                            (String[]) columns.getArray(),
                            rs.getString("index_expression")
                    );
                } finally {
                    columns.free();
                }
            })
                    .stream()
                    .collect(Collectors.groupingBy(TableIndex::getTableName));
            columnDict = jdbcTemplate.query(
                    GET_TABLE_COLUMNS,
                    (rs, i) -> Pair.of(rs.getString("tablename"), rs.getString("attname"))
            ).stream()
                    .collect(groupingBy(Pair::getKey, mapping(Pair::getValue, toList())));
        }
    }

    boolean isIndexed(Column column, Table table, Predicate<Integer> testIndexColumnNum) {
        String name;
        if (table != null && hasIndices(name = table.getName().toLowerCase())) {
            List<TableIndex> tableIndices = getIndices(name);
            return tableIndices.stream()
                    .anyMatch(i -> testIndexColumnNum.test(i.getColumnPosition(column.getColumnName())));
        }

        return false;
    }

    boolean isIndexed(List<Column> columns, Table table) {
        String name;
        if (table != null && hasIndices(name = table.getName().toLowerCase())) {
            List<TableIndex> tableIndices = getIndices(name);
            return tableIndices.stream()
                    .anyMatch(i -> columns.stream().anyMatch(c -> i.getColumnPosition(c.getColumnName()) == 0));
        }

        return false;
    }

    private List<TableIndex> getIndices(String name) {
        return indices.get(name.toLowerCase());
    }

    private boolean hasIndices(String name) {
        return indices.containsKey(name.toLowerCase());
    }

    boolean hasColumn(String name, String columnName) {
        if (columnDict.containsKey(name.toLowerCase())) {
            return columnDict.get(name.toLowerCase()).contains(columnName.toLowerCase());
        }
        return false;
    }

    @VisibleForTesting
    void addIndex(TableIndex tableIndex) {
        if (indices == null) {
            throw new IllegalStateException();
        }
        String tableName = tableIndex.getTableName().toLowerCase();
        List<TableIndex> tableIndices = indices.computeIfAbsent(tableName, k -> new ArrayList<>());
        tableIndices.add(tableIndex);
    }

    boolean isLoaded() {
        return indices != null && columnDict != null;
    }
}
