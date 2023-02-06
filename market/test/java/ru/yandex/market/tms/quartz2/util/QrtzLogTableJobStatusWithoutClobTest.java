package ru.yandex.market.tms.quartz2.util;

import java.sql.Clob;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
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
import ru.yandex.market.tms.quartz2.spring.CronTrigger;
import ru.yandex.market.tms.quartz2.spring.TestExecutionStateHolder;

/**
 * Проверяет, пишутся ли логи в таблицу при выключенных клобах.
 *
 * @author ogonek
 */
@ContextConfiguration
@ActiveProfiles("development")
public class QrtzLogTableJobStatusWithoutClobTest extends FunctionalTest {

    private static final TestExecutionStateHolder HOLDER = new TestExecutionStateHolder();
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    private JobService jobService;

    @Test
    @DbUnitDataSet
    @DisplayName("Проверяет, что дефолтные qrtzSaveFullLogs и qrtzStatusMaxLength работают нормально с коротким логом")
    void testShortStatusWithoutClob() throws SchedulerException, InterruptedException {
        QrtzTestUtil.runNow(
                "okExecutor",
                jobService,
                jdbcTemplate,
                HOLDER
        );
        String shortStatus = jdbcTemplate.queryForObject(
                "SELECT JOB_STATUS FROM TEST_QRTZ_LOG WHERE JOB_NAME = 'okExecutor'",
                String.class
        );
        Clob clobStatus = jdbcTemplate.queryForObject(
                "SELECT FULL_JOB_STATUS FROM TEST_QRTZ_LOG WHERE JOB_NAME = 'okExecutor'",
                Clob.class
        );
        Assertions.assertEquals("OK", shortStatus);
        Assertions.assertNull(clobStatus);
    }

    @Test
    @DbUnitDataSet
    @DisplayName("Проверяет, что дефолтные qrtzSaveFullLogs и qrtzStatusMaxLength работают нормально с длинным логом")
    void testLargeStatusWithoutClob() throws SchedulerException, InterruptedException {
        QrtzTestUtil.runNow(
                "exceptionExecutor",
                jobService,
                jdbcTemplate,
                HOLDER
        );
        String shortStatus = jdbcTemplate.queryForObject(
                "SELECT JOB_STATUS FROM TEST_QRTZ_LOG WHERE JOB_NAME = 'exceptionExecutor'",
                String.class
        );
        Clob clobStatus = jdbcTemplate.queryForObject(
                "SELECT FULL_JOB_STATUS FROM TEST_QRTZ_LOG WHERE JOB_NAME = 'exceptionExecutor'",
                Clob.class
        );
        Assertions.assertEquals(200, shortStatus.length());
        Assertions.assertNull(clobStatus);
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
        public Executor okExecutor() {
            return (Executor) context -> {
            };
        }

        @Bean
        @CronTrigger(
                cronExpression = "0/1 * * * * ?",
                description = "Test executor"
        )
        public Executor exceptionExecutor() {
            return (Executor) context -> {
                String exeption = "";
                for (int i = 0; i < 100; i++) {
                    exeption = exeption.concat("0123456789");
                }
                throw new RuntimeException(exeption);
            };
        }

    }

}
