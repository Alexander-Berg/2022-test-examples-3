package ru.yandex.market.tms.quartz2.model;

import java.util.Collection;
import java.util.HashMap;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.quartz.simpl.RAMJobStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.common.util.collections.immutable.SingletonMap;
import ru.yandex.market.tms.quartz2.config.FunctionalTest;
import ru.yandex.market.tms.quartz2.config.FunctionalTestConfig;
import ru.yandex.market.tms.quartz2.executors.TestClassExecutor;
import ru.yandex.market.tms.quartz2.service.JobService;
import ru.yandex.market.tms.quartz2.spring.AnnotatedTriggersFactory;
import ru.yandex.market.tms.quartz2.spring.CronTrigger;

import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author ogonek
 * @date 28.08.18
 */
@ContextConfiguration
@ActiveProfiles("development")
class QrtzTmsGetAllJobsInfoWithDataMapTest extends FunctionalTest {

    private static final String TEST_EXPR = AnnotatedTriggersFactory.NEVER_RUN_CRON_EXPR;

    @Autowired
    private JobService jobService;

    @Autowired
    private Scheduler scheduler;

    /**
     * Проверяет, что getAllJobsInfoWithoutDataMap достает правильную информацию по всем джобам из таблицы.
     */
    @Test
    void getAllJobsInfoWithoutDataMap() throws SchedulerException {

        Class currentJobStore = scheduler.getMetaData().getJobStoreClass();
        Assertions.assertEquals(RAMJobStore.class, currentJobStore);

        JobInfo expected1 = new JobInfo(
                new JobKey("testExecutor", "DEFAULT"),
                null,
                new HashMap<>(),
                new SingletonMap<>(
                        new TriggerKey("testExecutor", "DEFAULT"),
                        TEST_EXPR),
                null
        );
        JobInfo expected2 = new JobInfo(
                new JobKey("testExecutor2", "DEFAULT"),
                null,
                new HashMap<>(),
                new SingletonMap<>(
                        new TriggerKey("testExecutor2", "DEFAULT"),
                        TEST_EXPR),
                null
        );
        JobInfo expected3 = new JobInfo(
                new JobKey("testClassExecutor", "DEFAULT"),
                null,
                new HashMap<>(),
                new SingletonMap<>(
                        new TriggerKey("testClassExecutor", "DEFAULT"),
                        TEST_EXPR),
                null
        );

        Collection<JobInfo> jobs = jobService.getAllJobs();
        MatcherAssert.assertThat(jobs, containsInAnyOrder(expected1, expected2, expected3));
    }

    @Configuration
    @Import({
            FunctionalTestConfig.class
    })
    @ComponentScan(basePackageClasses = TestClassExecutor.class)
    public static class Config {

        // Ещё две джобы приедут из FunctionalTestConfig и @ComponentScan

        @Bean
        @CronTrigger(
                cronExpression = TEST_EXPR,
                description = "Test executor"
        )
        public Executor testExecutor2() {
            return context -> {
            };
        }
    }
}
