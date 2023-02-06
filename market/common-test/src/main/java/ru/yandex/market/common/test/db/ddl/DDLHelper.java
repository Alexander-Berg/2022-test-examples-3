package ru.yandex.market.common.test.db.ddl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.dbunit.DatabaseUnitException;
import org.dbunit.database.IDatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public abstract class DDLHelper {
    private static final Logger log = LoggerFactory.getLogger(DDLHelper.class);

    private static final int FETCH_SIZE = 100;

    private static final String[] TABLE = {"TABLE"};
    private static final String TABLE_SCHEME = "TABLE_SCHEM";
    private static final String TABLE_NAME = "TABLE_NAME";

    public DDLDatabaseTester makeTester(DataSource dataSource, String schema, DbUnitDataBaseConfig config) {
        return new DDLDatabaseTester(dataSource, schema, config, this);
    }

    public void truncateAllTables(DataSource dataSource, Set<String> ignore) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            Set<String> tables = getTablesToTruncate(c);
            if (!tables.isEmpty()) {
                tables = filterIgnoreCase(tables, ignore);
                if (!tables.isEmpty()) {
                    try {
                        truncateTables(c, tables);
                    } finally {
                        c.commit();
                    }
                }
            }
        }
    }

    public void restartAllSequences(DataSource dataSource, Set<String> ignore) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            Set<String> sequences = getSequencesToRestart(c);
            if (!sequences.isEmpty()) {
                sequences = filterIgnoreCase(sequences, ignore);
                if (!sequences.isEmpty()) {
                    try {
                        restartSequences(c, sequences);
                    } finally {
                        c.commit();
                    }
                }
            }
        }
    }

    public void refreshAllMatViews(DataSource dataSource, Set<String> ignore) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            Set<String> tables = getMatViewsToRefresh(c);
            if (!tables.isEmpty()) {
                tables = filterIgnoreCase(tables, ignore);
                if (!tables.isEmpty()) {
                    try {
                        refreshMatViews(c, tables);
                    } finally {
                        c.commit();
                    }
                }
            }
        }
    }

    static Set<String> filterIgnoreCase(Set<String> data, Set<String> ignore) {
        Set<String> upperIgnore = ignore.stream().map(String::toUpperCase).collect(Collectors.toSet());
        return data.stream().filter(s -> !upperIgnore.contains(s.toUpperCase())).collect(Collectors.toSet());
    }

    Set<String> getTablesToTruncate(Connection connection) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getTables(null, null, null, TABLE)) {
            rs.setFetchSize(FETCH_SIZE);
            Set<String> tables = new HashSet<>();
            while (rs.next()) {
                String schema = rs.getString(TABLE_SCHEME);
                String table = rs.getString(TABLE_NAME);
                tables.add(schema == null
                        ? table
                        : schema + '.' + table);
            }
            return tables;
        }
    }

    abstract Set<String> getMatViewsToRefresh(Connection connection) throws SQLException;

    abstract void truncateTables(Connection connection, Set<String> tables) throws SQLException;

    abstract Set<String> getSequencesToRestart(Connection connection) throws SQLException;

    abstract void restartSequences(Connection connection, Set<String> sequences) throws SQLException;

    abstract void refreshMatViews(Connection connection, Set<String> matViews) throws SQLException;

    abstract IDatabaseConnection getConnection(
            DataSource dataSource,
            String schema
    ) throws DatabaseUnitException, SQLException;

    final Set<String> getObjectNames(Statement s, String sql) throws SQLException {
        try (ResultSet rs = s.executeQuery(sql)) {
            rs.setFetchSize(FETCH_SIZE);
            Set<String> objects = new HashSet<>();
            while (rs.next()) {
                String schema = rs.getString(1);
                String object = rs.getString(2);
                objects.add(schema == null
                        ? object
                        : schema + '.' + object);
            }
            return objects;
        }
    }

    final void executeStatement(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            log.debug("Executed statement: " + sql);
        }
    }
}
