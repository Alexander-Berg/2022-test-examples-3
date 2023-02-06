package ru.yandex.market.tms.quartz2.spring;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.config.FunctionalTest;
import ru.yandex.market.tms.quartz2.config.FunctionalTestConfig;
import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.market.tms.quartz2.service.JobService;

/**
 * Тест проверяет, что при активации spring профайла {@code development} инициализация и хранение данных
 * шедулера производится только в памяти.
 * <p>
 * Запись в таблицу с логами выполнения задач происходит в любом случае
 */
@ContextConfiguration
@ActiveProfiles("development")
public class EnableMarketTmsWithDevelopmentProfileTest extends FunctionalTest {

    private static final TestExecutionStateHolder HOLDER = new TestExecutionStateHolder();
    @Autowired
    private JobService jobService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DbUnitDataSet
    void testEnableMarketTmsContextInit() throws InterruptedException, SchedulerException {
        jobService.runNow("testExecutor");

        HOLDER.getLatch().await(10, TimeUnit.SECONDS);
        Assertions.assertTrue(HOLDER.isSuccess());

        MarkerTmsAsserts.assertExistTestQrtzLogRecords(jdbcTemplate);
    }

    @Configuration
    @Import({
            FunctionalTestConfig.class
    })
    public static class Config {

        @Bean
        @CronTrigger(
                cronExpression = "0/1 * * * * ?",
                description = "Test executor"
        )
        public Executor testExecutor() {
            return context -> {
                HOLDER.markSuccess();
                HOLDER.getLatch().countDown();
            };
        }

    }

}
