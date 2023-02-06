package ru.yandex.market.sqb.test.db.datasource;

import java.sql.SQLException;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import oracle.jdbc.pool.OracleDataSource;
import org.hsqldb.jdbc.JDBCDataSource;

/**
 * Фабрика для создания {@link DataSource}.
 *
 * @author Vladislav Bauer
 */
public final class DbDataSourceFactory {

    // Тестовое окружение / in-memory DB
    public static final String TEST_DRIVER_CLASS = "org.hsqldb.jdbc.JDBCDriver";
    public static final String TEST_CONNECTION_URL = "jdbc:hsqldb:mem:test";
    public static final String TEST_USERNAME = "sa";
    public static final String TEST_PASSWORD = "";

    // Development окружение для интеграционных тестов с реальной Oracle БД
    private static final String REAL_CONNECTION_URL =
            "jdbc:oracle:thin:@mdbaas-sas-scan.paysys.yandex.net:1521/billingdb";
    private static final String REAL_USERNAME = "sysdev";
    private static final String REAL_PASSWORD = "sysdev";


    private DbDataSourceFactory() {
        throw new UnsupportedOperationException();
    }


    @Nonnull
    public static DataSource createTestDataSource() {
        final JDBCDataSource dataSource = new JDBCDataSource();
        dataSource.setURL(TEST_CONNECTION_URL);
        dataSource.setUser(TEST_USERNAME);
        dataSource.setPassword(TEST_PASSWORD);
        return dataSource;
    }

    @Nonnull
    public static DataSource createRealDataSource() throws SQLException {
        final OracleDataSource dataSource = new OracleDataSource();
        dataSource.setURL(REAL_CONNECTION_URL);
        dataSource.setUser(REAL_USERNAME);
        dataSource.setPassword(REAL_PASSWORD);
        return dataSource;
    }

}
