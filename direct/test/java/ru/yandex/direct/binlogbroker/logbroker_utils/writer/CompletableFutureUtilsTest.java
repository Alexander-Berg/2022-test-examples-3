package ru.yandex.direct.binlogbroker.logbroker_utils.writer;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.direct.binlogbroker.logbroker_utils.writer.CompletableFutureUtils.wrapCompletableFutureToRetry;

public class CompletableFutureUtilsTest {

    private static final Duration SLEEP_TIME = Duration.ofMillis(0);


    @Test
    public void testWrapCompletableFutureToRetry_SuccessFuture() throws ExecutionException, InterruptedException {
        int retryCount = 3;
        AtomicInteger callCnt = new AtomicInteger(0);
        Supplier<Integer> supplier = callCnt::incrementAndGet;
        AtomicLong exceptionCnt = new AtomicLong();

        Supplier<CompletableFuture<Void>> exceptionAction =
                () -> CompletableFuture.runAsync(exceptionCnt::incrementAndGet);
        Supplier<CompletableFuture<Integer>> completableFutureSupplier = () -> CompletableFuture.supplyAsync(supplier);
        CompletableFuture<Integer> retryingCompletableFuture =
                wrapCompletableFutureToRetry(completableFutureSupplier, exceptionAction, retryCount,
                        SLEEP_TIME);
        retryingCompletableFuture.get();
        assertThat(callCnt.get()).isEqualTo(1);
        assertThat(exceptionCnt.get()).isEqualTo(0);
    }

    @Test
    public void testWrapCompletableFutureToRetry_FailureFuture() {
        int retryCount = 3;
        AtomicInteger callCnt = new AtomicInteger(0);
        AtomicLong exceptionCnt = new AtomicLong();
        Supplier<CompletableFuture<Void>> exceptionAction =
                () -> CompletableFuture.runAsync(exceptionCnt::incrementAndGet);

        Supplier<Integer> supplier = () -> {
            callCnt.incrementAndGet();
            throw new IllegalStateException("failure supplier");
        };
        Supplier<CompletableFuture<Integer>> completableFutureSupplier =
                () -> CompletableFuture.supplyAsync(supplier);


        CompletableFuture<Integer> retryingCompletableFuture =
                wrapCompletableFutureToRetry(completableFutureSupplier, exceptionAction, retryCount,
                        SLEEP_TIME);

        assertThatThrownBy(retryingCompletableFuture::get).isInstanceOf(ExecutionException.class);
        assertThat(callCnt.get()).isEqualTo(retryCount + 1);
        assertThat(exceptionCnt.get()).isEqualTo(retryCount);
    }

    @Test
    public void testWrapCompletableFutureToRetry_SeveralTimeFailureFuture()
            throws ExecutionException, InterruptedException {
        int retryCount = 3;
        AtomicInteger failureCount = new AtomicInteger(2);
        AtomicInteger callCnt = new AtomicInteger(0);
        Supplier<Integer> supplier = () -> {
            int currentCAllCnt = callCnt.incrementAndGet();
            int failureAttempts = failureCount.getAndDecrement();
            if (failureAttempts > 0) {
                throw new IllegalStateException("failure supplier");
            } else {
                return currentCAllCnt;
            }
        };
        AtomicLong exceptionCnt = new AtomicLong();
        Supplier<CompletableFuture<Void>> exceptionAction =
                () -> CompletableFuture.runAsync(exceptionCnt::incrementAndGet);

        Supplier<CompletableFuture<Integer>> completableFutureSupplier = () -> CompletableFuture.supplyAsync(supplier);
        CompletableFuture<Integer> retryingCompletableFuture =
                wrapCompletableFutureToRetry(completableFutureSupplier, exceptionAction, retryCount,
                        SLEEP_TIME);
        retryingCompletableFuture.get();
        assertThat(callCnt.get()).isEqualTo(3);
        assertThat(exceptionCnt.get()).isEqualTo(2);
    }

