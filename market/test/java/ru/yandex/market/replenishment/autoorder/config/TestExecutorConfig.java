package ru.yandex.market.replenishment.autoorder.config;

import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
@AutoConfigureBefore(ExecutorConfig.class)
public class TestExecutorConfig {

    @Bean
    @Qualifier("generationDataExecutor")
    public ExecutorService generationDataExecutor(
        @Value("${autoorder.background-generation-file.threadCount:1}") int threadCount) {
        return new SyncThreadPoolExecutor(threadCount);
    }
}
