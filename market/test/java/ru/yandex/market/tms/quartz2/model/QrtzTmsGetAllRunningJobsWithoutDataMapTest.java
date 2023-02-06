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
 * Проверяет, выбираются ли из таблицы только все работающие в данный момент джобы.
 *
 * @author ogonek 24.08.2018
 */
@DbUnitDataSet(
        before = "QrtzTmsGetAllRunningJobsWithoutDataMapTest.before.csv"
)
@ActiveProfiles("testing")
class QrtzTmsGetAllRunningJobsWithoutDataMapTest extends FunctionalTest {

    @Autowired
    private JobService jobService;

    @Autowired
    private Scheduler scheduler;

    @Test
    void GetAllRunningJobsInfoWithoutDataMap() throws SchedulerException {

        Class currentJobStore = scheduler.getMetaData().getJobStoreClass();
        Assertions.assertEquals(LocalDataSourceJobStore.class, currentJobStore);

        Collection<JobInfo> jobs = jobService.getRunningJobs();

        List<String> jobsInfo = ImmutableList.of(
                Iterables.getFirst(jobs, null).getJobName(),
                Iterables.getLast(jobs, null).getJobName()
        );

        MatcherAssert.assertThat(jobsInfo, Matchers.containsInAnyOrder("testExecutor", "testExecutor2"));
    }

}
