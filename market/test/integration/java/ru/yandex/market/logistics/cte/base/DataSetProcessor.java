package ru.yandex.market.logistics.cte.base;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

class DataSetProcessor {

    private static final String TRUNCATE_TABLES_SQL = "TRUNCATE TABLE %s CASCADE;";
    private static final String TRUNCATE_DBQUEUE_TABLES_SQL = "TRUNCATE TABLE dbqueue.%s CASCADE;";
    private static final String FIND_TABLES_SQL =
            "SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE';";
    private static final String FIND_DBQUEUE_TABLES_SQL =
            "SELECT table_name FROM information_schema.tables WHERE table_schema='dbqueue' " +
                    "AND table_type='BASE TABLE';";
    private static final String FIND_SEQUENCES_SQL =
            "SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema='public';";
    private static final String FIND_DBQUEUE_SEQUENCES_SQL =
            "SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema='dbqueue';";
    private static final String RESET_SEQUENCES_SQL = "ALTER SEQUENCE %s RESTART WITH 1;";
    private static final String RESET_DBQUEUE_SEQUENCES_SQL = "ALTER SEQUENCE dbqueue.%s RESTART WITH 1;";

    private Set<String> ignoreTables;

    DataSetProcessor(final Set<String> ignoreTables) {
        this.ignoreTables = ignoreTables;
    }

    void truncateAllTables(DataSource dataSource) throws SQLException {
        clean(dataSource, FIND_TABLES_SQL, TRUNCATE_TABLES_SQL);
        clean(dataSource, FIND_DBQUEUE_TABLES_SQL, TRUNCATE_DBQUEUE_TABLES_SQL);
    }

    void resetAllSequences(DataSource dataSource) throws SQLException {
        clean(dataSource, FIND_SEQUENCES_SQL, RESET_SEQUENCES_SQL);
        clean(dataSource, FIND_DBQUEUE_SEQUENCES_SQL, RESET_DBQUEUE_SEQUENCES_SQL);
    }

    private void clean(DataSource dataSource, String findSql, String cleanSql) throws SQLException {
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement()) {

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
