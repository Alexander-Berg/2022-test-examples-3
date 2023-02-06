package ru.yandex.market.delivery.tracker.configuration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

class DataSetProcessor {

    private static final String TRUNCATE_TABLES_SQL = "truncate table delivery_tracker.%s restart identity cascade;";
    private static final String FIND_TABLES_SQL = "" +
        "select table_name from information_schema.tables " +
        "where table_schema='delivery_tracker' and table_type <> 'VIEW';";

    private Set<String> ignoreTables;

    DataSetProcessor(final Set<String> ignoreTables) {
        this.ignoreTables = ignoreTables;
    }

    void truncateAllTables(final DataSource dataSource) throws SQLException {
        clean(dataSource, FIND_TABLES_SQL, TRUNCATE_TABLES_SQL);
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
