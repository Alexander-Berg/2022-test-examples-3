package ru.yandex.market.tms.quartz2.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.config.FunctionalTest;
import ru.yandex.market.tms.quartz2.config.WithClobFunctionalTestConfig;
import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.market.tms.quartz2.service.JobService;
import ru.yandex.market.tms.quartz2.spring.CronTrigger;
import ru.yandex.market.tms.quartz2.spring.TestExecutionStateHolder;

/**
 * Проверяет, пишутся ли логи в таблицу при включенных клобах.
 *
 * @author ogonek
 */
@ContextConfiguration
@ActiveProfiles("development")
public class QrtzLogTableJobStatusWithClobTest extends FunctionalTest {
    private static final TestExecutionStateHolder HOLDER = new TestExecutionStateHolder();
    private static final String STACK_TRACE_PART = "at org.quartz.core.JobRunShell.run";

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    private JobService jobService;

    @Test
    @DbUnitDataSet
    @DisplayName("Проверяет, что проперти qrtzSaveFullLogs и qrtzStatusMaxLength работают нормально с коротким логом")
    void testShortStatusWithClob() throws SchedulerException, InterruptedException {
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

        LobHandler lobHandler = new DefaultLobHandler();
        String largeStatus = jdbcTemplate.query(
                "SELECT * FROM TEST_QRTZ_LOG WHERE JOB_NAME = 'okExecutor'",
                (rs, i) -> lobHandler.getClobAsString(rs, "FULL_JOB_STATUS")
        ).stream().findFirst().orElseThrow(() -> new RuntimeException("Large status Clob = null."));

        Assertions.assertEquals("OK", shortStatus);
        Assertions.assertEquals("OK", largeStatus);
    }

    @Test
    @DbUnitDataSet
    @DisplayName("Проверяет, что проперти qrtzSaveFullLogs и qrtzStatusMaxLength работают нормально с длинным логом")
    void testLargeStatusWithClobAndExtendedLengthShortStatus() throws
            SchedulerException, InterruptedException {
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

        LobHandler lobHandler = new DefaultLobHandler();
        String largeStatus = jdbcTemplate.query(
                "SELECT * FROM TEST_QRTZ_LOG WHERE JOB_NAME = 'exceptionExecutor'",
                (rs, i) -> lobHandler.getClobAsString(rs, "FULL_JOB_STATUS")
        ).stream().findFirst().orElseThrow(() -> new RuntimeException("Large status Clob = null."));

        Assertions.assertEquals(250, shortStatus.length());
        Assertions.assertTrue(12000 <= largeStatus.length(), "Large status Clob is too short.");    //Проверка, что
        // клоб сохраняется
        Assertions.assertTrue(largeStatus.contains(STACK_TRACE_PART), "Клоб не содержит в себе стектрейс.");
        //Проверка, что сохраняется стектрейс
    }

    @Configuration
    @Import({
            WithClobFunctionalTestConfig.class
    })
    public static class Config {

        @Bean
        @CronTrigger(
                cronExpression = "0/1 * * * * ?",
                description = "Test executor"
        )
        public Executor okExecutor() {
            return context -> {
            };
        }

        @Bean
        @CronTrigger(
                cronExpression = "0/1 * * * * ?",
                description = "Test executor"
        )
        public Executor exceptionExecutor() {
            return context -> {
                String exeption = "";
                for (int i = 0; i < 1200; i++) {
                    exeption = exeption.concat("0123456789");
                }
                throw new RuntimeException(exeption);
            };
        }

    }

}
