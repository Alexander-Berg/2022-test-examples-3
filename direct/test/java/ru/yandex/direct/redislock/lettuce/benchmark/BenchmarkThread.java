package ru.yandex.direct.redislock.lettuce.benchmark;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.direct.redislock.DistributedLockException;
import ru.yandex.direct.redislock.lettuce.LettuceLock;
import ru.yandex.direct.redislock.lettuce.LettuceLockBuilder;

public class BenchmarkThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(LettuceLockBuilder.class);

    private final LettuceLockBuilder factory;
    private final int requestCount;
    private final int upperBound;
    private final CyclicBarrier barrier;
    private long processingTime;
    private long singleMaxTime;
    private long singleMinTime;

    public BenchmarkThread(LettuceLockBuilder factory, int requestCount, int upperBound, CyclicBarrier barrier) {
        this.factory = factory;
        this.barrier = barrier;
        this.requestCount = requestCount;
        this.upperBound = upperBound;
    }

    @Override
    public void run() {
        try {
            barrier.await();
            long startTime = System.currentTimeMillis();
            singleMaxTime = 0;
            singleMinTime = Long.MAX_VALUE;
            Random rnd = new Random();
            final Stopwatch stopwatch = Stopwatch.createUnstarted();
            for (int i = 0; i < requestCount; ++i) {
                String keyPrefix = String.valueOf(rnd.nextInt(upperBound));
                LettuceLock lck = null;
                stopwatch.start();
                try {
                    lck = factory.createLock(keyPrefix);
                    lck.tryLock();
                } catch (Exception ex) {
                    logger.error("cant lock", ex);
                } finally {
                    if (lck != null) {
                        try {
                            lck.unlock();
                        } catch (DistributedLockException ex) {
                            logger.error("can't unlock");
                        }
                    }
                }
                long singleExecTime = stopwatch.stop().elapsed(TimeUnit.MILLISECONDS);
                stopwatch.reset();
                singleMaxTime = Math.max(singleExecTime, singleMaxTime);
                singleMinTime = Math.min(singleExecTime, singleMinTime);
            }
            processingTime = System.currentTimeMillis() - startTime;
        } catch (InterruptedException | BrokenBarrierException ex) {
            logger.error("cant await", ex);
        }
    }

    public long getProcessingTime() {
        return processingTime;
    }

    public long getSingleMaxTime() {
        return singleMaxTime;
    }

    public long getSingleMinTime() {
        return singleMinTime;
    }
}
