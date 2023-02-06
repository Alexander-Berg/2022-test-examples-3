package ru.yandex.chemodan.test;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;
import ru.yandex.misc.test.Assert;

import static org.junit.Assert.fail;

public class ConcurrentTestUtils {
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentTestUtils.class);

    public static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    public static void testConcurrency(Supplier<Assert.Block> jobGenerator) {
        testConcurrency(cpus -> Stream.generate(jobGenerator)
                .limit(cpus)
                .collect(Collectors.toList())
        );
    }

    public static void testConcurrency(Function<Integer, List<Assert.Block>> jobGenerator) {
        testConcurrency(jobGenerator.apply(CPU_COUNT));
    }

    public static void testConcurrency(List<Assert.Block> jobs) {
        ExecutorService executor = Executors.newFixedThreadPool(CPU_COUNT);
        AtomicReference<Throwable> exception = new AtomicReference<>(null);
        CyclicBarrier barrier = new CyclicBarrier(jobs.size());
        logger.info("testConcurrency {} jobs on {} cpus", jobs.size(), CPU_COUNT);
        for (Assert.Block job : jobs) {
            executor.submit(() -> {
                try {
                    barrier.await();
                    job.execute();
                } catch (Throwable e) {
                    logger.error("exception in job", e);
                    exception.set(e);
                }
            });
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                executor.shutdownNow();
                fail();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (exception.get() != null) {
            logger.error("", exception.get());
            throw new AssertionError(
                    "jobsCount: " + jobs.size() + " errorMessage: " + exception.get().getMessage(),
                    exception.get()
            );
        }
    }
}
