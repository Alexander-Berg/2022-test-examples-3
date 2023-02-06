package ru.yandex.common.cache.memcached;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ru.yandex.common.cache.memcached.impl.TransactionalMemCachedAgent;
import ru.yandex.common.transaction.TransactionListener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class MemCachedTransactionTemplateTest {

    private MemCachedTransactionTemplate transactionTemplate;
    private PlatformTransactionManager tm;
    private TransactionListener listener;
    private TransactionalMemCachedAgent agent;

    @Before
    public void setUp() {
        listener = Mockito.mock(TransactionListener.class);
        agent = Mockito.mock(TransactionalMemCachedAgent.class);
        tm = Mockito.mock(PlatformTransactionManager.class);
        when(tm.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        transactionTemplate = new MemCachedTransactionTemplate();
        transactionTemplate.setTransactionalMemCachedAgent(agent);
        transactionTemplate.setTransactionManager(tm);
        transactionTemplate.afterPropertiesSet();
        transactionTemplate.addListener(listener);
    }

    @Test
    public void testExecuteWithCommit() {
        TransactionCallback callback = Mockito.mock(TransactionCallback.class);
        transactionTemplate.execute(callback);

        InOrder inOrder = inOrder(tm, listener, agent, callback);

        inOrder.verify(agent).beginTransaction();
        inOrder.verify(listener).onBeforeTransaction(any());
        inOrder.verify(callback).doInTransaction(any());
        inOrder.verify(listener).onBeforeCommit(any());
        inOrder.verify(tm).commit(any());
        inOrder.verify(agent).endTransaction(true);
        inOrder.verify(listener).onAfterCommit(any());
        inOrder.verify(listener).onAfterTransaction(any());
    }

    @Test
    public void testExecuteWithRollback() {
        TransactionCallback callback = Mockito.mock(TransactionCallback.class);
        when(callback.doInTransaction(any())).thenThrow(new RuntimeException());

        Exception thrown = null;

        try {
            transactionTemplate.execute(callback);
        } catch (Exception e) {
            thrown = e;
        }

        Assert.assertNotNull(thrown);

        InOrder inOrder = inOrder(tm, listener, agent, callback);

        inOrder.verify(agent).beginTransaction();
        inOrder.verify(listener).onBeforeTransaction(any());
        inOrder.verify(callback).doInTransaction(any());
        inOrder.verify(listener).onBeforeRollback(any());
        inOrder.verify(tm).rollback(any());
        inOrder.verify(agent).endTransaction(false);
        inOrder.verify(listener).onAfterRollback(any());
        inOrder.verify(listener).onAfterTransaction(any());
    }

    /**
     * При ошибке в слушателе ДО коммита, должен выполнятся откат транзакции со всеми соответствующими слушателями
     */
    @Test
    public void testExecuteWithBadBeforeCommitListener() {
        TransactionCallback callback = Mockito.mock(TransactionCallback.class);
        transactionTemplate.addListener(new TransactionListener() {
            @Override
            public void onBeforeCommit(TransactionStatus status) {
                throw new RuntimeException();
            }
        });

        try {
            transactionTemplate.execute(callback);
        } catch (Exception e) {
            // пропускаем
        }

        InOrder inOrder = inOrder(tm, listener, agent, callback);

        inOrder.verify(agent).beginTransaction();
        inOrder.verify(listener).onBeforeTransaction(any());
        inOrder.verify(callback).doInTransaction(any());
        inOrder.verify(listener).onBeforeCommit(any());
        inOrder.verify(listener).onBeforeRollback(any());
        inOrder.verify(tm).rollback(any());
        inOrder.verify(agent).endTransaction(false);
        inOrder.verify(listener).onAfterRollback(any());
        inOrder.verify(listener).onAfterTransaction(any());
    }

    /**
     * При ошибке в слушателе ПОСЛЕ коммита, всё равно должны выполняться все завершающие слушатели,
     * т.к. они выполняют возврат к исходному состоянию (очистка мемкеша после успешной фиксации транзакции)
     */
    @Test
    public void testExecuteWithBadAfterCommitListener() {
        TransactionCallback callback = Mockito.mock(TransactionCallback.class);
        transactionTemplate.addListener(new TransactionListener() {
            @Override
            public void onAfterCommit(TransactionStatus status) {
                throw new RuntimeException();
            }
        });

        try {
            transactionTemplate.execute(callback);
        } catch (Exception e) {
            // пропускаем
        }

        InOrder inOrder = inOrder(tm, listener, agent, callback);

        inOrder.verify(agent).beginTransaction();
        inOrder.verify(listener).onBeforeTransaction(any());
        inOrder.verify(callback).doInTransaction(any());
        inOrder.verify(listener).onBeforeCommit(any());
        inOrder.verify(tm).commit(any());
        inOrder.verify(agent).endTransaction(true);
        inOrder.verify(listener).onAfterCommit(any());
        inOrder.verify(listener).onAfterTransaction(any());
    }
}