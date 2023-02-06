package ru.yandex.market.crm.core.schedule;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import ru.yandex.market.mcrm.lock.LockService;

@Configuration
@EnableScheduling
public class ScheduleTestConfig {

    @Bean
    public ClusterSchedulingConfigurer clusterSchedulingConfigurer(ApplicationContext ctx,
                                                                   ExecutorService taskScheduler,
                                                                   LockService lockService) {
        return new ClusterSchedulingConfigurer(ctx, taskScheduler, lockService);
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService taskScheduler() {
        return Executors.newScheduledThreadPool(3);
    }

    @Bean
    public LockService lockService() {
        return Mockito.mock(LockService.class);
    }
}
