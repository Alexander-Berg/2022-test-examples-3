package ru.yandex.market.wms.scheduler.dao;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;
import ru.yandex.market.wms.scheduler.dao.entity.JobMonitoringConfigRow;
import ru.yandex.market.wms.scheduler.dto.JobMonitoringConfigDto;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertAll;

class JobMonitoringConfigDaoTest extends SchedulerIntegrationTest {

    private static final String JOB_GROUP = "clean";
    private static final String JOB_NAME = "CleanSkuLocJob";
    private static final int RUNS_NUMBER = 1;
    private static final long MAX_DELAY_TIME = 60 * 60 * 24;
    private static final long MAX_EXEC_TIME = 60L;
    private static final int MAX_FAILED_RUNS = 1;
    private static final int MAX_EXECUTION_RUNS = 5;
    private static final int WARN_EXECUTION_RUNS = 3;
    private static final int RUNS_NUMBER_TO_CONSIDER_FOR_HANGING = 1;
    private static final JobMonitoringConfigDto CONFIG_DTO = JobMonitoringConfigDto.builder()
            .maxDelayTime(100).warnDelayTime(50).maxExecutionTime(100)
            .warnExecutionTime(50).maxFailedRuns(1).warnFailedRuns(1)
            .maxExecutionRuns(MAX_EXECUTION_RUNS).warnExecutionRuns(WARN_EXECUTION_RUNS)
            .runsNumberToConsiderForHanging(RUNS_NUMBER_TO_CONSIDER_FOR_HANGING)
            .runsNumberToConsiderForFailing(RUNS_NUMBER).build();
    private static final JobMonitoringConfigDto CHANGED_CONFIG_DTO = JobMonitoringConfigDto.builder()
            .maxDelayTime(500).warnDelayTime(500).maxExecutionTime(1000)
            .warnExecutionTime(500).maxFailedRuns(2).warnFailedRuns(1)
            .maxExecutionRuns(MAX_EXECUTION_RUNS).warnExecutionRuns(WARN_EXECUTION_RUNS)
            .runsNumberToConsiderForHanging(5).runsNumberToConsiderForFailing(4).build();
    private static final JobMonitoringConfigRow CONFIG_ROW = JobMonitoringConfigRow.builder()
            .jobGroup(JOB_GROUP).jobName(JOB_NAME).maxDelayTime(100).warnDelayTime(50).maxExecutionTime(100)
            .warnExecutionTime(50).maxFailedRuns(1).warnFailedRuns(1)
            .maxExecutionRuns(MAX_EXECUTION_RUNS).warnExecutionRuns(WARN_EXECUTION_RUNS)
            .runsNumberToConsiderForHanging(RUNS_NUMBER_TO_CONSIDER_FOR_HANGING)
            .runsNumberToConsiderForFailing(RUNS_NUMBER).build();
    private static final JobMonitoringConfigRow DEFAULT_CONFIG_ROW = JobMonitoringConfigRow.builder()
            .jobGroup(JOB_GROUP).jobName(JOB_NAME)
            .runsNumberToConsiderForHanging(RUNS_NUMBER_TO_CONSIDER_FOR_HANGING)
            .runsNumberToConsiderForFailing(RUNS_NUMBER)
            .maxDelayTime(MAX_DELAY_TIME).warnDelayTime(MAX_DELAY_TIME).maxExecutionTime(MAX_EXEC_TIME)
            .maxExecutionRuns(MAX_EXECUTION_RUNS).warnExecutionRuns(WARN_EXECUTION_RUNS)
            .warnExecutionTime(MAX_EXEC_TIME).maxFailedRuns(MAX_FAILED_RUNS).warnFailedRuns(MAX_FAILED_RUNS).build();

    @Autowired
    private JobMonitoringConfigDao dao;

