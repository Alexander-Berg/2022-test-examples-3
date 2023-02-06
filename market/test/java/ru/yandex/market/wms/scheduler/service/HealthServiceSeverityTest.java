package ru.yandex.market.wms.scheduler.service;

import java.time.Instant;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;
import ru.yandex.market.wms.scheduler.dto.JobFailure;
import ru.yandex.market.wms.scheduler.dto.Reason;
import ru.yandex.market.wms.scheduler.dto.SeverityLevel;

public class HealthServiceSeverityTest extends SchedulerIntegrationTest {

    private static final String JOB_GROUP = "clean";
    private static final String JOB_NAME = "SomeCleanJob";

    @Autowired
    private ConfigCache configCache;

    @Autowired
    private HealthService healthService;

    @BeforeEach
    public void before() {
        configCache.updateConfigs();
    }

    @Test
    @DatabaseSetup(value = "/db/health/hanging/happy.xml", connection = "schedulerConnection")
    public void checkHangingJobHappyPathTest() {
        List<JobFailure> actual = healthService.checkHangingJob(JOB_GROUP, JOB_NAME);
        List<JobFailure> expected = List.of();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DatabaseSetup(value = "/db/health/hanging/exec-time-exceeded-crit-1.xml", connection = "schedulerConnection")
    public void checkHangingJobExecutionTimeExceededCritTest1() {
        List<JobFailure> actual = healthService.checkHangingJob(JOB_GROUP, JOB_NAME);
        List<JobFailure> expected = List.of(
                new JobFailure("SomeCleanJob", Instant.parse("3021-12-16T09:00:00Z"),
                        Reason.EXECUTION_TIME_EXCEEDED, SeverityLevel.CRIT,
                        "Max execution time: 120 s (W:50/C:100)")
        );
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DatabaseSetup(value = "/db/health/hanging/exec-time-exceeded-crit-2.xml", connection = "schedulerConnection")
    public void checkHangingJobExecutionTimeExceededCritTest2() {
        List<JobFailure> actual = healthService.checkHangingJob(JOB_GROUP, JOB_NAME);
        List<JobFailure> expected = List.of(
                new JobFailure("SomeCleanJob", Instant.parse("3021-12-16T09:00:00Z"),
                        Reason.EXECUTION_TIME_EXCEEDED, SeverityLevel.CRIT,
                        "Max execution time: 120 s (W:50/C:100)")
        );
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DatabaseSetup(value = "/db/health/hanging/exec-time-exceeded-warn-1.xml", connection = "schedulerConnection")
    public void checkHangingJobExecutionTimeExceededWarnTest1() {
        List<JobFailure> actual = healthService.checkHangingJob(JOB_GROUP, JOB_NAME);
        List<JobFailure> expected = List.of(
                new JobFailure("SomeCleanJob", Instant.parse("3021-12-16T11:00:00Z"),
                        Reason.EXECUTION_TIME_EXCEEDED, SeverityLevel.WARN,
                        "Max execution time: 60 s (W:50/C:100)")
        );
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DatabaseSetup(value = "/db/health/hanging/exec-time-exceeded-warn-2.xml", connection = "schedulerConnection")
    public void checkHangingJobExecutionTimeExceededWarnTest2() {
        List<JobFailure> actual = healthService.checkHangingJob(JOB_GROUP, JOB_NAME);
        List<JobFailure> expected = List.of(
                new JobFailure("SomeCleanJob", Instant.parse("3021-12-16T11:00:00Z"),
                        Reason.EXECUTION_TIME_EXCEEDED, SeverityLevel.WARN,
                        "Max execution time: 60 s (W:50/C:100)")
        );
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DatabaseSetup(value = "/db/health/hanging/running-delay-exceeded-crit-1.xml", connection = "schedulerConnection")
    public void checkHangingJobRunningDelayExceededCritTest1() {
        List<JobFailure> actual = healthService.checkHangingJob(JOB_GROUP, JOB_NAME);
        List<JobFailure> expected = List.of(
                new JobFailure("SomeCleanJob", Instant.parse("2020-12-16T12:00:00Z"),
                        Reason.RUNNING_DELAY_EXCEEDED, SeverityLevel.CRIT)
        );
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DatabaseSetup(value = "/db/health/hanging/running-delay-exceeded-crit-2.xml", connection = "schedulerConnection")
    public void checkHangingJobRunningDelayExceededCritTest2() {
        List<JobFailure> actual = healthService.checkHangingJob(JOB_GROUP, JOB_NAME);
        List<JobFailure> expected = List.of(
                new JobFailure("SomeCleanJob", Instant.parse("2020-12-16T12:00:00Z"),
                        Reason.RUNNING_DELAY_EXCEEDED, SeverityLevel.CRIT)
        );
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DatabaseSetup(value = "/db/health/failing/crit-1.xml", connection = "schedulerConnection")
    public void checkFailingJobCritTest1() {
        List<JobFailure> actual = healthService.checkFailedJob(JOB_GROUP, JOB_NAME);
        List<JobFailure> expected = List.of(
                new JobFailure("SomeCleanJob", Instant.parse("2020-12-16T11:00:00Z"),
                        Reason.FAILED_TASK, SeverityLevel.CRIT, "Failed 1 times (W=1, C=1)")
        );
        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DatabaseSetup(value = "/db/health/failing/warn-1.xml", connection = "schedulerConnection")
    public void checkFailingJobWarnTest1() {
        List<JobFailure> actual = healthService.checkFailedJob(JOB_GROUP, JOB_NAME);
        List<JobFailure> expected = List.of(
                new JobFailure("SomeCleanJob", Instant.parse("2020-12-16T11:00:00Z"),
                        Reason.FAILED_TASK, SeverityLevel.WARN, "Failed 1 times (W=1, C=2)")
        );
        Assertions.assertEquals(expected, actual);
    }
}
