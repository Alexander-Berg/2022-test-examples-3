package ru.yandex.market.common.test.db.ddl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;

import javax.sql.DataSource;

import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.ext.h2.H2Connection;
import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.Validate;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class H2DDLHelper extends DDLHelper {

    @Override
    public Set<String> getTablesToTruncate(Connection connection) throws SQLException {
        return getObjectNames(connection.createStatement(),
                "select table_schema, table_name from information_schema.tables " +
                        "where table_schema != 'INFORMATION_SCHEMA' and table_type = 'TABLE' " +
                        "and row_count_estimate > 0");
    }

    @Override
    Set<String> getMatViewsToRefresh(Connection connection) throws SQLException {
        return Collections.emptySet();
    }

    @Override
    public void truncateTables(Connection connection, Set<String> tables) throws SQLException {
        executeStatement(connection, "set referential_integrity false");
        for (String table : tables) {
            executeStatement(connection, "truncate table " + table);
        }
        executeStatement(connection, "set referential_integrity true");
    }

    @Override
    void refreshMatViews(Connection connection, Set<String> matViews) throws SQLException {

    }

    @Override
    public Set<String> getSequencesToRestart(Connection connection) throws SQLException {
        //language=sql
        return getObjectNames(connection.createStatement(),
                "select sequence_schema, sequence_name from information_schema.sequences " +
                        "where sequence_schema != 'INFORMATION_SCHEMA' and current_value > 0");

    }

    @Override
    public void restartSequences(Connection connection, Set<String> sequences) throws SQLException {
        for (String sequence : sequences) {
            executeStatement(connection, "alter sequence " + sequence + " restart with 1");
        }
    }

    @Validate
    public IDatabaseConnection getConnection(
            DataSource dataSource,
            String schema
    ) throws DatabaseUnitException, SQLException {
        IDatabaseConnection dbConnection = new H2Connection(dataSource.getConnection(), schema);
        dbConnection.getConfig().setProperty(
                DatabaseConfig.PROPERTY_ESCAPE_PATTERN,
                "\""
        );
        return dbConnection;
    }
}
