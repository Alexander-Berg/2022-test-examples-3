package ru.yandex.market.mbi.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public interface DbmsLockServiceTestBase {
    String LOCK_NAME = DbmsLockServiceTestBase.class.getSimpleName();

    DbmsLockService lockService();

    TransactionTemplate transactionTemplate();

    @Test
    default void lockRequiresTx() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> lockService().lock(LOCK_NAME))
                .withMessageContaining("transaction");
    }

    @Test
    default void lockIsReentrant() {
        transactionTemplate().execute(status -> {
            lockService().lock(LOCK_NAME);
            lockService().lock(LOCK_NAME);
            return null;
        });
    }

    @Test
    default void lockWorksConcurrently() {
        var executor = Executors.newSingleThreadExecutor();
        var secondTxReady = new CountDownLatch(1);
        var lockIsLocked = new CountDownLatch(1);

        try {
            // открываем первую транзакцию
            var locked2FOuter = transactionTemplate().execute(status -> {
                // запускаем параллельно вторую транзакцию в другом потоке,
                // чтобы убедиться что мы будем висеть именно на локе, а не на ожидании коннекта
                var locked2F = executor.submit(() -> transactionTemplate().execute(status2 -> {
                    secondTxReady.countDown();
                    await(lockIsLocked);
                    lockService().lock(LOCK_NAME);
                    return true;
                }));

                await(secondTxReady);
                lockService().lock(LOCK_NAME);
                lockIsLocked.countDown();
                try {
                    locked2F.get(2L, TimeUnit.SECONDS); // должно хватить для попытки взятия лока
                    return fail("should wait until tx finishes");
                } catch (InterruptedException | ExecutionException e) {
                    return fail("should not get here", e);
                } catch (TimeoutException e) {
                    return locked2F; // должны попасть сюда
                }
            });
            // после закрытия первой транзакции должна отработать вторая до конца с вновь полученным локом
            assertThat(locked2FOuter)
                    .isNotNull()
                    .succeedsWithin(10L, TimeUnit.SECONDS)
                    .as("other threads should wait forever until lock is released")
                    .isEqualTo(true);
        } finally {
            executor.shutdownNow();
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 500L, Long.MAX_VALUE})
    default void tryLockWithinTxRequiresTx(long waitTimeoutMsec) {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> lockService().tryLockWithinTx(LOCK_NAME, waitTimeoutMsec))
                .withMessageContaining("transaction")
                .as("lock with releaseOnCommit requires tx");
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 500L, Long.MAX_VALUE})
    default void tryLockWithinTxIsReentrant(long waitTimeoutMsec) {
        var result = transactionTemplate().execute(status -> {
            assertThat(lockService().tryLockWithinTx(LOCK_NAME, waitTimeoutMsec)).isTrue();
            assertThat(lockService().tryLockWithinTx(LOCK_NAME, waitTimeoutMsec))
                    .as("tryLockWithinTx is reentrant")
                    .isTrue();
            return 1;
        });
        assertThat(result).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 500L}) // без Long.MAX_VALUE, иначе тест не закончится
    default void tryLockWithinTxWorksConcurrently(long waitTimeoutMsec) {
        var executor = Executors.newSingleThreadExecutor();
        var secondTxReady = new CountDownLatch(1);
        var lockIsLocked = new CountDownLatch(1);

        try {
            // открываем первую транзакцию
            transactionTemplate().execute(status -> {
                // запускаем параллельно вторую транзакцию в другом потоке,
                // чтобы убедиться что мы будем висеть именно на локе, а не на ожидании коннекта
                var locked2F = executor.submit(() -> transactionTemplate().execute(status2 -> {
                    secondTxReady.countDown();
                    await(lockIsLocked);
                    return lockService().tryLockWithinTx(LOCK_NAME, waitTimeoutMsec);
                }));

                await(secondTxReady);
                assertThat(lockService().tryLockWithinTx(LOCK_NAME, waitTimeoutMsec)).isTrue();
                lockIsLocked.countDown();
                assertThat(locked2F)
                        .succeedsWithin(10L, TimeUnit.SECONDS)
                        .as("other threads should not succeed in locking")
                        .isEqualTo(false);
                return null;
            });
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    default void releaseLock() {
        lockService().releaseLock(LOCK_NAME); // should not fail
        lockService().releaseLock(LOCK_NAME); // one more time
    }

    default void await(CountDownLatch l) {
        try {
            assertThat(l.await(10L, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
