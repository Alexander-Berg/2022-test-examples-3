package ru.yandex.market.common.test.db.ddl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.NotImplementedException;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.ext.oracle.OracleConnection;

public class OracleDDLHelper extends DDLHelper {
    private static final String FIND_TABLE_CONSTRAINTS = "" +
            "SELECT CONSTRAINT_NAME, TABLE_NAME " +
            "FROM ALL_CONSTRAINTS " +
            "WHERE OWNER = :1 AND STATUS = :2 AND CONSTRAINT_TYPE = :3";

    @Override
    public void truncateTables(Connection connection, Set<String> tables) throws SQLException {
        disableConstraints(connection);
        for (String table : tables) {
            executeStatement(connection, "TRUNCATE TABLE " + table);
        }
        enableConstraints(connection);
    }

    @Override
    void refreshMatViews(Connection connection, Set<String> matViews) throws SQLException {
        throw new NotImplementedException("Oracle refreshMatViews is not implemented");
    }

    private void disableConstraints(Connection connection) throws SQLException {
        String schema = connection.getSchema();
        findTableConstraintsByStatus(connection, schema, TableConstraintStatus.ENABLED)
                .forEach((table, constraints) ->
                        switchTableConstraints(connection, TableConstraintAction.DISABLE, schema, table, constraints)
                );
    }

    private void enableConstraints(Connection connection) throws SQLException {
        String schema = connection.getSchema();
        findTableConstraintsByStatus(connection, schema, TableConstraintStatus.DISABLED)
                .forEach((table, constraints) ->
                        switchTableConstraints(connection, TableConstraintAction.ENABLE, schema, table, constraints)
                );
    }

    @Override
    public Set<String> getSequencesToRestart(Connection connection) throws SQLException {
        String schema = connection.getSchema();
        Set<String> sequences = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT SEQUENCE_NAME FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER = :1"
        )) {
            statement.setString(1, schema);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String table = rs.getString("SEQUENCE_NAME");
                sequences.add(String.format("%s.%s", schema, table));
            }
        }
        return sequences;
    }

    @Override
    public void restartSequences(Connection connection, Set<String> sequences) throws SQLException {
        for (String sequence : sequences) {
            executeStatement(connection, String.format("ALTER SEQUENCE %s RESTART START WITH 1", sequence));
        }
    }

    @Override
    public IDatabaseConnection getConnection(
            DataSource dataSource,
            String schema
    ) throws DatabaseUnitException, SQLException {
        return new OracleConnection(dataSource.getConnection(), schema);
    }

    @Override
    public Set<String> getTablesToTruncate(Connection connection) throws SQLException {
        String schema = connection.getSchema();
        Set<String> tables = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER = :1"
        )) {
            statement.setString(1, schema);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String table = rs.getString("TABLE_NAME");
                tables.add(String.format("%s.%s", schema, table));
            }
        }
        return tables;
    }

    @Override
    Set<String> getMatViewsToRefresh(Connection connection) throws SQLException {
        throw new NotImplementedException("Oracle getMatViewsToRefresh is not implemented");
    }

    private Map<String, List<String>> findTableConstraintsByStatus(Connection c,
                                                                   String schema,
                                                                   TableConstraintStatus status) throws SQLException {
        try (PreparedStatement selectConstraints = c.prepareStatement(FIND_TABLE_CONSTRAINTS)) {
            selectConstraints.setString(1, schema);
            selectConstraints.setString(2, status.name());
            selectConstraints.setString(3, "R");
            return tableConstraintsMapper(selectConstraints.executeQuery());
        }
    }

    private Map<String, List<String>> tableConstraintsMapper(ResultSet rs) throws SQLException {
        Map<String, List<String>> tableConstraints = new HashMap<>();
        while (rs.next()) {
            String table = rs.getString("TABLE_NAME");
            String constraint = rs.getString("CONSTRAINT_NAME");
            tableConstraints.compute(table, (key, value) -> {
                if (value == null) {
                    return Lists.newArrayList(constraint);
                }
                value.add(constraint);
                return value;
            });
        }
        return tableConstraints;
    }

    private void switchTableConstraints(Connection c,
                                        TableConstraintAction action,
                                        String schema,
                                        String table,
                                        List<String> constraints) {
        for (String constraint : constraints) {
            try (Statement disableConstrains = c.createStatement()) {
                disableConstrains.execute(
                        String.format("ALTER TABLE %s.%s %s CONSTRAINT %s", schema, table, action.name(), constraint)
                );
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private enum TableConstraintStatus {
        ENABLED,
        DISABLED
    }

    private enum TableConstraintAction {
        ENABLE,
        DISABLE
    }

}
