package ru.yandex.market.mbo.tms.model;

import java.sql.Connection;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class CountMappingsPerModelJobExecutorTest {

    private CountMappingsPerModelJobExecutor executor;
    private JdbcTemplate yqlTemplateMock;
    private JdbcTemplate scatTemplateMock;
    private TransactionTemplate scatTxTemplateMock;

    private DataSource dataSourceMock;
    private Connection connectionMock;
    private ResultSet resultSetMock;

    @Before
    public void setUp() {
        yqlTemplateMock = Mockito.mock(JdbcTemplate.class);
        scatTemplateMock = Mockito.mock(JdbcTemplate.class);
        scatTxTemplateMock = Mockito.mock(TransactionTemplate.class);

        dataSourceMock = Mockito.mock(DataSource.class);
        connectionMock = Mockito.mock(Connection.class);
        resultSetMock = Mockito.mock(ResultSet.class);

        executor = new CountMappingsPerModelJobExecutor();
        executor.setSqlJdbcTemplate(scatTemplateMock);
        executor.setSqlTransactionTemplate(scatTxTemplateMock);
        executor.setYqlJdbcTemplate(yqlTemplateMock);
    }

    @Test
    public void testClosingScatConnection() throws Exception {
        Mockito.when(scatTemplateMock.getDataSource()).thenReturn(dataSourceMock);
        Mockito.when(scatTxTemplateMock.execute(Mockito.any(TransactionCallback.class)))
            .then(invocation -> {
                TransactionCallback<Void> callback = invocation.getArgument(0);
                return callback.doInTransaction(Mockito.mock(TransactionStatus.class));
            });

        Mockito.when(dataSourceMock.getConnection()).thenReturn(connectionMock);
        Mockito.when(resultSetMock.next()).thenReturn(false);
        Mockito.when(yqlTemplateMock.query(Mockito.anyString(), Mockito.any(ResultSetExtractor.class)))
            .then(invocation -> {
                ResultSetExtractor<Void> extractor = invocation.getArgument(1);
                extractor.extractData(resultSetMock);
                return null;
            });

        executor.doRealJob(null);
        Mockito.verify(dataSourceMock, Mockito.times(1)).getConnection();
        Mockito.verify(connectionMock, Mockito.times(1)).close();
    }
}
