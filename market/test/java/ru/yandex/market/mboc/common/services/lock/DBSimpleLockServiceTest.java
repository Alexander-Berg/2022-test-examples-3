package ru.yandex.market.mboc.common.services.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.db.config.JooqConfig;

public class DBSimpleLockServiceTest extends BaseDbTestClass {

    private static int SHOP_ID = 1;

    private static final String LOCK_1 = "lock 1";
    private static final String LOCK_2 = "lock 2";

    @Autowired
    public TransactionHelper transactionHelper;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    public JooqConfig jooqConfig;

    private ExecutorService executorService;

    public DBSimpleLockService simpleLockService;

    @Before
    public void setUp() {
        supplierRepository.insert(new Supplier(SHOP_ID, "Test"));
        simpleLockService = new DBSimpleLockService(transactionHelper, jooqConfig.dsl());
        executorService = Executors.newFixedThreadPool(2);
    }

    @After
    public void tearDown() throws InterruptedException {
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    public void testAcquireSameLock() throws InterruptedException {
        CountDownLatch action1Started = new CountDownLatch(1);
        CountDownLatch action3Started = new CountDownLatch(1);
        CountDownLatch actionFinish = new CountDownLatch(1);
        CountDownLatch mainLatch = new CountDownLatch(1);

        executorService.submit(() -> simpleLockService.doWithLock(LOCK_1, () -> {
            try {
                action1Started.countDown();
                actionFinish.await(10, TimeUnit.SECONDS);
                mainLatch.countDown();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));

        // LOCK_1 successfully acquired
        action1Started.await(1, TimeUnit.SECONDS);

        // check LOCK_1 cannot be acquired twice
        Assertions.assertThatThrownBy(() -> simpleLockService.doWithLock(LOCK_1, () -> {
        }))
            .isExactlyInstanceOf(LockException.class)
            .hasMessageContaining("Failed to obtain lock " + LOCK_1);

        actionFinish.countDown();
        mainLatch.await(1, TimeUnit.SECONDS);

        executorService.submit(() -> simpleLockService.doWithLock(LOCK_1, action3Started::countDown));

        // LOCK_1 successfully acquired again after release
        action1Started.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testAcquireDifferentLocks() throws InterruptedException {
        CountDownLatch action1Started = new CountDownLatch(1);
        CountDownLatch action2Started = new CountDownLatch(1);
        CountDownLatch actionFinish = new CountDownLatch(1);
        CountDownLatch mainLatch = new CountDownLatch(2);

        executorService.submit(() -> simpleLockService.doWithLock(LOCK_1, () -> {
            try {
                action1Started.countDown();
                actionFinish.await(10, TimeUnit.SECONDS);
                mainLatch.countDown();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));

        // LOCK_1 successfully acquired
        action1Started.await(1, TimeUnit.SECONDS);

        executorService.submit(() -> simpleLockService.doWithLock(LOCK_2, () -> {
            try {
                action2Started.countDown();
                actionFinish.await(10, TimeUnit.SECONDS);
                mainLatch.countDown();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));

        // LOCK_2 successfully acquired
        action2Started.await(1, TimeUnit.SECONDS);

        actionFinish.countDown();
        mainLatch.await(1, TimeUnit.SECONDS);
    }
}
