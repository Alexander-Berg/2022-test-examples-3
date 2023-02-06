package ru.yandex.market.logistics.test.integration.db.cleaner;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

public final class DataSourceUtils {

    private DataSourceUtils() {
        throw new UnsupportedOperationException();
    }

    public static String getDefaultSchemaFromConnection(DataSource dataSource) {
        try (Connection c = dataSource.getConnection()) {
            return c.getSchema();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
