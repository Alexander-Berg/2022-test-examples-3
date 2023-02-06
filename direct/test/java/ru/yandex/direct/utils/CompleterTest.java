package ru.yandex.direct.utils;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("squid:S2925") // сложно тестировать таймауты без Thread.sleep()
public class CompleterTest {
    @Test
    public void test() throws InterruptedException, ExecutionException, BrokenBarrierException {
        CyclicBarrier start = new CyclicBarrier(3);
        Completer.CompleterException exc = null;
        Completer.Builder builder = new Completer.Builder(Duration.ofSeconds(4));

        Future<Long> fail = builder.submit("sleep", () -> {
            Thread.sleep(100000);
            return 0L;
        });

        Future<String> ok = builder.submit("ok", () -> {
            start.await();
            ThreadUtils.sleepUninterruptibly(Duration.ofSeconds(2));
            return "ok";
        });

        builder.submitVoid("uninterruptible sleep", () -> {
            start.await();
            ThreadUtils.sleepUninterruptibly(Duration.ofSeconds(10));
        });

        builder.submitVoid("noop", () -> {
        });

        try (Completer completer = builder.build()) {
            start.await();
            completer.waitAll(Duration.ofSeconds(1));
        } catch (Completer.CompleterException e) {
            exc = e;
        }

        // проверка результата ok
        Assert.assertEquals("ok", ok.get());

        // проверка результата fail
        ExecutionException failExc = null;
        try {
            fail.get();
        } catch (ExecutionException e) {
            failExc = e;
        }
        Assert.assertNotNull(failExc);
        Assert.assertThat(failExc.getCause(), IsInstanceOf.instanceOf(InterruptedException.class));
        Assert.assertEquals(0, failExc.getSuppressed().length);

        // проверка исключения из Completer.close
        Assert.assertNotNull(exc);
        Assert.assertEquals("Some tasks have failed", exc.getMessage());
        Assert.assertNull(exc.getCause());
        Assert.assertThat(exc, IsInstanceOf.instanceOf(Completer.CompleterException.class));
        Assert.assertEquals(1, exc.getSuppressed().length);

        // проверка подавленного исключения из Completer.close
        Throwable supressed = exc.getSuppressed()[0];
        Assert.assertNull(supressed.getCause());
        Assert.assertEquals(0, supressed.getSuppressed().length);
        Assert.assertEquals(
                "Task 'uninterruptible sleep' is still running, interruption is timed out (timeout=4s)",
                supressed.getMessage());
    }

    @Test
    public void testEarlyException() throws InterruptedException {
        Completer.CompleterException exc = null;
        Completer.Builder builder = new Completer.Builder(Duration.ofSeconds(1));

        builder.submitVoid("error", () -> {
            throw new IOException("io");
        });
        builder.submitVoid("sleep", () -> Thread.sleep(100000));

        try (Completer completer = builder.build()) {
            completer.waitAll(Duration.ofSeconds(10));
        } catch (Completer.CompleterException e) {
            exc = e;
        }

        Assert.assertNotNull(exc);
        Assert.assertThat(exc, IsInstanceOf.instanceOf(Completer.CompleterException.class));
        Assert.assertEquals("Failed task 'error'", exc.getMessage());

        Assert.assertNotNull(exc.getCause());
        Assert.assertThat(exc.getCause(), IsInstanceOf.instanceOf(IOException.class));
        Assert.assertEquals("io", exc.getCause().getMessage());
        Assert.assertEquals(1, exc.getSuppressed().length);
    }

    @Test
    public void testCancelBeforeCall() throws InterruptedException {
        Completer.Builder builder = new Completer.Builder(Duration.ofSeconds(10));
        Future<Void> sleep = builder.submitVoid("sleep", () -> Thread.sleep(100000));

        sleep.cancel(true);
        try (Completer completer = builder.build()) {
            Assert.assertTrue(completer.waitAll(Duration.ofSeconds(10)));
        }
    }

    @Test
    public void testCancelAfterCall() throws InterruptedException, BrokenBarrierException {
        CyclicBarrier start = new CyclicBarrier(2);
        Completer.Builder builder = new Completer.Builder(Duration.ofSeconds(5));
        Future<Void> sleep = builder.submitVoid("sleep", () -> {
            start.await();
            try {
                Thread.sleep(100000);
            } catch (InterruptedException exc) {
                // имитируем остановку с задержкой, чтобы протестировать, что Completer.close()
                // корректно дожидается завершения задач.
                ThreadUtils.sleepUninterruptibly(Duration.ofSeconds(1));
            }
        });

        try (Completer completer = builder.build()) {
            start.await();
            sleep.cancel(true);
            Assert.assertTrue(completer.waitAll(Duration.ofSeconds(10)));
        }
    }

    @Test
    public void testFailedShutdown() throws InterruptedException, BrokenBarrierException {
        Completer.CompleterException exc = null;
        CyclicBarrier start = new CyclicBarrier(2);
        Completer.Builder builder = new Completer.Builder(Duration.ofSeconds(5));

        builder.submitVoid("sleep", () -> {
            start.await();
            try {
                Thread.sleep(100000);
            } catch (InterruptedException e) {
                e.addSuppressed(new IllegalStateException("failure"));
                throw e;
            }
        });

        try (Completer completer = builder.build()) {
            start.await();
            Assert.assertFalse(completer.waitAll(Duration.ofSeconds(1)));
        } catch (Completer.CompleterException e) {
            exc = e;
        }

        Assert.assertNotNull(exc);
        Assert.assertThat(exc, IsInstanceOf.instanceOf(Completer.CompleterException.class));
        Assert.assertEquals("Some tasks have failed", exc.getMessage());
        Assert.assertEquals(1, exc.getSuppressed().length);

        Throwable suppressed = exc.getSuppressed()[0];
        Assert.assertThat(suppressed, IsInstanceOf.instanceOf(Completer.CompleterException.class));
        Assert.assertEquals(
                "Task 'sleep' is failed when interrupted",
                suppressed.getMessage());
    }

    @Test
    public void testLongCancel() throws InterruptedException, BrokenBarrierException {
        Completer.CompleterException exc = null;
        CyclicBarrier start = new CyclicBarrier(2);
        Completer.Builder builder = new Completer.Builder(Duration.ofSeconds(1));

        Future<Void> sleep = builder.submitVoid("sleep", () -> {
            start.await();
            ThreadUtils.sleepUninterruptibly(Duration.ofSeconds(5));
        });

        try (Completer completer = builder.build()) {
            start.await();
            sleep.cancel(true);
            Assert.assertTrue(completer.waitAll(Duration.ofSeconds(1)));
        } catch (Completer.CompleterException e) {
            exc = e;
        }

        Assert.assertNotNull(exc);
        Assert.assertThat(exc, IsInstanceOf.instanceOf(Completer.CompleterException.class));
        Assert.assertEquals("Some tasks have failed", exc.getMessage());
        Assert.assertEquals(1, exc.getSuppressed().length);

        Throwable suppressed = exc.getSuppressed()[0];
        Assert.assertThat(suppressed, IsInstanceOf.instanceOf(Completer.CompleterException.class));
        Assert.assertEquals(
                "Cancelled task 'sleep' is still running, interruption is timed out (timeout=1s)",
                suppressed.getMessage());
    }
}
