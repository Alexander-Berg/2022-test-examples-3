package ru.yandex.market.infra;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Dmitry Poluyanov <a href="https://t.me/neiwick">Dmitry Poluyanov</a>
 * @since 05.10.17
 */
public class TestClickHouseConnection {
    private static final String CLICKHOUSE_DB_URL =
            String.format("jdbc:clickhouse://%s:8123/default", System.getenv("DOCKER_HOSTNAME"));
    private static Connection connection;

    @BeforeClass
    public static void beforeClass() throws SQLException {
        connection = DriverManager.getConnection(CLICKHOUSE_DB_URL);
    }

    @AfterClass
    public static void afterClass() throws SQLException {
        connection.close();
    }

    @Test
    public void testConnection() throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1")) {
            try (ResultSet rs = statement.executeQuery()) {
                Assert.assertTrue("Should have at least one row", rs.next());
                Assert.assertEquals(1, rs.getInt(1));
            }
        }
    }
}
