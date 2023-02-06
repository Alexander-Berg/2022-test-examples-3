package ru.yandex.market.tms.quartz2.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.commons.util.StringUtils;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.tms.quartz2.config.FunctionalTest;
import ru.yandex.market.tms.quartz2.config.WithTraceFunctionalTestConfig;
import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.market.tms.quartz2.service.JobService;
import ru.yandex.market.tms.quartz2.spring.CronTrigger;
import ru.yandex.market.tms.quartz2.spring.TestExecutionStateHolder;

/**
 * Проверяет, сохраняется ли trace id задачек.
 *
 * @author artemmz
 */
@ContextConfiguration
@ActiveProfiles("development")
public class QrtzLogTableJobStatusWithTraceTest extends FunctionalTest {
    private static final TestExecutionStateHolder HOLDER = new TestExecutionStateHolder();

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    private JobService jobService;

    @ParameterizedTest
    @CsvSource({"okExecutor", "exceptionExecutor"})
    @DisplayName("Trace id записан для любой задачи")
    void testTraceAfterOk(String executorName) throws SchedulerException, InterruptedException {
        QrtzTestUtil.runNow(
                executorName,
                jobService,
                jdbcTemplate,
                HOLDER
        );

        String traceId = jdbcTemplate.queryForObject(
                "SELECT TRACE_ID FROM TEST_QRTZ_LOG WHERE JOB_NAME = ?",
                String.class, executorName
        );

        Assertions.assertTrue(StringUtils.isNotBlank(traceId));
    }

    @Configuration
    @Import({
            WithTraceFunctionalTestConfig.class
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
                description = "Test failed executor"
        )
        public Executor exceptionExecutor() {
            return context -> {
                throw new RuntimeException("job failed!");
            };
        }

    }

}
