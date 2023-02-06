package ru.yandex.market.ff.base;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

/**
 * Позволяет производить фиксацию БД для корректного выполнения функциональных тестов.
 *
 * @author avetokhin 11/01/18.
 */
class DataSetProcessor {

    private static final String TRUNCATE_TABLES_SQL = "truncate table public.%s cascade;";
    private static final String FIND_TABLES_SQL =
            "select table_name from information_schema.tables where table_schema='public' and table_type='BASE TABLE';";
    private static final String FIND_SEQUENCES_SQL =
            "select sequence_name from information_schema.sequences where sequence_schema='public';";
    private static final String RESET_SEQUENCES_SQL = "alter sequence %s restart;";

    private Set<String> ignoreTables;

    DataSetProcessor(final Set<String> ignoreTables) {
        this.ignoreTables = ignoreTables;
    }

    void truncateAllTables(final DataSource dataSource) throws SQLException {
        clean(dataSource, FIND_TABLES_SQL, TRUNCATE_TABLES_SQL);
    }

    void resetAllSequences(final DataSource dataSource) throws SQLException {
        clean(dataSource, FIND_SEQUENCES_SQL, RESET_SEQUENCES_SQL);
    }

    private void clean(final DataSource dataSource, final String findSql, final String cleanSql) throws SQLException {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {

            Set<String> objects = getObjects(s, findSql);

            for (String table : objects) {
                s.execute(String.format(cleanSql, table));
            }
        }
    }

    private Set<String> getObjects(Statement s, String sql) throws SQLException {
        Set<String> objects = new HashSet<>();
        try (ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString(1);
                if (!ignoreTables.contains(name)) {
                    objects.add(name);
                }
            }
        }
        return objects;
    }

}
