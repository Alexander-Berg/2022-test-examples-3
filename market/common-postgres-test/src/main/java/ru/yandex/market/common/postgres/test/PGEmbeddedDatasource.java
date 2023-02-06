package ru.yandex.market.common.postgres.test;

import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;

/**
 * Created by antipov93@yndx-team.ru
 */
public class PGEmbeddedDatasource implements DataSource, Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PGEmbeddedDatasource.class);

    private final PGSchemaInitializer schemaInitializer;
    private final String url;

    /**
     * Дополнительные параметры для JDBC-connection-а.
     * <a href="https://jdbc.postgresql.org/documentation/83/connect.html">Документация по параметрам</a>
     */
    private final Properties additionalProperties;

    public PGEmbeddedDatasource(PostgresConfig config) {
        this(config, null);
    }

    public PGEmbeddedDatasource(PostgresConfig config, String defaultSchemaName) {
        this(config, defaultSchemaName, null);
    }

    public PGEmbeddedDatasource(PostgresConfig config,
                                String defaultSchemaName,
                                Properties additionalProperties) {
        this.url = String.format(
                "jdbc:postgresql://%s:%s/%s",
                config.net().host(),
                config.net().port(),
                config.storage().dbName()
        );
        this.schemaInitializer = new PGSchemaInitializer(defaultSchemaName);
        this.additionalProperties = createAdditionalProperties(config, additionalProperties);;
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(url, additionalProperties);
            schemaInitializer.initSchemaIfNecessary(conn);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Created an embedded connection to {}", url);
            }
            return conn;
        } catch (SQLException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Failed to create an embedded connection to {}: {}", url, e.getMessage());
            }
            throw e;
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getConnection();
    }

    @Override
    public <T> T unwrap(Class<T> iface) {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return false;
    }

    @Override
    public PrintWriter getLogWriter() {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
    }

    @Override
    public void setLoginTimeout(int seconds) {
    }

    @Override
    public int getLoginTimeout() {
        return 0;
    }

    @Override
    public java.util.logging.Logger getParentLogger() {
        return null;
    }

    public String getUrl() {
        return url;
    }

    private Properties createAdditionalProperties(PostgresConfig config, Properties original) {
        Properties result = original != null ? new Properties(original) : new Properties();

        String username = config.credentials().username();
        if (username != null) {
            result.put("user", username);
        }

        String password = config.credentials().password();
        if (password != null) {
            result.put("password", password);
        }

        return result;
    }

}
