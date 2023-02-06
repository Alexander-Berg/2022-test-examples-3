package ru.yandex.market.logistics.management.configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@TestConfiguration
@EnableAsync
public class TestAsyncConfiguration {

    @Bean(name = "timedCommonExecutor")
    public Executor timedCommonExecutor(ThreadPoolTaskExecutor commonExecutor) {
        return commonExecutor;
    }

    @Bean(name = "timedGeosearchExecutor")
    public ExecutorService timedGeosearchExecutor() {
        return new ForkJoinPool(5);
    }

    @Bean(name = "timedWarehouseExecutor")
    public ExecutorService timedWarehouseExecutor(ThreadPoolTaskExecutor commonExecutor) {
        return commonExecutor.getThreadPoolExecutor();
    }

    @Bean(name = "timedPickupPointsExecutor")
    public ExecutorService timedPickupPointsExecutor(ThreadPoolTaskExecutor commonExecutor) {
        return commonExecutor.getThreadPoolExecutor();
    }

    @Bean
    @ConfigurationProperties(prefix = "test.async.pool")
    protected ThreadPoolTaskExecutor commonExecutor() {
        return new ThreadPoolTaskExecutor();
    }
}
