package ru.yandex.market.common.test.db.ddl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;

import javax.sql.DataSource;

import org.dbunit.DatabaseUnitException;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.ext.mysql.MySqlConnection;
import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.Validate;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class MySQLDDLHelper extends DDLHelper {
    @Override
    Set<String> getMatViewsToRefresh(Connection connection) throws SQLException {
        return Collections.emptySet();
    }

    @Override
    public void truncateTables(Connection connection, Set<String> tables) throws SQLException {
        executeStatement(connection, "SET FOREIGN_KEY_CHECKS = 0");
        for (String table : tables) {
            executeStatement(connection, "TRUNCATE TABLE " + table);
        }
        executeStatement(connection, "SET FOREIGN_KEY_CHECKS = 0");
    }

    @Override
    void refreshMatViews(Connection connection, Set<String> matViews) throws SQLException {

    }

    @Override
    public Set<String> getSequencesToRestart(Connection connection) {
        return Collections.emptySet();
    }

    @Override
    public void restartSequences(Connection connection, Set<String> sequences) throws SQLException {
    }

    @Validate
    public IDatabaseConnection getConnection(
            DataSource dataSource,
            String schema
    ) throws DatabaseUnitException, SQLException {
        return new MySqlConnection(dataSource.getConnection(), schema);
    }
}
