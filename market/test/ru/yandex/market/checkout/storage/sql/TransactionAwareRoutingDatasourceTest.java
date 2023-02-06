package ru.yandex.market.checkout.storage.sql;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.request.trace.Module;

@ExtendWith(MockitoExtension.class)
public class TransactionAwareRoutingDatasourceTest {

    private TransactionAwareRoutingDatasource datasource;
    private TransactionTemplate transactionTemplate;
    private TransactionTemplate readonlyTransactionTemplate;

    @Mock
    private DataSource master;
    @Mock
    private DataSource slave;
    @Mock
    private Connection connection;

    @BeforeEach
    public void setUp() throws SQLException {
        datasource = new TransactionAwareRoutingDatasource();
        datasource.setTargetDataSources(ImmutableMap.of(
                DataSourceType.MASTER, master,
                DataSourceType.SLAVE, slave
        ));
        datasource.afterPropertiesSet();

        DataSourceTransactionManagerWrapper transactionManager =
                new DataSourceTransactionManagerWrapper(Module.CHECKOUTER);
        transactionManager.setDataSource(datasource);
        transactionManager.afterPropertiesSet();

        transactionTemplate = new TransactionTemplate(transactionManager);

        DefaultTransactionDefinition readOnly = new DefaultTransactionDefinition();
        readOnly.setReadOnly(true);
        readonlyTransactionTemplate = new TransactionTemplate(transactionManager, readOnly);

        Mockito.lenient().when(master.getConnection()).thenReturn(connection);
        Mockito.lenient().when(slave.getConnection()).thenReturn(connection);
    }

    @Test
    public void shouldChooseSlaveByDefault() throws SQLException {
        datasource.getConnection();

        Mockito.verify(slave, Mockito.only()).getConnection();
        Mockito.verify(master, Mockito.never()).getConnection();
    }

    @Test
    public void shouldChooseMasterIfNotReadOnlyTransaction() throws SQLException {
        transactionTemplate.execute(ts -> {
            DataSourceUtils.getConnection(datasource);
            return null;
        });

        Mockito.verify(slave, Mockito.never()).getConnection();
        Mockito.verify(master, Mockito.only()).getConnection();
    }

    @Test
    public void shouldChooseSlaveIfReadonlyTransaction() throws SQLException {
        readonlyTransactionTemplate.execute(ts -> {
            DataSourceUtils.getConnection(datasource);
            return null;
        });

        Mockito.verify(slave, Mockito.only()).getConnection();
        Mockito.verify(master, Mockito.never()).getConnection();
    }
}
