package ru.yandex.direct.dbutil.wrapper;

import org.jooq.DSLContext;
import org.jooq.TransactionalRunnable;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DslContextProviderTest {
    private DslContextProvider testedProvider;


    private DSLContext ppcDslContextFirstShard;
    private DatabaseWrapper ppcDatabaseWrapperFirstShard;
    private DSLContext ppcDslContextSecondShard;
    private DatabaseWrapper ppcDatabaseWrapperSecondShard;
    private DSLContext ppcdictDslContext;
    private DatabaseWrapper ppcdictDatabaseWrapper;
    private DSLContext ppclogDslContext;
    private DatabaseWrapper ppclogDatabaseWrapper;
    private DSLContext monitorDslContext;
    private DatabaseWrapper monitorDatabaseWrapper;
    private DatabaseWrapperProvider databaseWrapperProvider;
    private TransactionalRunnable transactionalRunnable;

    @Before
    public void setUp() throws Exception {
        databaseWrapperProvider = mock(DatabaseWrapperProvider.class);

        ppcDslContextFirstShard = mock(DSLContext.class);
        ppcDatabaseWrapperFirstShard = mock(DatabaseWrapper.class);
        when(ppcDatabaseWrapperFirstShard.getDslContext()).thenReturn(ppcDslContextFirstShard);
        when(databaseWrapperProvider.get(ShardedDb.PPC, 1)).thenReturn(ppcDatabaseWrapperFirstShard);

        ppcDslContextSecondShard = mock(DSLContext.class);
        ppcDatabaseWrapperSecondShard = mock(DatabaseWrapper.class);
        when(ppcDatabaseWrapperSecondShard.getDslContext()).thenReturn(ppcDslContextSecondShard);
        when(databaseWrapperProvider.get(ShardedDb.PPC, 2)).thenReturn(ppcDatabaseWrapperSecondShard);

        ppcdictDslContext = mock(DSLContext.class);
        ppcdictDatabaseWrapper = mock(DatabaseWrapper.class);
        when(ppcdictDatabaseWrapper.getDslContext()).thenReturn(ppcdictDslContext);
        when(databaseWrapperProvider.get(SimpleDb.PPCDICT)).thenReturn(ppcdictDatabaseWrapper);

        ppclogDslContext = mock(DSLContext.class);
        ppclogDatabaseWrapper = mock(DatabaseWrapper.class);
        when(ppclogDatabaseWrapper.getDslContext()).thenReturn(ppclogDslContext);
        when(databaseWrapperProvider.get(SimpleDb.PPCLOG)).thenReturn(ppclogDatabaseWrapper);

        monitorDslContext = mock(DSLContext.class);
        monitorDatabaseWrapper = mock(DatabaseWrapper.class);
        when(monitorDatabaseWrapper.getDslContext()).thenReturn(monitorDslContext);
        when(databaseWrapperProvider.get(SimpleDb.MONITOR)).thenReturn(monitorDatabaseWrapper);

        testedProvider = new DslContextProvider(databaseWrapperProvider);

        transactionalRunnable = mock(TransactionalRunnable.class);
    }

    @Test
    public void ppc() {
        DSLContext dslContext1 = testedProvider.ppc(1);
        assertThat(dslContext1, sameInstance(ppcDslContextFirstShard));
        DSLContext dslContext2 = testedProvider.ppc(2);
        assertThat(dslContext2, sameInstance(ppcDslContextSecondShard));
    }

    @Test
    public void ppcdict() {
        DSLContext dslContext = testedProvider.ppcdict();
        assertThat(dslContext, sameInstance(ppcdictDslContext));
    }

    @Test
    public void ppclog() {
        DSLContext dslContext = testedProvider.ppclog();
        assertThat(dslContext, sameInstance(ppclogDslContext));
    }

    @Test
    public void monitor() {
        DSLContext dslContext = testedProvider.monitor();
        assertThat(dslContext, sameInstance(monitorDslContext));
    }

    @Test
    public void ppcTransaction() {
        testedProvider.ppcTransaction(1, transactionalRunnable);
        verify(ppcDslContextFirstShard, atLeast(1)).transaction(any(TransactionalRunnable.class));

        testedProvider.ppcTransaction(2, transactionalRunnable);
        verify(ppcDslContextSecondShard, atLeast(1)).transaction(any(TransactionalRunnable.class));
    }

    @Test
    public void monitorTransaction() {
        testedProvider.monitorTransaction(transactionalRunnable);
        verify(monitorDslContext, atLeast(1)).transaction(any(TransactionalRunnable.class));
    }

}
