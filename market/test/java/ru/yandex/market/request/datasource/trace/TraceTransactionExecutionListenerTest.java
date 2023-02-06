package ru.yandex.market.request.datasource.trace;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.request.trace.Module;
import ru.yandex.market.request.trace.RequestContextHolder;
import ru.yandex.market.request.trace.RequestLogRecordBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TraceTransactionExecutionListenerTest {
    private MockLogger mockLogger;
    private Connection connection;

    private Method setAutoCommit;
    private Method commitMethod;
    private Method rollbackMethod;
    private Method dummyMethod;

    @Before
    public void setUp() throws NoSuchMethodException {
        mockLogger = new MockLogger();
        connection = mock(Connection.class);

        setAutoCommit = Connection.class.getMethod("setAutoCommit", boolean.class);
        commitMethod = Connection.class.getMethod("commit");
        rollbackMethod = Connection.class.getMethod("rollback");
        dummyMethod = PreparedStatement.class.getDeclaredMethod("setString", int.class, String.class);
    }

    @Test
    public void testBegin() throws SQLException {
        RequestContextHolder.createContext("123");
        when(connection.getAutoCommit()).thenReturn(true);

        MethodExecutionContext executionContext = new MethodExecutionContext();
        executionContext.setMethod(setAutoCommit);
        executionContext.setMethodArgs(new Object[]{false});
        executionContext.setElapsedTime(50);
        executionContext.setTarget(connection);

        TraceTransactionExecutionListener listener = new TraceTransactionExecutionListener(Module.PGAAS,
                mockLogger, () -> true);
        listener.execute(executionContext);

        RequestLogRecordBuilder expected =
                RequestLogRecordBuilder.forCurrentOutRequest(RequestContextHolder.getContext());
        expected.setDurationMillis(50);
        expected.setTarget(Module.PGAAS);
        expected.setQuery("begin");

        assertEquals(expected, mockLogger.get());
    }

    @Test
    public void testSetAutoCommitToTrue() {
        RequestContextHolder.createContext("123");

        MethodExecutionContext executionContext = new MethodExecutionContext();
        executionContext.setMethod(setAutoCommit);
        executionContext.setMethodArgs(new Object[]{true});
        executionContext.setElapsedTime(50);
        executionContext.setTarget(connection);

        TraceTransactionExecutionListener listener = new TraceTransactionExecutionListener(Module.PGAAS,
                mockLogger, () -> true);
        listener.execute(executionContext);

        assertTrue(mockLogger.getAll().isEmpty());
    }

    @Test
    public void testCommit() {
        RequestContextHolder.createContext("123");

        MethodExecutionContext executionContext = new MethodExecutionContext();
        executionContext.setMethod(commitMethod);
        executionContext.setElapsedTime(50);
        executionContext.setTarget(connection);

        TraceTransactionExecutionListener listener = new TraceTransactionExecutionListener(Module.PGAAS,
                mockLogger, () -> true);
        listener.execute(executionContext);

        RequestLogRecordBuilder expected =
                RequestLogRecordBuilder.forCurrentOutRequest(RequestContextHolder.getContext());
        expected.setDurationMillis(50);
        expected.setTarget(Module.PGAAS);
        expected.setQuery("commit");

        assertEquals(expected, mockLogger.get());
    }

    @Test
    public void testRollback() {
        RequestContextHolder.createContext("123");

        MethodExecutionContext executionContext = new MethodExecutionContext();
        executionContext.setMethod(rollbackMethod);
        executionContext.setElapsedTime(50);
        executionContext.setTarget(connection);

        TraceTransactionExecutionListener listener = new TraceTransactionExecutionListener(Module.PGAAS,
                mockLogger, () -> true);
        listener.execute(executionContext);

        RequestLogRecordBuilder expected =
                RequestLogRecordBuilder.forCurrentOutRequest(RequestContextHolder.getContext());
        expected.setDurationMillis(50);
        expected.setTarget(Module.PGAAS);
        expected.setQuery("rollback");

        assertEquals(expected, mockLogger.get());
    }

    @Test
    public void testSkipOnAnotherMethod() {
        RequestContextHolder.createContext("123");

        MethodExecutionContext executionContext = new MethodExecutionContext();
        executionContext.setMethod(dummyMethod);
        executionContext.setElapsedTime(50);
        executionContext.setTarget(connection);

        TraceTransactionExecutionListener listener = new TraceTransactionExecutionListener(Module.PGAAS,
                mockLogger, () -> true);
        listener.execute(executionContext);

        assertTrue(mockLogger.getAll().isEmpty());
    }

    @Test
    public void testSkipOnNotTracing() {
        RequestContextHolder.createContext("123");

        MethodExecutionContext executionContext = new MethodExecutionContext();
        executionContext.setMethod(commitMethod);
        executionContext.setElapsedTime(50);
        executionContext.setTarget(connection);

        TraceTransactionExecutionListener listener = new TraceTransactionExecutionListener(Module.PGAAS,
                mockLogger, () -> false);
        listener.execute(executionContext);

        assertTrue(mockLogger.getAll().isEmpty());
    }

    @Test
    public void testThrowable() {
        RequestContextHolder.createContext("123");

        MethodExecutionContext executionContext = new MethodExecutionContext();
        executionContext.setMethod(commitMethod);
        executionContext.setElapsedTime(50);
        executionContext.setTarget(connection);
        executionContext.setThrown(new RuntimeException("Custom thrown"));

        TraceTransactionExecutionListener listener = new TraceTransactionExecutionListener(Module.PGAAS,
                mockLogger, () -> true);
        listener.execute(executionContext);

        RequestLogRecordBuilder expected =
                RequestLogRecordBuilder.forCurrentOutRequest(RequestContextHolder.getContext());
        expected.setDurationMillis(50);
        expected.setTarget(Module.PGAAS);
        expected.setQuery("commit");
        expected.setErrorMessage("java.lang.RuntimeException: Custom thrown");

        assertEquals(expected, mockLogger.get());
    }
}
