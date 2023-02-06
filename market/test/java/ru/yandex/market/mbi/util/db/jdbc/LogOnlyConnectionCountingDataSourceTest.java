package ru.yandex.market.mbi.util.db.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class LogOnlyConnectionCountingDataSourceTest {
    private ConnectionCountingDataSource dataSource;
    private DataSource lowLevelDataSource = mock(DataSource.class);

    @BeforeEach
    public void prepareDatasource() throws SQLException {
        when(lowLevelDataSource.getConnection()).thenAnswer(invocation -> {
            var isClosed = new AtomicBoolean(false);
            var connection = mock(Connection.class);
            when(connection.prepareStatement(anyString()))
                    .thenAnswer(i -> mock(PreparedStatement.class));
            doAnswer(invocation1 -> {
                isClosed.set(true);
                return null;
            }).when(connection).close();
            doAnswer(invocation1 -> isClosed.get()).when(connection).isClosed();
            return connection;
        });
        dataSource = new ConnectionCountingDataSource(lowLevelDataSource);
    }

    @Test
    public void testTooManyConnectionsNoExceptionLogOnly() throws SQLException {
        try (var connection1 = dataSource.getConnection()) {
            // Some fake activity:
            try (var statement = connection1.prepareStatement("update test set t1 = 2")) {
                statement.executeUpdate();
            }
            try (var connection2 = dataSource.getConnection()) {
                // Some fake activity:
                try (var statement = connection2.prepareStatement("update test set t1 = 3")) {
                    statement.executeUpdate();
                }
            }
        }
    }

    @Test
    public void testTooManyConnectionsNoExceptionLogOnlyWith3Connections() throws SQLException {
        try (var connection1 = dataSource.getConnection()) {
            // Some fake activity:
            try (var statement = connection1.prepareStatement("update test set t1 = 2")) {
                statement.executeUpdate();
            }
            try (var connection2 = dataSource.getConnection()) {
                // Some fake activity:
                try (var statement = connection2.prepareStatement("update test set t1 = 3")) {
                    statement.executeUpdate();
                }
                try (var connection3 = dataSource.getConnection()) {
                    // Some fake activity:
                    try (var statement = connection3.prepareStatement("update test set t1 = 4")) {
                        statement.executeUpdate();
                    }
                }
            }
        }
    }

    @Test
    public void testLogOnlyAndNoExceptionsOnCloseFromAnotherThread() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        try {
            var future = executor.submit(() -> dataSource.getConnection());
            var connection = future.get();
            connection.close();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void closesUnderlyingIfItImplementsCloseable() throws IOException {
        dataSource.close();

        verifyNoInteractions(lowLevelDataSource);
    }
}
