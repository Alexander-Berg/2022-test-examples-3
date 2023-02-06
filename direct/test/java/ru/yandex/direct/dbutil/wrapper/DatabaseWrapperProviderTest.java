package ru.yandex.direct.dbutil.wrapper;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.db.config.DbConfig;
import ru.yandex.direct.db.config.DbConfigFactory;
import ru.yandex.direct.env.EnvironmentType;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

public class DatabaseWrapperProviderTest {
    private DatabaseWrapperProvider testedProvider;
    private DataSourceFactory dataSourceFactory;
    private DbConfigFactory dbConfigFactory;
    private DataSource dataSource;
    private DbConfig dbConfig;

    @Before
    public void setUp() {
        dataSource = mock(DataSource.class);
        dbConfig = mock(DbConfig.class);

        dataSourceFactory = mock(DataSourceFactory.class);
        when(dataSourceFactory.createDataSource(any())).thenReturn(dataSource);

        dbConfigFactory = mock(DbConfigFactory.class);
        when(dbConfigFactory.get(SimpleDb.PPCDICT.toString())).thenReturn(dbConfig);

        testedProvider = DatabaseWrapperProvider
                .newInstance(dataSourceFactory, dbConfigFactory, EnvironmentType.DEVELOPMENT);
    }

    @Test
    public void instantiate_RegistersAtDbConfigFactoryAsListener() {
        verify(dbConfigFactory).addListener(argThat(sameInstance(testedProvider)));
    }

    @Test
    public void get_NotCached_ReturnsWrapper() {
        DatabaseWrapper dbWrapper1 = testedProvider.get(SimpleDb.PPCDICT);
        assertThat(dbWrapper1, notNullValue());
    }

    @Test
    public void get_NotCached_ReturnsNewWrapperWhenAnotherCached() {
        DatabaseWrapper dbWrapper1 = testedProvider.get(SimpleDb.PPCDICT);
        DatabaseWrapper dbWrapper2 = testedProvider.get(SimpleDb.CLICKHOUSE_CLOUD);
        assumeThat(dbWrapper1, notNullValue());
        assertThat(dbWrapper2, both(notNullValue()).and(not(sameInstance(dbWrapper1))));
    }

    @Test
    public void get_NotCached_ObtainsValidDbConfigFromFactory() {
        testedProvider.get(SimpleDb.PPCDICT);
        verify(dbConfigFactory).get(SimpleDb.PPCDICT.toString());
    }

    @Test
    public void get_NotCached_ObtainsValidDataSourceFromFactory() {
        testedProvider.get(SimpleDb.PPCDICT);
        verify(dataSourceFactory).createDataSource(argThat(sameInstance(dbConfig)));
    }

    @Test
    public void get_NotCached_ReturnsWrapperWithRefreshableDataSource() {
        DatabaseWrapper dbWrapper = testedProvider.get(SimpleDb.PPCDICT);
        assertThat(dbWrapper.getDataSource(), instanceOf(RefreshableDataSource.class));
    }

    @Test
    public void get_NotCached_ReturnsWrapperWithDataSourceFromFactory() {
        DatabaseWrapper dbWrapper = testedProvider.get(SimpleDb.PPCDICT);
        assumeThat(dbWrapper.getDataSource(), instanceOf(RefreshableDataSource.class));
        assertThat(((RefreshableDataSource) dbWrapper.getDataSource()).getDataSource(),
                sameInstance(dataSource));
    }

    @Test
    public void get_Cached_ReturnsCachedWrapper() {
        DatabaseWrapper dbWrapper = testedProvider.get(SimpleDb.PPCDICT);
        DatabaseWrapper dbWrapperSameInstance = testedProvider.get(SimpleDb.PPCDICT);
        assertThat(dbWrapper, sameInstance(dbWrapperSameInstance));
    }

    @Test
    public void get_Cached_ReturnsCachedWrapperAfterAnotherWasCached() {
        DatabaseWrapper dbWrapper1 = testedProvider.get(SimpleDb.PPCDICT);
        DatabaseWrapper dbWrapper2 = testedProvider.get(SimpleDb.CLICKHOUSE_CLOUD);
        DatabaseWrapper dbWrapper1SameInstance = testedProvider.get(SimpleDb.PPCDICT);
        assumeThat(dbWrapper2, notNullValue());
        assertThat(dbWrapper1, sameInstance(dbWrapper1SameInstance));
    }

    @Test
    public void get_Cached_DoesNotObtainDbConfigFromFactory() {
        testedProvider.get(SimpleDb.PPCDICT);
        testedProvider.get(SimpleDb.PPCDICT);
        verify(dbConfigFactory, times(1)).get(any());
    }

    @Test
    public void get_Cached_DoesNotObtainDataSourceFromFactory() {
        testedProvider.get(SimpleDb.PPCDICT);
        testedProvider.get(SimpleDb.PPCDICT);
        verify(dataSourceFactory, times(1)).createDataSource(any());
    }
}
