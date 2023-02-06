package ru.yandex.market.request.datasource.trace;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.ttddyy.dsproxy.ConnectionInfo;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.proxy.ParameterSetOperation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.request.datasource.trace.DatasourceTimingContext.DatasourceTimingType;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.request.trace.RequestContextHolder;
import ru.yandex.market.request.trace.RequestLogRecordBuilder;

public class TraceQueryExecutionListenerTest {

    private MockLogger mockLogger;
    private ConnectionInfo connectionInfo;
    private Method dummyMethod;
    private Statement statement;

    @Before
    public void setUp() throws NoSuchMethodException {
        mockLogger = new MockLogger();
        statement = Mockito.mock(Statement.class);
        connectionInfo = new ConnectionInfo();
        dummyMethod = PreparedStatement.class.getDeclaredMethod("setString", int.class, String.class);
    }

    @Test
    public void shouldExtractHostFromUrl() {
        String url = "jdbc:mysql://mp-storage02:3306/checkout?connectTimeout=10000&socketTimeout=50000&useUnicode" +
                "=true&characterEncoding=utf8";

        String host = BaseExecutionListener.extractHost(url);

        Assert.assertEquals("mp-storage02:3306", host);
    }

    @Test
    public void shouldExtractDatabaseFromUrl() {
        String url = "jdbc:mysql://mp-storage02:3306/checkout?connectTimeout=10000&socketTimeout=50000&useUnicode" +
                "=true&characterEncoding=utf8";
        String database = BaseExecutionListener.extractDatabase(url);

        Assert.assertEquals("checkout", database);
    }

    @Test
    public void extractDatabaseFromUrl() {
        String url = "jdbc:oracle:thin:@grade.yandex.ru";
        String database = BaseExecutionListener.extractDatabase(url);

        Assert.assertEquals(url, database);
    }

    @Test
    public void testIgnore() {
        String query = "select 1";
        TraceQueryExecutionListener withIgnore = new TraceQueryExecutionListener(Module.PGAAS,
                Collections.singleton(query));
        Assert.assertFalse(withIgnore.queryNotIgnored(Collections.singletonList(new QueryInfo(query))));
        Assert.assertTrue(withIgnore.queryNotIgnored(Collections.singletonList(new QueryInfo(query + " as foobar"))));

        TraceQueryExecutionListener withoutIgnore = new TraceQueryExecutionListener(Module.PGAAS);
        Assert.assertTrue(withoutIgnore.queryNotIgnored(Collections.singletonList(new QueryInfo(query))));
    }

    @Test
    public void testCreateOneLogRecordForNonBatchQuery() {
        String query = "select 1";
        RequestContextHolder.createContext("123");
        TraceQueryExecutionListener listener = new TraceQueryExecutionListener(Module.PGAAS, Collections.emptySet(),
                mockLogger, () -> true);
        final ExecutionInfo execInfo = new ExecutionInfo(connectionInfo, statement, false, -1, null, new Object[0]);
        execInfo.setSuccess(true);
        execInfo.setElapsedTime(50);
        final QueryInfo queryInfo = new QueryInfo(query);
        listener.afterQuery(execInfo, Collections.singletonList(queryInfo));
        final RequestLogRecordBuilder expected =
                RequestLogRecordBuilder.forCurrentOutRequest(RequestContextHolder.getContext());
        expected.setDurationMillis(50);
        expected.setTarget(Module.PGAAS);
        expected.setQuery(query);

        Assert.assertEquals(expected, mockLogger.get());
    }

    @Test
    public void testCreateOneLogRecordForEachArgumentsOnBatchQuery() {
        String query = "select 1";
        RequestContextHolder.createContext("123");

        TraceQueryExecutionListener listener = new TraceQueryExecutionListener(Module.PGAAS, Collections.emptySet(),
                mockLogger, () -> true);
        final ExecutionInfo execInfo = new ExecutionInfo(connectionInfo, statement, false, -1, null, new Object[0]);
        execInfo.setSuccess(true);
        execInfo.setElapsedTime(50);
        final QueryInfo queryInfo = new QueryInfo(query);
        queryInfo.setParametersList(Arrays.asList(
                Collections.singletonList(new ParameterSetOperation(dummyMethod, new Object[] {"k0", "v0"})),
                Collections.singletonList(new ParameterSetOperation(dummyMethod, new Object[] {"k1", "v1"})),
                Collections.singletonList(new ParameterSetOperation(dummyMethod, new Object[] {"k2", "v2"}))
        ));

        listener.afterQuery(execInfo, Collections.singletonList(queryInfo));

        List<RequestLogRecordBuilder> builders = mockLogger.getAll();
        for (int i = 0; i < 3; i++) {

            final RequestLogRecordBuilder expected =
                    RequestLogRecordBuilder.forCurrentOutRequest(RequestContextHolder.getContext());
            expected.setDurationMillis(50);
            expected.setTarget(Module.PGAAS);
            expected.setQuery(query);
            expected.addKeyValue("k" + i, "v" + i);

            Assert.assertEquals(expected, builders.get(i));
        }
    }

