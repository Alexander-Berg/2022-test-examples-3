package ru.yandex.market.yt.utils;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import ru.yandex.misc.thread.ThreadUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallableRetryTest {
    @Timeout(2)
    @Test
    void testInterrupted() throws InterruptedException {
        var retry = new CallableRetry(null, null, 3, null);

        var exception = new AtomicReference<>();
        var interrupted = new AtomicBoolean();

        var thread = new Thread(() -> {
            try {
                retry.retry(() -> {
                            ThreadUtils.sleep(5, TimeUnit.SECONDS);
                            return CompletableFuture.failedFuture(new RuntimeException("Need retry"));
                        }
                );
            } catch (Exception e) {
                exception.set(e);
            } finally {
                interrupted.set(Thread.currentThread().isInterrupted());
            }
        });
        thread.start();
        Thread.sleep(100);

        // retry получит сигнал и прекратит попытки
        thread.interrupt();
        thread.join();

        assertNotNull(exception.get());
        assertTrue(interrupted.get());
    }

    @Timeout(2)
    @Test
    void testTimeout() throws InterruptedException {
        var retry = new CallableRetry(null, null, 50, Duration.ofMillis(250));

        var exception = new AtomicReference<>();

        var thread = new Thread(() -> {
            try {
                retry.retry(() -> {
                            ThreadUtils.sleep(250, TimeUnit.MILLISECONDS);
                            return CompletableFuture.failedFuture(new RuntimeException("Need retry"));
                        }
                );
            } catch (Exception e) {
                exception.set(e);
            }
        });
        thread.start();

        // Автоматически завершится после первой попытки
        thread.join();

        assertNotNull(exception.get());
    }

}
