package ru.yandex.market.common.test.db.ddl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import ru.yandex.market.common.test.db.DatabaseType;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class DDLHelperRegistry {
    private static final Map<DatabaseType, DDLHelper> HELPERS = ImmutableMap.<DatabaseType, DDLHelper>builder()
            .put(DatabaseType.H2, new H2DDLHelper())
            .put(DatabaseType.POSTGRES, new PostgreSQLDDLHelper())
            .put(DatabaseType.MYSQL, new MySQLDDLHelper())
            .put(DatabaseType.CLICKHOUSE, new ClickhouseDDLHelper())
            .put(DatabaseType.ORACLE, new OracleDDLHelper())
            .build();

    private DDLHelperRegistry() {
    }

    public static DDLHelper getHelper(Connection connection) throws SQLException {
        return getHelper(connection.getMetaData().getDatabaseProductName());
    }

    private static DDLHelper getHelper(String dbName) {
        return DatabaseType.of(dbName)
                .map(HELPERS::get)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported DB type " + dbName));
    }
}
