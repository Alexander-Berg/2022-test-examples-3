package ru.yandex.direct.redislock.lettuce.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.direct.redislock.ConfigData;
import ru.yandex.direct.redislock.lettuce.LettuceLockBuilder;

public class LettuceLockBenchmark {
    private static final Logger logger = LoggerFactory.getLogger(LettuceLockBenchmark.class);

    private static final int numIds = 100_000;

    private static final int numLaunches = 10;

    private static final int defaultNumThreads = 256;

    public static void main(String[] args) throws InterruptedException {
        int numThreads = args.length == 1 ? Integer.valueOf(args[0]) : defaultNumThreads;
        LettuceLockBuilder factory = LettuceLockBuilder.
                newBuilder(LettuceLockBenchmark::createRedisClusterConnection).
                withMaxLocks(10).
                withTTL(9000);
        for (int i = 0; i < numLaunches; ++i) {
            benchmarkLock(factory, numThreads);
        }
    }

    public static StatefulRedisClusterConnection<String, String> createRedisClusterConnection() {
        RedisClusterClient client = RedisClusterClient.create(ConfigData.REDIS_URIS);
        return client.connect();
    }

    private static void benchmarkLock(LettuceLockBuilder factory, int numThreads) throws InterruptedException {
        final CyclicBarrier barrier = new CyclicBarrier(numThreads);
        List<BenchmarkThread> threads = new ArrayList<>(numThreads);
        for (int i = 0; i < numThreads; ++i) {
            BenchmarkThread t = new BenchmarkThread(factory, numIds / numThreads, numIds, barrier);
            threads.add(t);
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numThreads; ++i) {
            BenchmarkThread t = threads.get(i);
            t.start();
        }
        long singleMaxTime = 0;
        for (int i = 0; i < numThreads; ++i) {
            BenchmarkThread t = threads.get(i);
            t.join();
            if (singleMaxTime < t.getSingleMaxTime()) {
                singleMaxTime = t.getSingleMaxTime();
            }
        }
        long totalTime = System.currentTimeMillis() - startTime;
        logger.debug("threads: {}, total time: {}, items: {}, rps: {}, singleMaxTime: {}",
                numThreads, totalTime, numIds, numIds * 1000.0 / totalTime, singleMaxTime);
    }
}
