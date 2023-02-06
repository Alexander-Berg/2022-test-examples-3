package ru.yandex.market.pers.notify.settings;

import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.test.MockedDbTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 25.12.17
 */
public class SubscriptionAndIdentityDAOTest extends MockedDbTest {
    @Autowired
    private SubscriptionAndIdentityDAO subscriptionAndIdentityDAO;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    public void defaultRepeatableReadReadsFromFirstSnapshot() throws Exception {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
        Uid uid = new Uid(1L);
        Callable<Long> task = () -> transactionTemplate.execute(status -> {
            try {
                cyclicBarrier.await();
                subscriptionAndIdentityDAO.getIdentityId(uid);
                cyclicBarrier.await();
                subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(uid);
                return subscriptionAndIdentityDAO.getIdentityId(uid);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<Long> f1 = pool.submit(task);
        Future<Long> f2 = pool.submit(task);
        assertTrue(f1.get() == null || f2.get() == null);
    }

    @Test
    public void createOrGetWorksFineConcurrently() throws Exception {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
        Uid uid = new Uid(1L);
        Callable<Long> task = () -> transactionTemplate.execute(status -> {
            try {
                cyclicBarrier.await();
                subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(uid);
                return subscriptionAndIdentityDAO.getIdentityId(uid);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<Long> f1 = pool.submit(task);
        Future<Long> f2 = pool.submit(task);
        assertNotNull(f1.get());
        assertNotNull(f2.get());
    }
}
