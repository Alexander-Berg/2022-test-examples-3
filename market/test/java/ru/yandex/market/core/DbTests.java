package ru.yandex.market.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;

public final class DbTests {

    private DbTests() {
        throw new UnsupportedOperationException();
    }

    public static DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=Oracle;TRACE_LEVEL_SYSTEM_OUT=2");
        return ds;
    }

    public static void initDataSourceFromClassPath(DataSource ds, String filename) {
        executeSqlQuery(ds, "RUNSCRIPT FROM 'classpath:" + filename + "'");
    }

    public static void initDataSourceFromFile(DataSource ds, String filename) {
        executeSqlQuery(ds, "RUNSCRIPT FROM '" + filename + "'");
    }


    private static void executeSqlQuery(DataSource ds, String sqlQuery) {
        try (Connection conn = ds.getConnection()) {
            executeSqlQuery(conn, sqlQuery);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    private static void executeSqlQuery(Connection connection, String sqlQuery) {
        try (Statement st = connection.createStatement()) {
            st.execute(sqlQuery);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
