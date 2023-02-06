package ru.yandex.market.common.test.db.ddl;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.StringJoiner;

import javax.sql.DataSource;

import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.ext.mysql.MySqlMetadataHandler;

import ru.yandex.market.common.test.db.ddl.datatype.CustomPostgresqlDataTypeFactory;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class PostgreSQLDDLHelper extends DDLHelper {
    @Override
    Set<String> getTablesToTruncate(Connection connection) throws SQLException {
        // https://www.postgresql.org/docs/10/view-pg-tables.html
        // https://www.postgresql.org/docs/10/catalog-pg-class.html
        // https://wiki.postgresql.org/wiki/Count_estimate
        // https://jdbc.postgresql.org/documentation/head/callproc.html
        try (Statement s = connection.createStatement()) {
            s.execute("create or replace function pg_temp.PostgreSQLDDLHelper_anyTableRow" +
                    "(schema text, name text, out result int) as $$" +
                    "begin" +
                    "   execute format('select 1 from %I.%I limit 1', schema, name) into result;" +
                    "end;" +
                    "$$ language plpgsql strict stable");

            return getObjectNames(s, "select n.nspname, c.relname" +
                    " from pg_namespace n" +
                    "   join pg_class c on n.oid = c.relnamespace" +
                    " where c.relkind in ('r', 'p')" + // regular table, partitioned table
                    "   and n.nspname not in ('pg_catalog', 'information_schema')" +
                    "   and pg_temp.PostgreSQLDDLHelper_anyTableRow(n.nspname, c.relname) is not null");
        }
    }

    @Override
    Set<String> getMatViewsToRefresh(Connection connection) throws SQLException {
        try (Statement s = connection.createStatement()) {
            return getObjectNames(s, "select n.nspname, c.relname" +
                    " from pg_namespace n" +
                    "   join pg_class c on n.oid = c.relnamespace" +
                    " where c.relkind in ('m')" + // mat view
                    "   and n.nspname not in ('pg_catalog', 'information_schema')");
        }
    }

    @Override
    void truncateTables(Connection connection, Set<String> tables) throws SQLException {
        // зовем restart identity для безымянных последовательностей
        StringJoiner sql = new StringJoiner(",", "truncate table ", " restart identity cascade");
        tables.forEach(sql::add);
        executeStatement(connection, sql.toString());
    }

    @Override
    void refreshMatViews(Connection connection, Set<String> matViews) throws SQLException {
        for (String matView : matViews) {
            executeStatement(connection, "REFRESH MATERIALIZED VIEW " + matView);
        }
    }

    @Override
    Set<String> getSequencesToRestart(Connection connection) throws SQLException {
        // https://www.postgresql.org/docs/10/view-pg-sequences.html
        // https://www.postgresql.org/docs/10/catalog-pg-class.html
        try (Statement s = connection.createStatement()) {
            return getObjectNames(s, "select n.nspname, c.relname" +
                    " from pg_namespace n" +
                    "   join pg_class c on n.oid = c.relnamespace" +
                    " where c.relkind = 'S'" + // sequence
                    "   and n.nspname not in ('pg_catalog', 'information_schema')");
        }
    }

    @Override
    void restartSequences(Connection connection, Set<String> sequences) throws SQLException {
        // https://www.postgresql.org/docs/12/sql-altersequence.html
        // alter sequence S restart транзакционный, юзаем setval который нет
        try (PreparedStatement s = connection.prepareStatement("select setval(?, 1, false)")) {
            for (String sequence : sequences) {
                s.setString(1, sequence);
                s.execute();
            }
        }
    }

    @Override
    IDatabaseConnection getConnection(
            DataSource dataSource,
            String schema
    ) throws SQLException {
        IDatabaseConnection dbConnection = new DatabaseDataSourceConnection(dataSource, schema);
        dbConnection.getConfig().setProperty(
                DatabaseConfig.PROPERTY_DATATYPE_FACTORY,
                new CustomPostgresqlDataTypeFactory()
        );
        dbConnection.getConfig().setProperty(
                DatabaseConfig.PROPERTY_METADATA_HANDLER,
                new PgMetadataHandler()
        );
        dbConnection.getConfig().setProperty(
                DatabaseConfig.PROPERTY_ESCAPE_PATTERN,
                "\""
        );
        dbConnection.getConfig().setProperty(
                DatabaseConfig.PROPERTY_TABLE_TYPE,
                new String[] {"TABLE", "PARTITIONED TABLE"}
        );
        return dbConnection;
    }

    // case-sensitivity workaround, https://stackoverflow.com/a/20944167
    private static class PgMetadataHandler extends MySqlMetadataHandler {
        @Override
        public ResultSet getTables(
                DatabaseMetaData metaData,
                String schemaName,
                String[] tableType
        ) throws SQLException {
            return metaData.getTables(null, schemaName.toLowerCase(), "%", tableType);
        }
    }
}

