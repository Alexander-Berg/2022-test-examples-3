package ru.yandex.market.fulfillment.stockstorage.util;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import lombok.SneakyThrows;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class AsyncWaiterService {

    private static final long DEFAULT_TIMEOUT = 2000;

    private final List<ThreadPoolExecutor> threadPoolExecutors;

    public AsyncWaiterService(List<ThreadPoolTaskExecutor> executors) {
        this.threadPoolExecutors = executors.stream()
            .map(ThreadPoolTaskExecutor::getThreadPoolExecutor)
            .collect(Collectors.toList());
    }


    public void awaitTasks() {
        this.awaitTasks(DEFAULT_TIMEOUT);
    }

    public void awaitTasks(long timeoutMillis) {
        timeoutMillis = timeoutMillis < 0 ? DEFAULT_TIMEOUT : timeoutMillis;
        long deadline = System.currentTimeMillis() + timeoutMillis;
        threadPoolExecutors.forEach(executor -> await(executor, deadline));
    }

    @SneakyThrows
    private void await(ThreadPoolExecutor executor, long deadline) {
        do {
            Thread.sleep(100);
        } while (executor.getActiveCount() != 0 &&
                deadline > System.currentTimeMillis());
    }

}
