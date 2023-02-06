package ru.yandex.market.test.pg.embedded;

import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;

import static java.lang.String.format;

/**
 * Created by antipov93@yndx-team.ru
 */
public class PGEmbeddedDataSource implements DataSource, Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PGEmbeddedDataSource.class);

    private PostgresConfig config;
    private String defaultSchemaName;

    @Override
    public Connection getConnection() throws SQLException {
        String url = format(
                "jdbc:postgresql://%s:%s/%s",
                config.net().host(),
                config.net().port(),
                config.storage().dbName()
        );
        try {
            String username = config.credentials().username();
            String password = config.credentials().password();
            Connection conn = DriverManager.getConnection(url, username, password);
            if (!isSchemaExist(conn, defaultSchemaName)) {
                tryCreateSchema(conn, defaultSchemaName);
            }
            conn.setSchema(defaultSchemaName);
            if (LOGGER != null) {
                LOGGER.debug("Created an embedded connection to " + url);
            }
            return conn;
        } catch (SQLException e) {
            if (LOGGER != null) {
                LOGGER.debug("Failed to create an embedded connection to " + url + ": " + e);
            }
            throw e;
        }
    }

    private boolean isSchemaExist(Connection connection, String schemaName) throws SQLException {
        if (StringUtils.isEmpty(schemaName)) {
            return true;
        }
        PreparedStatement ps = connection.prepareStatement(
                "SELECT count(*) FROM information_schema.schemata WHERE schema_name = ?");
        ps.setString(1, schemaName);
        ResultSet rs = ps.executeQuery();
        rs.next();
        int count = rs.getInt(1);
        ps.close();
        return count != 0;
    }

    private void tryCreateSchema(Connection connection, String schemaName) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("CREATE SCHEMA " + schemaName);
        ps.executeUpdate();
        ps.close();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getConnection();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {

    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {

    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    @Required
    public void setConfig(PostgresConfig config) {
        this.config = config;
    }

    public void setDefaultSchemaName(String defaultSchemaName) {
        this.defaultSchemaName = defaultSchemaName;
    }
}
