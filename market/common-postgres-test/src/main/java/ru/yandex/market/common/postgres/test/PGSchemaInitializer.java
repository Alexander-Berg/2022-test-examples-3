package ru.yandex.market.common.postgres.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;

class PGSchemaInitializer {

    private final String defaultSchemaName;
    private volatile boolean schemaInited;

    PGSchemaInitializer(String defaultSchemaName) {
        this.defaultSchemaName = defaultSchemaName;
    }


    void initSchemaIfNecessary(Connection conn) throws SQLException {
        if (!schemaInited) {
            synchronized (PGEmbeddedDatasource.class) {
                if (!schemaInited) {
                    if (!isSchemaExist(conn, defaultSchemaName)) {
                        tryCreateSchema(conn, defaultSchemaName);
                    }
                    schemaInited = true;
                }
            }
        }
        conn.setSchema(defaultSchemaName);
    }

    private boolean isSchemaExist(Connection connection, String schemaName) throws SQLException {
        if (StringUtils.isEmpty(schemaName)) {
            return true;
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT count(*) FROM information_schema.schemata WHERE schema_name = ?")) {
            ps.setString(1, schemaName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int count = rs.getInt(1);
                return count != 0;
            }
        }
    }

    private void tryCreateSchema(Connection connection, String schemaName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("CREATE SCHEMA " + schemaName)) {
            ps.executeUpdate();
        }
    }

}
