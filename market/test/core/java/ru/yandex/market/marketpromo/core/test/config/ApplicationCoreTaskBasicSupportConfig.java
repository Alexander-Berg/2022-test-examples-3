package ru.yandex.market.marketpromo.core.test.config;

import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import net.javacrumbs.shedlock.core.LockProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import ru.yandex.market.marketpromo.core.config.ApplicationSchedulingConfig;
import ru.yandex.market.marketpromo.core.dao.internal.SchedulingLockDao;
import ru.yandex.market.marketpromo.core.scheduling.executors.CPIScheduledExecutorService;
import ru.yandex.market.marketpromo.core.scheduling.lock.YdbLockProvider;
import ru.yandex.market.marketpromo.core.utils.RequestContextUtils;
import ru.yandex.market.marketpromo.misc.ExtendedClock;

import static ru.yandex.market.marketpromo.core.config.LogConstants.LOG_TYPE_MARK;
import static ru.yandex.market.marketpromo.core.config.LogConstants.Marks.TMS;

@Configuration
@ComponentScan(
        value = "ru.yandex.market.marketpromo.core.service.task",
        includeFilters = @ComponentScan.Filter(Service.class)
)
public class ApplicationCoreTaskBasicSupportConfig {

    private static final Logger LOG = LogManager.getLogger(ApplicationSchedulingConfig.class);

    @Bean(
            initMethod = "initialize",
            destroyMethod = "destroy"
    )
    public ThreadPoolTaskScheduler tmsTaskScheduler(
            @Value("${app.tms.processing.poolSize:30}") int processingPoolSize
    ) {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler() {
            @Override
            protected ScheduledExecutorService createExecutor(
                    int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler
            ) {
                return CPIScheduledExecutorService.wrap(
                        super.createExecutor(poolSize, threadFactory, rejectedExecutionHandler));
            }
        };
        taskScheduler.setPoolSize(processingPoolSize);
        taskScheduler.setErrorHandler(throwable -> {
            RequestContextUtils.wrapWith(LOG_TYPE_MARK, TMS,
                    () -> LOG.error("unexpected error in tms", throwable));
            //TODO: monitoring
        });
        return taskScheduler;
    }

    @ConditionalOnMissingBean(ScheduledExecutorService.class)
    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService unlockScheduler(@Value("${app.tms.unlocking.poolSize:10}") int poolSize) {
        return CPIScheduledExecutorService.wrap(Executors.newScheduledThreadPool(poolSize));
    }

    @ConditionalOnMissingBean(LockProvider.class)
    @Bean
    public LockProvider lockProvider(
            SchedulingLockDao schedulingLockDao,
            ExtendedClock extendedClock,
            ScheduledExecutorService unlockScheduler
    ) {
        return new YdbLockProvider(
                schedulingLockDao,
                extendedClock,
                unlockScheduler
        );
    }
}
