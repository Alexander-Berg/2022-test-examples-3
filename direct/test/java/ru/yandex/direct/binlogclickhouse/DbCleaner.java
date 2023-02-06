package ru.yandex.direct.binlogclickhouse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import ru.yandex.direct.mysql.MySQLUtils;

public class DbCleaner implements AutoCloseable {
    private Connection conn;
    private Set<String> origDbNames;

    public DbCleaner(Connection conn) throws SQLException {
        this.conn = conn;
        this.origDbNames = readDbNames(conn);
    }

    static Set<String> readDbNames(Connection conn) throws SQLException {
        Set<String> dbNames = new HashSet<>();
        try (
                PreparedStatement statement = conn.prepareStatement("SHOW DATABASES");
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                dbNames.add(resultSet.getString(1));
            }
        }
        return dbNames;
    }

    @Override
    public void close() throws SQLException {
        for (String dbName : readDbNames(conn)) {
            if (!origDbNames.contains(dbName)) {
                MySQLUtils.executeUpdate(conn, "DROP DATABASE " + MySQLUtils.quoteName(dbName));
            }
        }
    }
}
