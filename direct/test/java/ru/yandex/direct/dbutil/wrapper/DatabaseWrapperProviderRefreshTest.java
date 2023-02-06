package ru.yandex.direct.dbutil.wrapper;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.db.config.DbConfig;
import ru.yandex.direct.db.config.DbConfigEvent;
import ru.yandex.direct.db.config.DbConfigFactory;
import ru.yandex.direct.env.EnvironmentType;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.direct.dbutil.wrapper.SimpleDb.CLICKHOUSE_CLOUD;
import static ru.yandex.direct.dbutil.wrapper.SimpleDb.PPCDICT;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

public class DatabaseWrapperProviderRefreshTest {

    private DatabaseWrapperProvider testedProvider;
    private DataSourceFactory dataSourceFactory;
    private DbConfigFactory dbConfigFactory;
    private DataSource dataSource1;
    private DbConfig dbConfig1;
    private DataSource dataSource2;
    private DbConfig dbConfig2;

    @Before
    public void setUp() {
        dataSource1 = mock(HikariDataSource.class);
        dbConfig1 = mock(DbConfig.class);
        dataSource2 = mock(HikariDataSource.class);
        dbConfig2 = mock(DbConfig.class);

        dataSourceFactory = mock(DataSourceFactory.class);
        when(dataSourceFactory.createDataSource(any())).thenReturn(dataSource1);

        dbConfigFactory = mock(DbConfigFactory.class);
        when(dbConfigFactory.has(PPCDICT.toString())).thenReturn(true);
        when(dbConfigFactory.get(PPCDICT.toString())).thenReturn(dbConfig1);
        when(dbConfigFactory.has(CLICKHOUSE_CLOUD.toString())).thenReturn(true);
        when(dbConfigFactory.get(CLICKHOUSE_CLOUD.toString())).thenReturn(dbConfig1); // dbConfig1 for both arguments!

        testedProvider = DatabaseWrapperProvider
                .newInstance(dataSourceFactory, dbConfigFactory, EnvironmentType.DEVELOPMENT);
    }

    @Test
    public void get_ReturnsSameCachedWrapperAfterUpdate() {
        DatabaseWrapper dbWrapper = testedProvider.get(PPCDICT);

        when(dbConfigFactory.get(PPCDICT.toString())).thenReturn(dbConfig2);
        when(dataSourceFactory.createDataSource(any())).thenReturn(dataSource2);

        testedProvider.update(new DbConfigEvent());

        DatabaseWrapper dbWrapperSameInstance = testedProvider.get(PPCDICT);
        assertThat(dbWrapper, sameInstance(dbWrapperSameInstance));
    }

    @Test
    public void update_ObtainsNewDbConfigFromFactory() {
        testedProvider.get(PPCDICT);

        testedProvider.update(new DbConfigEvent());

        verify(dbConfigFactory, times(2)).get(PPCDICT.toString());
    }

    @Test
    public void update_ObtainsNewDataSourceFromFactory() {
        testedProvider.get(PPCDICT);

        when(dbConfigFactory.get(PPCDICT.toString())).thenReturn(dbConfig2);

        testedProvider.update(new DbConfigEvent());

        verify(dataSourceFactory).createDataSource(argThat(sameInstance(dbConfig2)));
    }

    @Test
    public void update_SetsNewDataSourceToWrapperFromFactory() {
        DatabaseWrapper dbWrapper = testedProvider.get(PPCDICT);

        when(dbConfigFactory.get(PPCDICT.toString())).thenReturn(dbConfig2);
        when(dataSourceFactory.createDataSource(any())).thenReturn(dataSource2);

        testedProvider.update(new DbConfigEvent());

        DatabaseWrapper dbWrapperSameInstance = testedProvider.get(PPCDICT);
        assumeThat(dbWrapper, sameInstance(dbWrapperSameInstance));
        assertThat(((RefreshableDataSource) dbWrapper.getDataSource()).getDataSource(), sameInstance(dataSource2));
    }

    @Test
    public void update_SetsNewDataSourceToAllWrappersFromFactory() {
        testedProvider.get(PPCDICT);
        DatabaseWrapper dbWrapper2 = testedProvider.get(CLICKHOUSE_CLOUD);

        when(dbConfigFactory.get(PPCDICT.toString())).thenReturn(dbConfig2);
        when(dataSourceFactory.createDataSource(any())).thenReturn(dataSource2);

        testedProvider.update(new DbConfigEvent());

        DatabaseWrapper dbWrapper2SameInstance = testedProvider.get(CLICKHOUSE_CLOUD);
        assumeThat(dbWrapper2, sameInstance(dbWrapper2SameInstance));
        assertThat(((RefreshableDataSource) dbWrapper2.getDataSource()).getDataSource(), sameInstance(dataSource2));
    }

    @Test
    public void update_ClosesOldDataSourceAtWrapper() {
        testedProvider.get(PPCDICT);

        testedProvider.update(new DbConfigEvent());

        verify((HikariDataSource) dataSource1).close();
    }

    @Test
    public void update_EvictDataSourceOnError() {
        // поместим dataSource в кэш
        testedProvider.get(PPCDICT);

        when(dbConfigFactory.get(PPCDICT.toString())).thenThrow(
                new RuntimeException("Error from dbConfigFactory should be caught in dbWrapperProvider.update"));

        testedProvider.update(new DbConfigEvent());
        // ожидаем, что не будет исключения
    }

    @Test
    public void update_EvictDataSourceWhenMissedFromConfig() {
        // поместим dataSource в кэш
        testedProvider.get(PPCDICT);

        // сбрасываем состояние mock'а, чтобы считать вызовы только в update(..)
        reset(dbConfigFactory);
        when(dbConfigFactory.has(PPCDICT.toString())).thenReturn(false);

        testedProvider.update(new DbConfigEvent());
        // ожидаем, что так как dbConfig говорит, что не знает такого dataSource, то и get(..) вызываться не будет
        verify(dbConfigFactory, times(0)).get(PPCDICT.toString());
    }
}
