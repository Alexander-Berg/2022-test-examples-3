package ru.yandex.direct.dbutil.wrapper;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RefreshableDataSourceTest {

    private RefreshableDataSource testedRefreshableDataSource;
    private DataSource initDataSource;
    private DataSource newDataSource;

    @Before
    public void before() {
        initDataSource = mock(DataSource.class);
        newDataSource = mock(DataSource.class);
        testedRefreshableDataSource = new RefreshableDataSource(initDataSource);
    }

    @Test
    public void instantiate_SetsWrappedDataSource() {
        assertThat(testedRefreshableDataSource.getDataSource(), sameInstance(initDataSource));
    }

    @Test
    public void refresh_SetsNewWrappedDataSource() {
        testedRefreshableDataSource.refresh(newDataSource);
        assertThat(testedRefreshableDataSource.getDataSource(), sameInstance(newDataSource));
    }

    @Test
    public void refresh_ReturnsPreviousDataSource() {
        DataSource previous = testedRefreshableDataSource.refresh(newDataSource);
        assertThat(previous, sameInstance(initDataSource));
    }

    @Test
    public void getConnection_DelegatesCallToWrappedDataSource() throws SQLException {
        testedRefreshableDataSource.getConnection();
        verify(initDataSource).getConnection();
    }
}
