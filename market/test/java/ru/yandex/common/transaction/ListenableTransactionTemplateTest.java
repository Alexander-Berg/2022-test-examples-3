package ru.yandex.common.transaction;

import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.CallbackPreferringPlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ListenableTransactionTemplateTest {
    TransactionListener tl = mock(TransactionListener.class);
    ListenableTransactionTemplate tt = new ListenableTransactionTemplate();

    @Test
    public void wrapPlatformTransactionManagerCommit() {
        // given
        tt.setTransactionManager(new StubPlatformTransactionManager());
        tt.afterPropertiesSet();
        tt.addListener(tl);

        // when
        tt.execute(status -> null);

        // then
        verify(tl).onBeforeTransaction(any());
        verify(tl).onBeforeCommit(any());
        verify(tl).onAfterCommit(any());
        verify(tl).onAfterTransaction(any());
        verifyNoMoreInteractions(tl);
    }

    @Test
    public void wrapPlatformTransactionManagerRollback() {
        // given
        tt.setTransactionManager(new StubPlatformTransactionManager());
        tt.afterPropertiesSet();
        tt.addListener(tl);

        // when
        try {
            tt.execute(status -> {
                throw new RuntimeException("boom");
            });
            fail("should not get here");
        } catch (RuntimeException ignored) {
        }


        // then
        verify(tl).onBeforeTransaction(any());
        verify(tl).onBeforeRollback(any());
        verify(tl).onAfterRollback(any());
        verify(tl).onAfterTransaction(any());
        verifyNoMoreInteractions(tl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrapCallbackPreferringPlatformTransactionManager() {
        // given
        tt.setTransactionManager(mock(CallbackPreferringPlatformTransactionManager.class));

        // when
        tt.afterPropertiesSet();
    }

    @Test
    public void wrapCallbackBasedTransactionManagerMixinCommit() {
        // given
        tt.setTransactionManager(new StubCallbackBasedTransactionManagerMixin());
        tt.afterPropertiesSet();
        tt.addListener(tl);

        // when
        tt.execute(status -> null);

        // then
        verify(tl).onBeforeTransaction(any());
        verify(tl).onBeforeCommit(any());
        verify(tl).onAfterCommit(any());
        verify(tl).onAfterTransaction(any());
        verifyNoMoreInteractions(tl);
    }

    @Test
    public void wrapCallbackBasedTransactionManagerMixinRollback() {
        // given
        tt.setTransactionManager(new StubCallbackBasedTransactionManagerMixin());
        tt.afterPropertiesSet();
        tt.addListener(tl);

        // when
        try {
            tt.execute(status -> {
                throw new RuntimeException("boom");
            });
            fail("should not get here");
        } catch (RuntimeException ignored) {
        }

        // then
        verify(tl).onBeforeTransaction(any());
        verify(tl).onBeforeRollback(any());
        verify(tl).onAfterRollback(any());
        verify(tl).onAfterTransaction(any());
        verifyNoMoreInteractions(tl);
    }

    private static class StubPlatformTransactionManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            status.setRollbackOnly();
        }

        @Override
        public void rollback(TransactionStatus status) {
            status.setRollbackOnly();
        }
    }

    private static class StubCallbackBasedTransactionManagerMixin
            extends StubPlatformTransactionManager
            implements CallbackBasedTransactionManagerMixin {
        @Override
        public <T> T execute(TransactionDefinition definition, TransactionCallback<T> callback) {
            return execute(this, definition, callback);
        }

        @Override
        public <T> T execute(
                PlatformTransactionManager self,
                TransactionDefinition definition,
                TransactionCallback<T> callback
        ) {
            return CallbackBasedTransactionManagerMixin.doExecute(self, definition, callback);
        }
    }
}
