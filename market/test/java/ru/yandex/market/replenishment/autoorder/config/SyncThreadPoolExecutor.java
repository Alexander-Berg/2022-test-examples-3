package ru.yandex.market.replenishment.autoorder.config;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class SyncThreadPoolExecutor extends ThreadPoolExecutor {

    public SyncThreadPoolExecutor(int threadCount) {
        super(threadCount, threadCount,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());
    }

    @Override
    @SneakyThrows
    public Future<?> submit(@NotNull Runnable task) {
        Future<?> submit = super.submit(task);
        while (!submit.isDone()) {
            Thread.sleep(1000);
            log.debug("waiting for complete submit task");
        }
        return submit;
    }
}
