package ru.yandex.market.hrms.tms.executor.sync;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.hrms.tms.AbstractTmsTest;

public class HrOperationSyncExecutorTest extends AbstractTmsTest {
    @Autowired
    private Scheduler scheduler;

    @Test
    void shouldRegisterSyncEmployeesTask() throws SchedulerException {
        JobDetail employeeStaffSyncExecutor = scheduler.getJobDetail(new JobKey("hrOperationSyncExecutor"));
        MatcherAssert.assertThat(employeeStaffSyncExecutor, CoreMatchers.notNullValue());
    }
}
