package ru.yandex.market.fulfillment.stockstorage.service.health;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.jobs.JobFailure;
import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.jobs.JobInfoDao;
import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.jobs.JobLogRow;
import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.jobs.JobMonitoringConfigRow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.fulfillment.stockstorage.domain.JobName.FULL_SYNC;

public class JobExecutionAnalyzerTest {

    private final JobInfoDao jobInfoDao = mock(JobInfoDao.class);
    private final JobExecutionAnalyzer jobExecutionAnalyzer = new JobExecutionAnalyzer(jobInfoDao);

    /**
     * Проверяет, что аналайзер вернул две джобы, одна из которыз failed, а вторая hanging
     */
    @Test
    public void analyzeLastJobExecution() {
        when(jobInfoDao.getJobMonitoringConfig()).thenReturn(getJobMonitoringConfig());

        when(jobInfoDao.getLastLogsForJob("okJob", 1)).thenReturn(getLastLogsForJob("okJob"));
        when(jobInfoDao.getLastLogsForJob("fullSyncExecutor", 1)).thenReturn(getLastLogsForJob("fullSyncExecutor"));
        when(jobInfoDao.getLastLogsForJob("delayExceededHangingJob", 1)).thenReturn(getLastLogsForJob(
                "delayExceededHangingJob"));

        Set<JobFailure> jobFailures = jobExecutionAnalyzer.analyzeLastJobExecution();

        List<JobFailure> jobFailureList = jobFailures.stream()
                .filter(JobFailure::isFailed)
                .collect(Collectors.toList());

        List<JobFailure> runningDelayExceededJobList = jobFailures.stream()
                .filter(JobFailure::isRunningDelayExceeded)
                .collect(Collectors.toList());

        assertEquals(2, jobFailures.size());

        assertEquals("jobFailureList should contain 1 element", 1, jobFailureList.size());
        assertTrue("jobFailure message should contain job name",
                jobFailureList.get(0).toString().contains(FULL_SYNC.name()));
        assertTrue(jobFailureList.get(0).toString().contains("failed"));

        assertEquals(1, runningDelayExceededJobList.size());
        assertTrue("jobFailure message should contain job name",
                runningDelayExceededJobList.get(0).toString().contains("delayExceededHangingJob"));
        assertTrue("jobFailure message should contain host name",
                runningDelayExceededJobList.get(0).toString().contains("host"));
    }

    @Test
    public void analyzeLastJobExecutionEmptyLog() {
        when(jobInfoDao.getJobMonitoringConfig()).thenReturn(getJobMonitoringConfig());
        when(jobInfoDao.getLastLogsForJob(any(String.class), any(Integer.class))).thenReturn(Collections.EMPTY_LIST);

        Set<JobFailure> jobFailures = jobExecutionAnalyzer.analyzeLastJobExecution();
        assertEquals(0, jobFailures.size());
    }

    private List<JobMonitoringConfigRow> getJobMonitoringConfig() {
        List<JobMonitoringConfigRow> jobMonitoringConfig = new LinkedList<>();

        jobMonitoringConfig.add(JobMonitoringConfigRow.builder()
                .setJobName("okJob")
                .setMaxFailedRuns(1)
                .setMaxExecutionTime(1800000L)
                .setMaxDelayTime(1200000L)
                .build());

        jobMonitoringConfig.add(JobMonitoringConfigRow.builder()
                .setJobName("fullSyncExecutor")
                .setMaxFailedRuns(1)
                .setMaxExecutionTime(1800000L)
                .setMaxDelayTime(1200000L)
                .build());

        jobMonitoringConfig.add(JobMonitoringConfigRow.builder()
                .setJobName("delayExceededHangingJob")
                .setMaxFailedRuns(1)
                .setMaxExecutionTime(1800000L)
                .setMaxDelayTime(1200000L)
                .build());

        return jobMonitoringConfig;
    }

    private List<JobLogRow> getLastLogsForJob(String jobName) {
        List<JobLogRow> jobLogRows = new LinkedList<>();

        switch (jobName) {
            case "okJob":
                jobLogRows.add(getBuilder("okJob", "OK").build());
                break;
            case "fullSyncExecutor":
                jobLogRows.add(getBuilder("fullSyncExecutor", "Exception").build());
                break;
            case "delayExceededHangingJob":
                jobLogRows.add(getBuilder("delayExceededHangingJob", "OK")
                        .setJobFinishedTime(org.apache.commons.lang.time.DateUtils.addDays(new Date(), -1))
                        .build());
                break;
            default:
                break;
        }

        return jobLogRows;
    }

    private JobLogRow.Builder getBuilder(String jobName, String jobStatus) {
        return JobLogRow.builder()
                .setHostName("host")
                .setJobName(jobName)
                .setJobStatus(jobStatus)
                .setJobFinishedTime(new Date());
    }
}
