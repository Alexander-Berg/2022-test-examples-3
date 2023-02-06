package ru.yandex.direct.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;

/**
 * Обертка для CompletableFuture, чтобы не повторять код.
 */
public final class AsyncContext {

    private static final int TIMEOUT = 10;
    private final CompletableFuture<AsyncContext> future;

    private AsyncContext(CompletableFuture<AsyncContext> future) {
        this.future = future;
    }

    public static AsyncContext context() {
        return new AsyncContext(new CompletableFuture<>());
    }

    public void complete() {
        try {
            future.complete(this);
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }

    public void await() {
        try {
            future.get(TIMEOUT, TimeUnit.SECONDS);
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }

    public void execute(AsyncAction action) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> handleAsyncAction(action));
    }

    public void assertFalse(boolean flag) {
        handleAsyncAction(() -> Assertions.assertFalse(flag));
    }

    public void assertNotNull(Object obj) {
        handleAsyncAction(() -> Assertions.assertNotNull(obj));
    }

    public void assertEquals(int expected, int actual) {
        handleAsyncAction(() -> Assertions.assertEquals(expected, actual));
    }

    public void assertEquals(String expected, String actual) {
        handleAsyncAction(() -> Assertions.assertEquals(expected, actual));
    }

    public void assertTrue(boolean flag) {
        handleAsyncAction(() -> Assertions.assertTrue(flag));
    }

    private void handleAsyncAction(AsyncAction asyncAssert) {
        try {
            asyncAssert.run();
        } catch (Throwable any) {
            future.completeExceptionally(any);
        }
    }

    public interface AsyncAction {
        void run();
    }
}
