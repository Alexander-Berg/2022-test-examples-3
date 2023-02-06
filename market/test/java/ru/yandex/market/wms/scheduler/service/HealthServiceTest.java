package ru.yandex.market.wms.scheduler.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.market.wms.common.spring.exception.BadRequestTitledException;
import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;
import ru.yandex.market.wms.scheduler.dao.JobMonitoringConfigDao;
import ru.yandex.market.wms.scheduler.dao.QuartzHistoryDao;
import ru.yandex.market.wms.scheduler.dao.entity.JobMonitoringConfigRow;
import ru.yandex.market.wms.scheduler.dto.JobMonitoringConfigDto;
import ru.yandex.market.wms.scheduler.service.calculator.FailedJobsCalculator;
import ru.yandex.market.wms.scheduler.service.calculator.JobsDelayCalculator;
import ru.yandex.market.wms.scheduler.service.calculator.JobsExecutionTimeCalculator;
import ru.yandex.market.wms.scheduler.service.calculator.PausedJobsCalculator;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HealthServiceTest extends SchedulerIntegrationTest {

    private static final String JOB_GROUP = "clean";
    private static final String JOB_NAME = "CleanSkuLocJob";
    private static final int RUNS_NUMBER_TO_CONSIDER = 1;
    private static final int RUNS_NUMBER_TO_CONSIDER_FOR_HANGING = 5;
    private static final long MAX_DELAY_TIME = 60 * 60 * 24;
    private static final long WARN_DELAY_TIME = MAX_DELAY_TIME;
    private static final long MAX_EXECUTION_TIME = 60L;
    private static final long WARN_EXECUTION_TIME = MAX_EXECUTION_TIME;
    private static final int MAX_FAILED_RUNS = 1;
    private static final int WARN_FAILED_RUNS = MAX_FAILED_RUNS;
    private static final int MAX_EXECUTION_RUNS = 5;
    private static final int WARN_EXECUTION_RUNS = 3;
    private static final JobMonitoringConfigDto CONFIG_DTO = JobMonitoringConfigDto.builder()
            .maxDelayTime(100).warnDelayTime(50).maxExecutionTime(100)
            .warnExecutionTime(50).maxFailedRuns(1).warnFailedRuns(1)
            .runsNumberToConsiderForHanging(1).runsNumberToConsiderForFailing(1)
            .build();
    private static final JobMonitoringConfigDto DEFAULT_CONFIG_DTO = JobMonitoringConfigDto.builder()
            .maxDelayTime(MAX_DELAY_TIME)
            .warnDelayTime(WARN_DELAY_TIME)
            .maxExecutionTime(MAX_EXECUTION_TIME)
            .warnExecutionTime(WARN_EXECUTION_TIME)
            .maxFailedRuns(MAX_FAILED_RUNS)
            .warnFailedRuns(WARN_FAILED_RUNS)
            .maxExecutionRuns(MAX_EXECUTION_RUNS)
            .warnExecutionRuns(WARN_EXECUTION_RUNS)
            .runsNumberToConsiderForHanging(RUNS_NUMBER_TO_CONSIDER_FOR_HANGING)
            .runsNumberToConsiderForFailing(RUNS_NUMBER_TO_CONSIDER)
            .build();
    private static final JobMonitoringConfigRow CONFIG_ROW = JobMonitoringConfigRow.builder()
            .jobGroup(JOB_GROUP).jobName(JOB_NAME).maxDelayTime(100).warnDelayTime(50).maxExecutionTime(100)
            .warnExecutionTime(50).maxFailedRuns(1).warnFailedRuns(1)
            .runsNumberToConsiderForFailing(1).runsNumberToConsiderForFailing(1)
            .build();

    @Autowired
    HealthService healthService;
    @SpyBean
    @Autowired
    private ConfigCache configCache;
    @MockBean
    @Autowired
    private QuartzHistoryDao qrtzHistoryDao;
    @MockBean
    @Autowired
    private FailedJobsCalculator failedJobsCalculator;
    @MockBean
    @Autowired
    private JobsExecutionTimeCalculator jobsExecutionTimeCalculator;
    @MockBean
    @Autowired
    private JobsDelayCalculator jobsDelayCalculator;
    @MockBean
    @Autowired
    private PausedJobsCalculator pausedJobsCalculator;
    @MockBean
    @Autowired
    private SchedulerService schedulerService;
    @MockBean
    @Autowired
    private JobMonitoringConfigDao monitoringConfigDao;

    private static Stream<Arguments> provideArguments() {
        return Stream.of(
                Arguments.of(true, true, 1, 1, 1, 0),
                Arguments.of(true, false, 1, 1, 0, 1),
                Arguments.of(false, false, 0, 0, 0, 0)
        );
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(qrtzHistoryDao);
        Mockito.reset(failedJobsCalculator);
        Mockito.reset(jobsExecutionTimeCalculator);
        Mockito.reset(jobsDelayCalculator);
        Mockito.reset(pausedJobsCalculator);
        Mockito.reset(schedulerService);
        Mockito.reset(monitoringConfigDao);
        Mockito.reset(configCache);
    }

    @Test
    void getMonitoringConfigsWhenConfigNotExistTest() {
        Optional<JobMonitoringConfigDto> actualResult = healthService.getMonitoringConfigs(JOB_GROUP, JOB_NAME);
        Assertions.assertEquals(Optional.empty(), actualResult);
        verifyDaoMock(0, 1, 0, 0, 0, 0, CONFIG_DTO);
    }

    @Test
    void getMonitoringConfigsWhenConfigNotExistButJobExistTest() {
        setUpDaoMock(true, false, List.of());
        Optional<JobMonitoringConfigDto> actualResult = healthService.getMonitoringConfigs(JOB_GROUP, JOB_NAME);
        Assertions.assertEquals(DEFAULT_CONFIG_DTO, actualResult.get());
        verifyDaoMock(0, 1, 0, 0, 0, 0, CONFIG_DTO);
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    void updateMonitoringConfigsTest(
            boolean hasJobExisted,
            boolean hasConfigExisted,
            int getConfigTimes,
            int hasConfigExistedTimes,
            int updateConfigTimes,
            int saveConfigTimes
    ) {
        setUpDaoMock(hasJobExisted, hasConfigExisted, Lists.list(CONFIG_ROW));
        healthService.updateMonitoringConfigs(JOB_GROUP, JOB_NAME, CONFIG_DTO);
        verifyDaoMock(getConfigTimes, 1, hasConfigExistedTimes, updateConfigTimes, saveConfigTimes, 0, CONFIG_DTO);
    }

    @Test
    void updateMonitoringConfigsRunsLessThanWarnFailing() {
        JobMonitoringConfigDto configDto = JobMonitoringConfigDto.builder()
                .maxDelayTime(MAX_DELAY_TIME).warnDelayTime(WARN_DELAY_TIME).maxExecutionTime(MAX_EXECUTION_TIME)
                .warnExecutionTime(WARN_EXECUTION_TIME).maxFailedRuns(MAX_FAILED_RUNS).warnFailedRuns(2)
                .runsNumberToConsiderForHanging(RUNS_NUMBER_TO_CONSIDER)
                .runsNumberToConsiderForFailing(1).build();
        setUpDaoMock(true, true, Lists.list(CONFIG_ROW));
        Assertions.assertThrows(BadRequestTitledException.class, () ->
            healthService.updateMonitoringConfigs(JOB_GROUP, JOB_NAME, configDto)
        );
    }

    @Test
    void updateMonitoringConfigsRunsLessThanMaxFailing() {
        JobMonitoringConfigDto configDto = JobMonitoringConfigDto.builder()
                .maxDelayTime(MAX_DELAY_TIME).warnDelayTime(WARN_DELAY_TIME).maxExecutionTime(MAX_EXECUTION_TIME)
                .warnExecutionTime(WARN_EXECUTION_TIME).maxFailedRuns(2).warnFailedRuns(WARN_FAILED_RUNS)
                .runsNumberToConsiderForHanging(RUNS_NUMBER_TO_CONSIDER)
                .runsNumberToConsiderForFailing(1).build();
        setUpDaoMock(true, true, Lists.list(CONFIG_ROW));
        Assertions.assertThrows(BadRequestTitledException.class, () ->
                healthService.updateMonitoringConfigs(JOB_GROUP, JOB_NAME, configDto)
        );
    }

    @Test
    void deleteMonitoringConfigsTest() {
        healthService.deleteMonitoringConfigs(JOB_GROUP, JOB_NAME);
        verifyDaoMock(1, 0, 0, 0, 0, 1, CONFIG_DTO);
    }

    private void setUpDaoMock(
            boolean hasJobExisted,
            boolean hasConfigExisted,
            List<JobMonitoringConfigRow> jobMonitoringConfigRowList
    ) {
        when(monitoringConfigDao.hasJobExisted(anyString(), anyString())).thenReturn(false);
        when(monitoringConfigDao.hasJobExisted(JOB_GROUP, JOB_NAME)).thenReturn(hasJobExisted);
        when(monitoringConfigDao.hasJobMonitoringConfigExisted(anyString())).thenReturn(false);
        when(monitoringConfigDao.hasJobMonitoringConfigExisted(JOB_NAME)).thenReturn(hasConfigExisted);
        when(monitoringConfigDao.getJobMonitoringConfig()).thenReturn(jobMonitoringConfigRowList);
    }

    private void verifyDaoMock(
            int getConfigTimes,
            int hasJobExistedTimes,
            int hasConfigExistedTimes,
            int updateConfigTimes,
            int saveConfigTimes,
            int deleteConfigTimes,
            JobMonitoringConfigDto configDto
    ) {
        verify(monitoringConfigDao, times(getConfigTimes)).getJobMonitoringConfig();
        verify(monitoringConfigDao, times(hasJobExistedTimes)).hasJobExisted(anyString(), anyString());
        verify(monitoringConfigDao, times(hasConfigExistedTimes)).hasJobMonitoringConfigExisted(anyString());
        verify(monitoringConfigDao, times(updateConfigTimes))
                .updateJobMonitoringConfig(anyString(), anyString(), eq(configDto));
        verify(monitoringConfigDao, times(saveConfigTimes))
                .saveJobMonitoringConfig(anyString(), anyString(), eq(configDto));
        verify(monitoringConfigDao, times(deleteConfigTimes)).deleteJobMonitoringConfig(anyString(), anyString());
    }
}
