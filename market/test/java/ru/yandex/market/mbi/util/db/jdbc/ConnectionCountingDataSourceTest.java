package ru.yandex.market.mbi.util.db.jdbc;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class ConnectionCountingDataSourceTest {
    private DataSource lowLevelDataSource = mock(DataSource.class, withSettings().extraInterfaces(Closeable.class));
    private ConnectionCountingDataSource dataSource;

    @BeforeEach
    void setUp() throws SQLException {
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
        dataSource = new ConnectionCountingDataSource(lowLevelDataSource, true);
    }

    @Test
    void testNoExceptions() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            // Some fake activity:
            try (var statement = connection.prepareStatement("update test set t1 = 2")) {
                statement.executeUpdate();
            }
        }
    }

    @Test
    void testTooManyConnectionsException() throws Exception {
        try (var connection1 = dataSource.getConnection()) {
            // Some fake activity:
            try (var statement = connection1.prepareStatement("update test set t1 = 2")) {
                statement.executeUpdate();
            }
            assertThatExceptionOfType(RuntimeException.class)
                    .isThrownBy(() -> {
                        try (var connection2 = dataSource.getConnection()) {
                            // Some fake activity:
                            try (var statement = connection2.prepareStatement("update test set t1 = 3")) {
                                statement.executeUpdate();
                            }
                        }
                    });
        }
    }

    @Test
    void testNoExceptionsOnMultipleClose() throws SQLException {
        var connection1 = dataSource.getConnection();
        connection1.close();
        connection1.close();
    }

    @Test
    void testExceptionsOnCloseFromAnotherThread() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        try {
            var future = executor.submit(() -> dataSource.getConnection());
            var connection = future.get();
            assertThatExceptionOfType(RuntimeException.class)
                    .isThrownBy(connection::close);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void closesUnderlyingIfItImplementsCloseable() throws IOException {
        dataSource.close();

        verify((Closeable) lowLevelDataSource).close();
    }
}