    @Test
    @DatabaseSetup(value = "/db/dao/job_monitoring_config/before.xml", connection = "schedulerConnection")
    @ExpectedDatabase(value = "/db/dao/job_monitoring_config/before.xml", connection = "schedulerConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void getJobMonitoringConfigWhenConfigExistsTest() {
        JobMonitoringConfigRow expected = CONFIG_ROW;
        List<JobMonitoringConfigRow> actualResult = dao.getJobMonitoringConfig();
        assertAll(
                () -> Assertions.assertEquals(1, actualResult.size()),
                () -> Assertions.assertEquals(expected.getJobGroup(), actualResult.get(0).getJobGroup()),
                () -> Assertions.assertEquals(expected.getJobName(), actualResult.get(0).getJobName()),
                () -> Assertions.assertEquals(expected.getRunsNumberToConsiderForHanging(),
                        actualResult.get(0).getRunsNumberToConsiderForHanging()),
                () -> Assertions.assertEquals(expected.getRunsNumberToConsiderForFailing(),
                        actualResult.get(0).getRunsNumberToConsiderForFailing()),
                () -> Assertions.assertEquals(expected.getMaxDelayTime(), actualResult.get(0).getMaxDelayTime()),
                () -> Assertions.assertEquals(expected.getWarnDelayTime(), actualResult.get(0).getWarnDelayTime()),
                () -> Assertions.assertEquals(expected.getMaxExecutionTime(),
                        actualResult.get(0).getMaxExecutionTime()),
                () -> Assertions.assertEquals(expected.getWarnExecutionTime(),
                        actualResult.get(0).getWarnExecutionTime()),
                () -> Assertions.assertEquals(expected.getMaxFailedRuns(), actualResult.get(0).getMaxFailedRuns()),
                () -> Assertions.assertEquals(expected.getWarnFailedRuns(),
                        actualResult.get(0).getWarnFailedRuns())
        );
    }

    @Test
    @DatabaseSetup(value = "/db/dao/job_monitoring_config/empty-config.xml", connection = "schedulerConnection")
    @ExpectedDatabase(value = "/db/dao/job_monitoring_config/empty-config.xml", connection = "schedulerConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void getJobMonitoringConfigWhenConfigDoesNotExistTest() {
        List<JobMonitoringConfigRow> actualResult = dao.getJobMonitoringConfig();
        Assertions.assertEquals(0, actualResult.size());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/job_monitoring_config/before.xml", connection = "schedulerConnection")
    @ExpectedDatabase(value = "/db/dao/job_monitoring_config/before.xml", connection = "schedulerConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void hasJobMonitoringConfigExistedWhenConfigDoesNotExistTest() {
        boolean actualResult = dao.hasJobMonitoringConfigExisted(JOB_NAME);
        Assertions.assertTrue(actualResult);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/job_monitoring_config/empty-config.xml", connection = "schedulerConnection")
    @ExpectedDatabase(value = "/db/dao/job_monitoring_config/empty-config.xml", connection = "schedulerConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void hasJobMonitoringConfigExistedWhenConfigExistsTest() {
        boolean actualResult = dao.hasJobMonitoringConfigExisted(JOB_NAME);
        Assertions.assertFalse(actualResult);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/job_monitoring_config/empty-job.xml", connection = "schedulerConnection")
    @ExpectedDatabase(value = "/db/dao/job_monitoring_config/empty-job.xml", connection = "schedulerConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void hasJobExistedWhenJobDoesNotExistTest() {
        boolean actualResult = dao.hasJobExisted(JOB_GROUP, JOB_NAME);
        Assertions.assertFalse(actualResult);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/job_monitoring_config/empty-config.xml", connection = "schedulerConnection")
    @ExpectedDatabase(value = "/db/dao/job_monitoring_config/before.xml", connection = "schedulerConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void saveJobMonitoringConfigTest() {
        dao.saveJobMonitoringConfig(JOB_GROUP, JOB_NAME, CONFIG_DTO);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/job_monitoring_config/before-two-jobs.xml", connection = "schedulerConnection")
    @ExpectedDatabase(value = "/db/dao/job_monitoring_config/after-two-jobs.xml", connection = "schedulerConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void updateJobMonitoringConfigTest() {
        dao.updateJobMonitoringConfig(JOB_GROUP, JOB_NAME, CHANGED_CONFIG_DTO);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/job_monitoring_config/before.xml", connection = "schedulerConnection")
    @ExpectedDatabase(value = "/db/dao/job_monitoring_config/empty-config.xml", connection = "schedulerConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void deleteJobMonitoringConfigTest() {
        dao.deleteJobMonitoringConfig(JOB_GROUP, JOB_NAME);
    }
}