    @Test
    public void testCreateOneLogRecordForEachArgumentsOnMultiSetQuery() {
        String query = "select 1";
        RequestContextHolder.createContext("123");
        TraceQueryExecutionListener listener = new TraceQueryExecutionListener(Module.PGAAS, Collections.emptySet(),
                mockLogger, () -> true);
        final ExecutionInfo execInfo = new ExecutionInfo(connectionInfo, statement, false, -1, null, new Object[0]);
        execInfo.setSuccess(true);
        execInfo.setElapsedTime(50);
        final QueryInfo queryInfo = new QueryInfo(query);
        queryInfo.setParametersList(Collections.singletonList(Arrays.asList(
                new ParameterSetOperation(dummyMethod, new Object[] {"k0", "v0"}),
                new ParameterSetOperation(dummyMethod, new Object[] {"k1", "v1"}),
                new ParameterSetOperation(dummyMethod, new Object[] {"k2", "v2"})
        )));

        listener.afterQuery(execInfo, Collections.singletonList(queryInfo));

        final RequestLogRecordBuilder expected =
                RequestLogRecordBuilder.forCurrentOutRequest(RequestContextHolder.getContext());
        expected.setDurationMillis(50);
        expected.setTarget(Module.PGAAS);
        expected.setQuery(query);
        expected.addKeyValue("k0", "v0");
        expected.addKeyValue("k1", "v1");
        expected.addKeyValue("k2", "v2");

        Assert.assertEquals(expected, mockLogger.get());
    }

    @Test
    public void testCreateOneLogRecordForEachStatementOnBatchQuery() {
        String query = "select 1";
        RequestContextHolder.createContext("123");
        TraceQueryExecutionListener listener = new TraceQueryExecutionListener(Module.PGAAS, Collections.emptySet(),
                mockLogger, () -> true);
        final ExecutionInfo execInfo = new ExecutionInfo(connectionInfo, statement, false, -1, null, new Object[0]);
        execInfo.setSuccess(true);
        execInfo.setElapsedTime(50);
        final List<QueryInfo> queryInfos = Arrays.asList(
                new QueryInfo(query + 0),
                new QueryInfo(query + 1),
                new QueryInfo(query + 2)
        );
        listener.afterQuery(execInfo, queryInfos);
        for (int i = 0; i < 3; i++) {
            final RequestLogRecordBuilder expected =
                    RequestLogRecordBuilder.forCurrentOutRequest(RequestContextHolder.getContext());
            expected.setDurationMillis(50);
            expected.setTarget(Module.PGAAS);
            expected.setQuery(query + i);

            Assert.assertEquals(expected, mockLogger.getAll().get(i));
        }
    }

    @Test
    public void testDatasourceTimingContext() {
        String query = "select 1";
        RequestContextHolder.createContext("123");

        DatasourceTimingContext.get().record(DatasourceTimingType.GET_CONNECTION, 10);
        DatasourceTimingContext.get().record(DatasourceTimingType.PREPARE_STATEMENT, 30);

        TraceQueryExecutionListener listener = new TraceQueryExecutionListener(Module.PGAAS, Collections.emptySet(),
                mockLogger, () -> true);
        final ExecutionInfo execInfo = new ExecutionInfo(connectionInfo, statement, false, -1, null, new Object[0]);
        execInfo.setSuccess(true);
        execInfo.setElapsedTime(50);
        final QueryInfo queryInfo = new QueryInfo(query);
        listener.afterQuery(execInfo, Collections.singletonList(queryInfo));
        final RequestLogRecordBuilder expected =
                RequestLogRecordBuilder.forCurrentOutRequest(RequestContextHolder.getContext());
        expected.setDurationMillis(50);
        expected.setTarget(Module.PGAAS);
        expected.setQuery(query);
        expected.addKeyValue("timing.getConnection.nanos", 10);
        expected.addKeyValue("timing.prepareStatement.nanos", 30);

        Assert.assertEquals(expected, mockLogger.get());
    }
}
