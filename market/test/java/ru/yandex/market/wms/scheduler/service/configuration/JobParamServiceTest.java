package ru.yandex.market.wms.scheduler.service.configuration;

import java.util.List;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.Job;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;
import ru.yandex.market.wms.scheduler.dao.entity.CleanOldReportsParam;
import ru.yandex.market.wms.scheduler.dao.entity.JobParam;
import ru.yandex.market.wms.scheduler.dto.JobParamDto;
import ru.yandex.market.wms.scheduler.job.CleanOldTransportOrdersJob;
import ru.yandex.market.wms.scheduler.service.SchedulerService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobParamServiceTest extends SchedulerIntegrationTest {

    private static final String JOB_GROUP = "clean";
    private static final String JOB_NAME = "CleanOldTransportOrdersJob";
    private static final JobParamDto PARAM_DTO_1 = JobParamDto.builder().name("partitionSize").value(1000)
            .minValue(JobParam.MIN_PARTITION_SIZE).maxValue(JobParam.MAX_PARTITION_SIZE).build();
    private static final JobParamDto PARAM_DTO_2 = JobParamDto.builder().name("daysThreshold").value(7)
            .minValue(JobParam.MIN_DAYS_THRESHOLD).maxValue(JobParam.MAX_DAYS_THRESHOLD).build();
    private static final JobParamDto PARAM_DTO_3 = JobParamDto.builder().name("deletingTimeout").value(120)
            .minValue(JobParam.MIN_TIMEOUT).maxValue(360).build();
    private static final JobParamDto PARAM_DTO_4 = JobParamDto.builder().name("partitionSize").value(555).build();
    private static final JobParamDto PARAM_DTO_5 = JobParamDto.builder().name("daysThreshold").value(5).build();
    private static final JobParamDto PARAM_DTO_6 = JobParamDto.builder().name("deletingTimeout").value(55).build();
    // Параметры с неправильным именем, значениями больше максимального и меньше минимального
    private static final JobParamDto PARAM_DTO_7 = JobParamDto.builder().name("daysThreshold").value(-100).build();
    private static final JobParamDto PARAM_DTO_8 = JobParamDto.builder().name("deletingTimeout").value(500).build();
    private static final JobParamDto PARAM_DTO_9 = JobParamDto.builder().name("batchSize").value(50_000).build();
    List<JobParamDto> jobParamDtoList = List.of(PARAM_DTO_4, PARAM_DTO_5, PARAM_DTO_6);
    List<JobParamDto> wrongJobParamDtoList = List.of(PARAM_DTO_7, PARAM_DTO_8, PARAM_DTO_9);

    @Autowired
    private JobParamService jobParamService;
    @SpyBean
    @Autowired
    private DbConfigService dbConfigService;
    @MockBean
    @Autowired
    private SchedulerService schedulerService;
    @MockBean
    @Autowired
    private CleanOldTransportOrdersJob job;

    @BeforeEach
    void setUp() {
        Mockito.reset(schedulerService);
        Mockito.reset(job);
        Mockito.reset(dbConfigService);
    }

    @Test
    void getJobParamsWhenJobWasFoundTest() throws SchedulerException {
        setUpMock(JOB_GROUP, JOB_NAME, Optional.of(CleanOldTransportOrdersJob.class),
                List.of(CleanOldReportsParam.values()));
        List<JobParamDto> expected = List.of(PARAM_DTO_1, PARAM_DTO_2, PARAM_DTO_3);
        List<JobParamDto> actualResult = jobParamService.getJobParams(JOB_GROUP, JOB_NAME);
        assertAll(
                () -> Assertions.assertEquals(expected.size(), actualResult.size()),
                () -> Assertions.assertTrue(actualResult.contains(PARAM_DTO_1)),
                () -> Assertions.assertTrue(actualResult.contains(PARAM_DTO_2)),
                () -> Assertions.assertTrue(actualResult.contains(PARAM_DTO_3))
        );
        verifyMock(1, JOB_GROUP, JOB_NAME, 1, 0);
    }

    @Test
    void getJobParamsWhenJobWasNotFoundTest() throws SchedulerException {
        String jobGroup = "GROUP";
        String jobName = "NAME";
        setUpMock("", "", Optional.empty(), List.of());
        List<JobParamDto> actualResult = jobParamService.getJobParams(jobGroup, jobName);
        assertAll(
                () -> Assertions.assertEquals(0, actualResult.size())
        );
        verifyMock(1, jobGroup, jobName, 0, 0);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/job-params/before.xml", connection = "schedulerConnection")
    @ExpectedDatabase(value = "/db/dao/job-params/after.xml", connection = "schedulerConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void updateJobParamsWhenJobWasFoundTest() throws SchedulerException {
        setUpMock(JOB_GROUP, JOB_NAME, Optional.of(CleanOldTransportOrdersJob.class),
                List.of(CleanOldReportsParam.values()));
        jobParamService.updateJobParams(JOB_GROUP, JOB_NAME, jobParamDtoList);
        verifyMock(1, JOB_GROUP, JOB_NAME, 1, 1);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/job-params/before.xml", connection = "schedulerConnection")
    @ExpectedDatabase(value = "/db/dao/job-params/before.xml", connection = "schedulerConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void updateJobParamsWhenJobWasFoundAndParamsHaveWrongValuesTest() throws SchedulerException {
        setUpMock(JOB_GROUP, JOB_NAME, Optional.of(CleanOldTransportOrdersJob.class),
                List.of(CleanOldReportsParam.values()));
        jobParamService.updateJobParams(JOB_GROUP, JOB_NAME, wrongJobParamDtoList);
        verifyMock(1, JOB_GROUP, JOB_NAME, 1, 0);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/job-params/before.xml", connection = "schedulerConnection")
    @ExpectedDatabase(value = "/db/dao/job-params/before.xml", connection = "schedulerConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void updateJobParamsWhenJobWasNotFoundTest() throws SchedulerException {
        setUpMock(JOB_GROUP, JOB_NAME, Optional.empty(), List.of());
        jobParamService.updateJobParams(JOB_GROUP, JOB_NAME, jobParamDtoList);
        verifyMock(1, JOB_GROUP, JOB_NAME, 0, 0);
    }

    private void setUpMock(
            String jobGroup,
            String jobName,
            Optional<Class<? extends Job>> optionalClass,
            List<JobParam> jobParams
    ) throws SchedulerException {
        when(schedulerService.getJobClassName(anyString(), anyString())).thenReturn(Optional.empty());
        when(schedulerService.getJobClassName(jobGroup, jobName)).thenReturn(optionalClass);
        when(job.getJobParams()).thenReturn(jobParams);
    }

    private void verifyMock(
            int getJobClassNameTimes,
            String jobGroup,
            String jobName,
            int getJobParamsTimes,
            int updateValuesByKeysTimes
    ) throws SchedulerException {
        verify(schedulerService, times(getJobClassNameTimes)).getJobClassName(jobGroup, jobName);
        verify(job, times(getJobParamsTimes)).getJobParams();
        verify(dbConfigService, times(updateValuesByKeysTimes)).updateValuesByKeys(anyMap());
    }
}