    @Test
    public void testWrapCompletableFutureToRetry_OneTimeFailureFuture()
            throws ExecutionException, InterruptedException {
        int retryCount = 3;
        AtomicInteger failureCount = new AtomicInteger(1);
        AtomicInteger callCnt = new AtomicInteger(0);
        AtomicLong exceptionCnt = new AtomicLong();
        Supplier<CompletableFuture<Void>> exceptionAction =
                () -> CompletableFuture.runAsync(exceptionCnt::incrementAndGet);

        Supplier<Integer> supplier = () -> {
            int currentCAllCnt = callCnt.incrementAndGet();
            int failureAttempts = failureCount.getAndDecrement();
            if (failureAttempts > 0) {
                throw new IllegalStateException("failure supplier");
            } else {
                return currentCAllCnt;
            }
        };
        Supplier<CompletableFuture<Integer>> completableFutureSupplier = () -> CompletableFuture.supplyAsync(supplier);
        CompletableFuture<Integer> retryingCompletableFuture =
                wrapCompletableFutureToRetry(completableFutureSupplier, exceptionAction, retryCount,
                        SLEEP_TIME);
        retryingCompletableFuture.get();
        assertThat(callCnt.get()).isEqualTo(2);
        assertThat(exceptionCnt.get()).isEqualTo(1);
    }

    @Test
    public void testWrapCompletableFutureToRetry_ZeroRetries_Success() throws ExecutionException, InterruptedException {
        int retryCount = 0;
        AtomicInteger callCnt = new AtomicInteger(0);
        Supplier<Integer> supplier = callCnt::incrementAndGet;
        AtomicLong exceptionCnt = new AtomicLong();
        Supplier<CompletableFuture<Void>> exceptionAction =
                () -> CompletableFuture.runAsync(exceptionCnt::incrementAndGet);
        Supplier<CompletableFuture<Integer>> completableFutureSupplier = () -> CompletableFuture.supplyAsync(supplier);
        CompletableFuture<Integer> retryingCompletableFuture =
                wrapCompletableFutureToRetry(completableFutureSupplier, exceptionAction, retryCount,
                        SLEEP_TIME);
        retryingCompletableFuture.get();
        assertThat(callCnt.get()).isEqualTo(1);
        assertThat(exceptionCnt.get()).isEqualTo(0);
    }

    @Test
    public void testWrapCompletableFutureToRetry_ZeroRetries_Failure() {
        int retryCount = 0;
        AtomicInteger callCnt = new AtomicInteger(0);
        Supplier<Integer> supplier = () -> {
            callCnt.incrementAndGet();
            throw new IllegalStateException("failure supplier");
        };
        Supplier<CompletableFuture<Integer>> completableFutureSupplier =
                () -> CompletableFuture.supplyAsync(supplier);
        AtomicLong exceptionCnt = new AtomicLong();
        Supplier<CompletableFuture<Void>> exceptionAction =
                () -> CompletableFuture.runAsync(exceptionCnt::incrementAndGet);

        CompletableFuture<Integer> retryingCompletableFuture =
                wrapCompletableFutureToRetry(completableFutureSupplier, exceptionAction, retryCount,
                        SLEEP_TIME);

        assertThatThrownBy(retryingCompletableFuture::get).isInstanceOf(ExecutionException.class);
        assertThat(callCnt.get()).isEqualTo(1);
        assertThat(exceptionCnt.get()).isEqualTo(0);
    }

    @Test
    public void testWrapCompletableFutureToRetry_InvalidRetiesCount() {
        int retryCount = -1;

        Supplier<Integer> supplier = () -> 0;
        Supplier<CompletableFuture<Void>> exceptionAction =
                () -> CompletableFuture.completedFuture(null);
        Supplier<CompletableFuture<Integer>> completableFutureSupplier = () -> CompletableFuture.supplyAsync(supplier);
        assertThatThrownBy(
                () -> wrapCompletableFutureToRetry(completableFutureSupplier, exceptionAction, retryCount, SLEEP_TIME))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
