package ru.yandex.ir.modelsclusterizer.utils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.joda.time.Duration;
import org.joda.time.ReadableDuration;
import org.joda.time.Seconds;
import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;


/**
 * Стырил тут https://arc.yandex-team.ru/wsvn/arc/trunk/arcadia/iceberg/misc/src/main/java/ru/yandex/misc/ProgressTest.java?rev=2363393&peg=2363393
 */
public class ProgressTest {
    private static final Logger logger = LogManager.getLogger();
    public static final int PARTIES = 4;

    private static void sleep(int x) {
        try {
            Thread.sleep(x);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void simple(){
        final CountingFunction calls = new CountingFunction();
        final Progress simple = new Progress("simple", calls, new Duration(1000));
        IntStream.range(1, 22).forEach(integer -> {
            simple.tick();
            sleep(100);
        });
        assertEquals(2, calls.counter.get());
    }

    @Test
    public void mt() throws InterruptedException, ExecutionException {
        final Duration printDuration = new Duration(1000);
        final UniqueFunction calls = new UniqueFunction(printDuration);
        final Progress simple = new Progress("simple", calls, printDuration, new Duration(3000));
        withBarrier(IntStream.range(0, PARTIES).mapToObj(tickerF(simple, 200)).collect(Collectors.toList()), true, Seconds.seconds(5).toStandardDuration());
        assertTrue("Not called", calls.counter.get() > 0);
    }

    @Ignore
    @Test
    public void throttle() {
        final Duration printDuration = new Duration(500);
        final Progress progress = new Progress("hello", logger::info, printDuration, printDuration);
        withBarrier(IntStream.range(0, 3).mapToObj(tickerF(progress.throttle(10), 5)).collect(Collectors.toList()), true, Seconds.seconds(1).toStandardDuration());
        final Double avg = progress.getLastExtendedStatus().averageItemsPerSecond().get();
        assertTrue(avg < 14.0 && avg > 6.0);


        final Progress progress2 = new Progress("hello2", logger::info, printDuration, printDuration);
        withBarrier(IntStream.range(0, 4).mapToObj(tickerF(progress2.throttle(0.5), 4)).collect(Collectors.toList()), true, Seconds.seconds(1).toStandardDuration());
        final Double avg2 = progress2.getLastExtendedStatus().averageItemsPerSecond().orElseThrow(() -> new RuntimeException("fail"));
        assertTrue(avg2 < 1.0 && avg2 > 0.0);
    }

    private IntFunction<Runnable> tickerF(final Progress progress, final int amount) {
        return integer -> () -> IntStream.range(1, amount).forEach(integer1 -> {
            progress.tick();
            sleep(10);
        });
    }

    private IntFunction<Runnable> tickerF(final Progress.Throttle throttle, final int amount) {
        return integer -> () -> IntStream.range(1, amount).forEach(integer1 -> throttle.tick());
    }

    public static void withBarrier(List<Runnable> parallelTasks, final boolean abortOnException, final Duration waitOnBarrier) {
        withBarrier(parallelTasks, abortOnException, waitOnBarrier, Optional.empty());
    }

    public static void withBarrier(List<Runnable> parallelTasks, final boolean abortOnException,
                                   final Duration waitOnBarrier, final Optional<Duration> waitTermination)
    {
        final int nThreads = parallelTasks.size();
        final CyclicBarrier cyclicBarrier = new CyclicBarrier(nThreads);
        final ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        final List<Callable<LastFailHolder>> callables = parallelTasks.stream().map(callback -> new Callable<LastFailHolder>() {
            @Override
            public LastFailHolder call() throws Exception {
                final LastFailHolder lastFailHolder = new LastFailHolder();
                try {
                    logger.info("Waiting for barrier [" + callback + "]");
                    final int pos = cyclicBarrier.await(waitOnBarrier.getMillis(), TimeUnit.MILLISECONDS);
                    logger.info("Calling callback [" + callback + "]");
                    callback.run();
                } catch (RuntimeException ex) {
                    if (abortOnException) {
                        lastFailHolder.exception = Optional.of(ex);
                        throw ex;
                    }
                }
                return lastFailHolder;
            }
        }).collect(Collectors.toList());
        try {
            final List<Future<LastFailHolder>> futures = executor.invokeAll(callables);
            for (Future<LastFailHolder> future : futures) {
                final LastFailHolder lastFailHolder = future.get();
                if (lastFailHolder.exception.isPresent())
                    throw new RuntimeException("Failed to execute task", lastFailHolder.exception.get());
            }
            waitTermination(executor, waitTermination);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            shutdownNowQuietly(executor);
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            shutdownNowQuietly(executor);
            throw new RuntimeException(ex);
        }
    }

    public static <T> BiConsumer<Logger, T> infoF() {
        return (logger1, t) -> logger1.info(t.toString());
    }

    private static class CountingFunction implements Consumer<String> {
        AtomicLong counter = new AtomicLong();

        @Override
        public void accept(String s) {
            counter.incrementAndGet();
            logger.info(s);
        }
    }

    private static class UniqueFunction extends CountingFunction {

        final long duration;
        volatile long lastChange = 0;

        private UniqueFunction(ReadableDuration duration) {
            this.duration = new Duration(duration).getMillis() / 2;
        }

        @Override
        public synchronized void accept(String s) {
            super.accept(s);
            final long now = System.currentTimeMillis();
            if (now - lastChange < duration) {
                fail("duration interval too short: " + s);
            }
            lastChange = now;
        }
    }

    private static void waitTermination(ExecutorService executor, Optional<Duration> waitTermination) throws InterruptedException {
        executor.shutdown();
        final Duration duration = waitTermination.orElse(Seconds.seconds(5).toStandardDuration());
        while (!executor.awaitTermination(duration.getMillis(), TimeUnit.MILLISECONDS)) {
            if (waitTermination.isPresent())
                break;
        }
    }

    private static void shutdownNowQuietly(ExecutorService executor) {
        try {
            List<Runnable> list = executor.shutdownNow();
            logger.debug("Cancelled {} tasks.", list.size());
        } catch (Exception ignored) {
        }
    }

    private static class LastFailHolder {
        public volatile Optional<RuntimeException> exception = Optional.empty();
    }
}