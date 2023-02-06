package ru.yandex.market.common.test.db.ddl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.google.common.collect.ImmutableSet;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.operation.CompositeOperation;
import org.dbunit.operation.DatabaseOperation;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;

/**
 * @author antipov93.
 */
public class ClickhouseDDLHelper extends DDLHelper {

    /**
     * Tables in database "system" can not be truncated in Clickhouse.
     */
    private static final Set<String> EXCLUDED_DATABASES = ImmutableSet.of("system");

    @Override
    public DDLDatabaseTester makeTester(DataSource dataSource, String schema, DbUnitDataBaseConfig config) {
        DDLDatabaseTester tester = new DDLDatabaseTester(dataSource, schema, config, this);
        tester.setSetUpOperation(new CompositeOperation(
                DatabaseOperation.TRUNCATE_TABLE, // Clickhouse does not support DELETE operation
                DatabaseOperation.INSERT
        ));
        return tester;
    }

    @Override
    public void truncateTables(Connection connection, Set<String> tables) throws SQLException {
        for (String table : tables) {
            executeStatement(connection, "TRUNCATE TABLE " + table);
        }
    }

    @Override
    void refreshMatViews(Connection connection, Set<String> matViews) throws SQLException {

    }

    @Override
    public Set<String> getTablesToTruncate(Connection connection) throws SQLException {
        Set<String> databases = getDatabases(connection);
        Set<String> result = new HashSet<>();
        for (String database : databases) {
            Set<String> databaseTables = getDatabaseTables(database, connection);
            List<String> tablesWithDatabase = databaseTables.stream()
                    .map(databaseTable -> database + "." + databaseTable)
                    .collect(Collectors.toList());
            result.addAll(tablesWithDatabase);
        }
        return result;
    }

    @Override
    Set<String> getMatViewsToRefresh(Connection connection) throws SQLException {
        return Collections.emptySet();
    }

    @Override
    public void restartSequences(Connection connection, Set<String> sequences) {
    }

    @Override
    public Set<String> getSequencesToRestart(Connection connection) {
        return Collections.emptySet();
    }

    @Override
    public IDatabaseConnection getConnection(
            DataSource dataSource,
            String schema
    ) throws DatabaseUnitException, SQLException {
        return new DatabaseDataSourceConnection(dataSource);
    }

    private Set<String> getDatabaseTables(String database, Connection c) throws SQLException {
        Set<String> tables = new HashSet<>();
        try (ResultSet rs = c.createStatement().executeQuery("SHOW TABLES FROM " + database)) {
            while (rs.next()) {
                String dbName = rs.getString("name");
                tables.add(dbName);
            }
        }
        return tables;
    }

    private Set<String> getDatabases(Connection c) throws SQLException {
        Set<String> databases = new HashSet<>();
        try (ResultSet rs = c.createStatement().executeQuery("SHOW DATABASES")) {
            while (rs.next()) {
                String dbName = rs.getString("name");
                databases.add(dbName);
            }
        }
        return databases.stream()
                .filter(database -> !EXCLUDED_DATABASES.contains(database))
                .collect(Collectors.toSet());
    }
}
