package ru.yandex.market.olap2;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.olap2.util.InterceptorDataSource;

import static org.junit.Assert.assertThat;

public class InterceptorDataSourceTest {
    private static final int Q_LIMIT_SECONDS = 42;
    private final Connection con = Mockito.mock(Connection.class);
    private final HikariDataSource ds = Mockito.mock(HikariDataSource.class);
    private final DataSource interceptedDs = InterceptorDataSource
            .connectionCountingInterceptor(ds, "name", Q_LIMIT_SECONDS);

    @Test
    public void mustInterceptCalls() throws SQLException {
        Mockito.when(ds.getConnection()).then(invocation -> con);
        Mockito.doAnswer(invocation -> {
            assertThat(invocation.getArgument(1), Matchers.is(Q_LIMIT_SECONDS * 1000));
            return null;
        }).when(con).setNetworkTimeout(Mockito.any(), Mockito.anyInt());
        assertThat(interceptedDs.getConnection(), Matchers.is(con));
    }
}
