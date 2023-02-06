package ru.yandex.market.tms.quartz2.spring;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.tms.quartz2.config.FunctionalTest;
import ru.yandex.market.tms.quartz2.config.FunctionalTestConfig;
import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.market.tms.quartz2.service.JobService;

/**
 * Тест проверяет, что триггеры, настроенные через аннотацию {@link CronTrigger} не могут работать впараллель
 */
@ContextConfiguration
class EnableMarketTmsConcurrentExecutionTest extends FunctionalTest {

    private static final AtomicInteger EXEC_COUNTER = new AtomicInteger(0);
    private static final CountDownLatch JOB_START_LATCH = new CountDownLatch(1);
    private static final CountDownLatch JOB_END_LATCH = new CountDownLatch(1);
    private static final CountDownLatch JOB_END_CONFIRMED_LATCH = new CountDownLatch(1);
    @Autowired
    private JobService jobService;

    @Test
    void testTriggersConcurrentExecution() throws InterruptedException, SchedulerException {
        Assertions.assertTrue(JOB_START_LATCH.await(10, TimeUnit.SECONDS));
        Assertions.assertTrue(JOB_END_LATCH.await(10, TimeUnit.SECONDS));

        jobService.removeJob("testExecutor");

        JOB_END_CONFIRMED_LATCH.countDown();

        Assertions.assertEquals(1, EXEC_COUNTER.get());
    }

    @Configuration
    @Import({
            FunctionalTestConfig.class,
    })
    static class Config {

        @Bean
        @CronTrigger(
                cronExpression = "0/1 * * * * ?",
                description = "Test executor"
        )
        public Executor testExecutor() {
            return context -> {
                JOB_START_LATCH.countDown();
                EXEC_COUNTER.incrementAndGet();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                JOB_END_LATCH.countDown();
                try {
                    JOB_END_CONFIRMED_LATCH.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            };
        }

    }

}
