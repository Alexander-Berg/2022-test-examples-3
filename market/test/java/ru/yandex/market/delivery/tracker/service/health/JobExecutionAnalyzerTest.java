package ru.yandex.market.delivery.tracker.service.health;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.delivery.tracker.dao.repository.JobInfoDao;
import ru.yandex.market.delivery.tracker.domain.entity.jobs.JobLogRow;
import ru.yandex.market.delivery.tracker.domain.entity.jobs.JobMonitoringConfigRow;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class JobExecutionAnalyzerTest {

    private final JobInfoDao jobInfoDao = Mockito.mock(JobInfoDao.class);

    private final JobExecutionAnalyzer analyzer = new JobExecutionAnalyzer(jobInfoDao);

    private final List<JobMonitoringConfigRow> jobConfig = ImmutableList.of(
        JobMonitoringConfigRow.builder()
            .setJobName("1")
            .setMaxDelayTime(5000L)
            .setMaxExecutionTime(5000L)
            .setMaxFailedRun(1)
            .build(),
        JobMonitoringConfigRow.builder()
            .setJobName("2")
            .setMaxDelayTime(5000L)
            .setMaxExecutionTime(5000L)
            .setMaxFailedRun(1)
            .build(),
        JobMonitoringConfigRow.builder()
            .setJobName("3")
            .setMaxDelayTime(5000L)
            .setMaxExecutionTime(5000L)
            .setMaxFailedRun(2)
            .build()
    );

    @BeforeEach
    void setUp() {
        when(jobInfoDao.getJobMonitoringConfig()).thenReturn(jobConfig);
    }

    @Test
    void testAllJobsOK() {

        when(jobInfoDao.getLastLogsForJob("1", 1))
            .thenReturn(Collections.singletonList(
                JobLogRow.builder().setJobName("1")
                    .setTriggerFireTime(new Date(System.currentTimeMillis() - 3000))//is running
                    .build()));
        when(jobInfoDao.getLastLogsForJob("2", 1))
            .thenReturn(Collections.singletonList(
                JobLogRow.builder().setJobName("2")
                    .setTriggerFireTime(new Date(System.currentTimeMillis() - 3000))
                    .setJobFinishedTime(new Date())//just stopped with OK
                    .setJobStatus("OK")
                    .build()));
        when(jobInfoDao.getLastLogsForJob("3", 1))
            .thenReturn(Collections.singletonList(
                JobLogRow.builder().setJobName("3")
                    .setTriggerFireTime(new Date(System.currentTimeMillis() - 3000))
                    .setJobFinishedTime(new Date())//just stopped with OK
                    .setJobStatus("OK")
                    .build()));

        Set<String> result = analyzer.checkHangingJobs(jobConfig).stream()
            .map(Object::toString)
            .collect(toSet());
        assertEquals(emptySet(), result, "Result must be empty");
    }

    @Test
    void testOneJobFinishedFailedOneTime() {
        when(jobInfoDao.getLastLogsForJob("1", 1))
            .thenReturn(Collections.singletonList(
                JobLogRow.builder().setJobName("1")
                    .setTriggerFireTime(new Date(System.currentTimeMillis() - 3000))//is running
                    .build()));
        when(jobInfoDao.getLastLogsForJob("2", 1))
            .thenReturn(Collections.singletonList(
                JobLogRow.builder().setJobName("2")
                    .setTriggerFireTime(new Date(System.currentTimeMillis() - 3000))
                    .setJobFinishedTime(new Date())//just stopped with OK
                    .setJobStatus("OK")
                    .build()));
        when(jobInfoDao.getLastLogsForJob("3", 1))
            .thenReturn(Collections.singletonList(
                JobLogRow.builder().setJobName("3")
                    .setTriggerFireTime(new Date(System.currentTimeMillis() - 3000))
                    .setJobFinishedTime(new Date())//stopped too much time ago with NON_OK
                    .setJobStatus("NON_OK")
                    .build()));

        Set<String> result = analyzer.checkHangingJobs(jobConfig).stream()
            .map(Object::toString)
            .collect(toSet());
        assertEquals(emptySet(), result, "Result must be empty");
    }

    @Test
    void testOneJobFinishedFailedOneTimeAndThenWorksOk() {
        when(jobInfoDao.getLastLogsForJob("1", 1))
            .thenReturn(Collections.singletonList(
                JobLogRow.builder().setJobName("1")
                    .setTriggerFireTime(new Date(System.currentTimeMillis() - 3000))//is running
                    .build()));
        when(jobInfoDao.getLastLogsForJob("2", 1))
            .thenReturn(Collections.singletonList(
                JobLogRow.builder().setJobName("2")
                    .setTriggerFireTime(new Date(System.currentTimeMillis() - 3000))
                    .setJobFinishedTime(new Date())//just stopped with OK
                    .setJobStatus("OK")
                    .build()));
        when(jobInfoDao.getLastLogsForJob("3", 1))
            .thenReturn(Arrays.asList(
                JobLogRow.builder().setJobName("3")
                    .setTriggerFireTime(new Date(System.currentTimeMillis() - 3000))
                    .setJobFinishedTime(new Date())//just stopped with NON_OK
                    .setJobStatus("NON_OK")
                    .build(),
                JobLogRow.builder().setJobName("3")
                    .setTriggerFireTime(new Date(System.currentTimeMillis() - 3000))
                    .setJobFinishedTime(new Date())//just stopped with OK
                    .setJobStatus("OK")
                    .build()));

        Set<String> result = analyzer.checkHangingJobs(jobConfig).stream()
            .map(Object::toString)
            .collect(toSet());
        assertEquals(emptySet(), result, "Result must be empty");
    }

    @Test
    void testOneJobIsBeingExecutedToLong() {
        when(jobInfoDao.getLastLogsForJob("1", 1))
            .thenReturn(Collections.singletonList(
                JobLogRow.builder().setJobName("1")
                    .setTriggerFireTime(new Date(System.currentTimeMillis() - 8000))//is running too long
                    .build()));
        when(jobInfoDao.getLastLogsForJob("2", 1))
            .thenReturn(Collections.singletonList(
                JobLogRow.builder().setJobName("2")
                    .setTriggerFireTime(new Date(System.currentTimeMillis() - 3000))
                    .setJobFinishedTime(new Date())//just stopped with OK
                    .setJobStatus("OK")
                    .build()));
        when(jobInfoDao.getLastLogsForJob("3", 1))
            .thenReturn(Collections.singletonList(
                JobLogRow.builder().setJobName("3")
                    .setTriggerFireTime(new Date(System.currentTimeMillis() - 3000))
                    .setJobFinishedTime(new Date())//just stopped with OK
                    .setJobStatus("OK")
                    .build()));

        Set<String> result = analyzer.checkHangingJobs(jobConfig).stream()
            .map(Object::toString)
            .collect(toSet());
        assertTrue(
            result.size() == 1
                    && result.iterator().next().startsWith("1 is running for "),
            "One job must have been executed too long"
        );
    }

    @Test
    void testOneJobIsBeingStoppedToLong() {
        when(jobInfoDao.getLastLogsForJob("1", 1))
            .thenReturn(Collections.singletonList(
                JobLogRow.builder().setJobName("1")
                    .setTriggerFireTime(new Date(System.currentTimeMillis() - 3000))//is running
                    .build()));
        when(jobInfoDao.getLastLogsForJob("2", 1))
            .thenReturn(Collections.singletonList(
                JobLogRow.builder().setJobName("2")
                    .setTriggerFireTime(new Date(System.currentTimeMillis() - 3000))
                    .setJobFinishedTime(new Date())//just stopped with OK
                    .setJobStatus("OK")
                    .build()));
        when(jobInfoDao.getLastLogsForJob("3", 1))
            .thenReturn(Collections.singletonList(
                JobLogRow.builder().setJobName("3")
                    .setTriggerFireTime(new Date(System.currentTimeMillis() - 15000))
                    .setJobFinishedTime(new Date(System.currentTimeMillis() - 8000))
                    .setJobStatus("OK")//stopped too much time ago with OK
                    .build()));

        Set<String> result = analyzer.checkHangingJobs(jobConfig).stream()
            .map(Object::toString)
            .collect(toSet());
        assertTrue(
    result.size() == 1 && result.iterator().next().startsWith("3 is not running for: "),
    "One job must have been stopped too long"
        );
    }
}
