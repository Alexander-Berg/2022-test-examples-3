package ru.yandex.market.wms.timetracker.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;

@Profile("test")
@Configuration
public class TestAsyncConfig implements AsyncConfigurer {
    @Bean
    public Executor taskExecutor() {
        return new SyncTaskExecutor();
    }
}
