package ru.yandex.market.tms.quartz2.model;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.LocalDataSourceJobStore;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.config.FunctionalTest;
import ru.yandex.market.tms.quartz2.service.JobService;

/**
 * @author ogonek
 * @date 22.08.18
 */
@DbUnitDataSet(
        before = "QrtzTmsGetAllRunningJobsWithoutDataMapTest.before.csv"
)
@ActiveProfiles("testing")
class QrtzTmsGetAllJobsInfoWithoutDataMapTest extends FunctionalTest {

    @Autowired
    private JobService jobService;

    @Autowired
    private Scheduler scheduler;

    /**
     * Проверяет, что getAllJobsInfoWithoutDataMap достает правильную информацию по всем джобам из шедулера.
     */
    @Test
    void getAllJobsInfoWithoutDataMap() throws SchedulerException {

        Class currentJobStore = scheduler.getMetaData().getJobStoreClass();
        Assertions.assertEquals(LocalDataSourceJobStore.class, currentJobStore);

        Collection<JobInfo> jobs = jobService.getAllJobs();

        List<String> jobsInfo = ImmutableList.of(
                Iterables.getFirst(jobs, null).getJobName(),
                Iterables.getLast(jobs, null).getJobName()
        );

        MatcherAssert.assertThat(jobsInfo, Matchers.containsInAnyOrder("testExecutor", "testExecutor4"));
    }
}
