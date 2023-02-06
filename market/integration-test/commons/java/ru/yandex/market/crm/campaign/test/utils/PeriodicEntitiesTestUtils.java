package ru.yandex.market.crm.campaign.test.utils;

import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import ru.yandex.market.tms.quartz2.ExclusiveExecutorJob;
import ru.yandex.market.tms.quartz2.model.Executor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author apershukov
 */
public final class PeriodicEntitiesTestUtils {

    public static void startTask(Executor task, String entityId) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("entity_id", entityId);

        JobDetail jobDetail = JobBuilder.newJob(ExclusiveExecutorJob.class)
                .setJobData(jobDataMap)
                .build();

        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getJobDetail()).thenReturn(jobDetail);

        task.doJob(context);
    }
}
