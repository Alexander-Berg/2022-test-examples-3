package ru.yandex.market.tms.quartz2.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.simpl.RAMJobStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.tms.quartz2.config.FunctionalTest;
import ru.yandex.market.tms.quartz2.service.JobService;

@ActiveProfiles("development")
class QrtzTmsGetAllRunningJobsInDevTest extends FunctionalTest {

    @Autowired
    private JobService jobService;

    @Autowired
    private Scheduler scheduler;

    @Test
    void GetAllRunningJobsInfoWithoutDataMap() throws SchedulerException {

        Class currentJobStore = scheduler.getMetaData().getJobStoreClass();
        Assertions.assertEquals(RAMJobStore.class, currentJobStore);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> jobService.getRunningJobs()
        );

    }
}
