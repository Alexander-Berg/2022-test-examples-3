package ru.yandex.market.tms.quartz2.util;

import java.util.concurrent.TimeUnit;

import org.quartz.SchedulerException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import ru.yandex.market.tms.quartz2.service.JobService;
import ru.yandex.market.tms.quartz2.spring.TestExecutionStateHolder;

/**
 * Класс с методами для тестов кварца.
 *
 * @author ogonek
 */
public class QrtzTestUtil {

    private QrtzTestUtil() {
        throw new UnsupportedOperationException("QrtzTestUtil is an utility class");
    }

    public static void runNow(
            String jobName,
            JobService jobService,
            JdbcTemplate jdbcTemplate,
            TestExecutionStateHolder holder
    ) throws SchedulerException, InterruptedException {
        jobService.runNow(jobName);
        for (int i = 0; i < 50; i++) {
            SqlRowSet set = jdbcTemplate.queryForRowSet(
                    "SELECT * FROM TEST_QRTZ_LOG WHERE JOB_NAME = ? and JOB_STATUS is not null", jobName
            );
            if (set != null && set.next()) {
                return;
            }
            holder.getLatch().await(200, TimeUnit.MILLISECONDS);
        }
    }
}
