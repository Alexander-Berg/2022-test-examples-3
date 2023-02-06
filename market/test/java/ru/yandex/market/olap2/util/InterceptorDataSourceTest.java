package ru.yandex.market.olap2.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertThat;

public class InterceptorDataSourceTest {
    private static final int Q_LIMIT_SECONDS = 42;
    private static final AtomicInteger CON_ID = new AtomicInteger(0);
    private final Connection con = Mockito.mock(Connection.class);
    private final HikariDataSource ds = Mockito.mock(HikariDataSource.class);
    private final DataSource interceptedDs = InterceptorDataSource
            .connectionCountingInterceptor(ds, "name", CON_ID, Q_LIMIT_SECONDS);

    @Test
    public void mustInterceptCalls() throws SQLException {
        Mockito.when(ds.getConnection()).then(invocation -> con);
        Mockito.doAnswer(invocation -> {
            assertThat(invocation.getArgument(1), Matchers.is(Q_LIMIT_SECONDS * 1000));
            return null;
        }).when(con).setNetworkTimeout(Mockito.any(), Mockito.anyInt());
        assertThat(interceptedDs.getConnection(), Matchers.is(con));
        assertThat(CON_ID.get(), Matchers.is(1));
    }
}
