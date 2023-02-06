package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.mbi.lock.LockService;
import ru.yandex.market.shop.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ReleaseLocksCommandTest extends FunctionalTest {
    static final String LOCK_NAME = ReleaseLocksCommandTest.class.getSimpleName();

    @Autowired
    LockService lockService;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    Terminal terminal;
    @Autowired
    ReleaseLocksCommand tested;

    @BeforeEach
    void beforeEach() {
        when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
    }

    @Test
    void testReleaseLocks() {
        var executor = Executors.newSingleThreadExecutor();
        var testComplete = new CountDownLatch(1);
        var lockIsLocked = new CountDownLatch(1);

        try {
            // открываем первую транзакцию
            transactionTemplate.execute(status -> {
                // запускаем параллельно вторую транзакцию в другом потоке,
                // чтобы убедиться что мы будем висеть именно на локе, а не на ожидании коннекта
                var lockedF = executor.submit(() -> transactionTemplate.execute(status2 -> {
                    var locked = lockService.tryLockWithinTx(LOCK_NAME);
                    assertThat(locked)
                            .as("nobody should hold the lock yet")
                            .isTrue();
                    lockIsLocked.countDown();
                    await(testComplete);
                    return locked;
                }));

                await(lockIsLocked);
                assertThat(lockService.tryLockWithinTx(LOCK_NAME))
                        .as("ensure that lock is locked")
                        .isFalse();

                tested.executeCommand(new CommandInvocation(
                        ReleaseLocksCommand.COMMAND_NAME,
                        new String[]{LOCK_NAME},
                        Map.of()
                ), terminal);
                assertThat(lockService.tryLockWithinTx(LOCK_NAME))
                        .as("tx-scoped locks can't be unlocked in PostgreSQL")
                        .isFalse();
                testComplete.countDown();
                assertThat(lockedF)
                        .succeedsWithin(10L, TimeUnit.SECONDS)
                        .isEqualTo(true);
                return null;
            });
        } finally {
            executor.shutdownNow();
        }
    }

    static void await(CountDownLatch l) {
        try {
            assertThat(l.await(10L, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
