package ru.yandex.market.ff.listener;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.configuration.QuartzDatabaseDatasourceConfig;
import ru.yandex.market.ff.health.model.JobLogRow;
import ru.yandex.market.ff.health.model.JobMonitoringConfigRow;
import ru.yandex.market.ff.health.repository.JobInfoRepository;
import ru.yandex.market.ff.tms.util.ContextRequestIdAppender;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = QuartzDatabaseDatasourceConfig.class)
public class JobContextListenerTest extends IntegrationTest {
    @Autowired
    JobInfoRepository jobInfoRepository;

    @Test
    @DatabaseSetup("classpath:tms/job-context-listener/before.xml")
    public void requestIdSaved()
            throws InvocationTargetException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        String jobName = "veryUsefulJob";
        scheduleJobNow(Append42ShopIdTestSuccessExecutor.class, jobName);

        List<JobLogRow> lastLogForJobByConfig =
                jobInfoRepository.getLastLogForJobByConfig(new JobMonitoringConfigRow(jobName, 0L, 0L, 1));

        String requestId = lastLogForJobByConfig.get(0).getRequestId();
        assertNotNull(requestId);

        assertions.assertThat(lastLogForJobByConfig.get(0).getJobStatus())
                .doesNotContain("requestId:" + requestId + "/42");
    }


    @Test
    @DatabaseSetup("classpath:tms/job-context-listener/before.xml")
    public void requestIdIsCleanAfterExecution() throws SchedulerException, InterruptedException {
        String jobName = "veryUsefulJob";
        scheduleJobNowViaScheduler(EmptyTestSuccessExecutor.class, jobName);

        List<JobLogRow> lastLogForJobByConfig =
                jobInfoRepository.getLastLogForJobByConfig(new JobMonitoringConfigRow(jobName, 0L, 0L, 1));

        String requestId = lastLogForJobByConfig.get(0).getRequestId();
        assertNotNull(requestId);
        assertNull(RequestContextHolder.getContext().getRequestId());
    }

    @Test
    @DatabaseSetup("classpath:tms/job-context-listener/before.xml")
    public void shopIdNotSavedSuccessExecution()
            throws InvocationTargetException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        String jobName = "veryUsefulJob";
        scheduleJobNow(Append42ShopIdTestSuccessExecutor.class, jobName);

        List<JobLogRow> lastLogForJobByConfig =
                jobInfoRepository.getLastLogForJobByConfig(new JobMonitoringConfigRow(jobName, 0L, 0L, 1));

        String requestId = lastLogForJobByConfig.get(0).getRequestId();
        assertNotNull(requestId);

        assertions.assertThat(lastLogForJobByConfig.get(0).getJobStatus())
                .doesNotContain("requestId:" + requestId + "/42");
    }

    @Test
    @DatabaseSetup("classpath:tms/job-context-listener/before.xml")
    public void shopIdSavedFailedExecution() throws InvocationTargetException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        String jobName = "veryUsefulJob";
        scheduleJobNow(Append42ShopIdTestFailedExecutor.class, jobName);

        List<JobLogRow> lastLogForJobByConfig =
                jobInfoRepository.getLastLogForJobByConfig(new JobMonitoringConfigRow(jobName, 0L, 0L, 1));

        String requestId = lastLogForJobByConfig.get(0).getRequestId();
        assertNotNull(requestId);

        assertions.assertThat(lastLogForJobByConfig.get(0).getJobStatus()).contains("requestId:" + requestId + "/42");
    }

    private void scheduleJobNowViaScheduler(Class jobClass, String jobName)
            throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobName)
                .build();

        Trigger trigger = TriggerBuilder
                .newTrigger()
                .startNow()
                .build();

        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.getListenerManager().addJobListener(new JobContextListener(jobInfoRepository));

        JobListener schedulerJobListener = Mockito.mock(JobListener.class);
        when(schedulerJobListener.getName()).thenReturn("testSchedulerJobListener");
        scheduler.getListenerManager().addJobListener(schedulerJobListener);

        scheduler.start();
        scheduler.scheduleJob(jobDetail, trigger);

        Mockito.verify(schedulerJobListener, after(100L).atLeastOnce()).jobWasExecuted(any(), any());
    }

    private void scheduleJobNow(Class jobClass, String jobName)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Object jobInstance = jobClass.getDeclaredConstructor().newInstance();

        Job job = (Job) jobInstance;

        executeJob(job, jobName);
    }

    private void executeJob(Job job, String jobName) {
        JobContextListener jobContextListener = new JobContextListener(jobInfoRepository);

        JobExecutionContext jobExecutionContext = Mockito.mock(JobExecutionContext.class);
        JobDetail jobDetail = Mockito.mock(JobDetail.class);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("RECORD_ID", 10100L);
        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        when(jobDetail.getKey()).thenReturn(new JobKey(jobName));
        when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);

        jobContextListener.jobToBeExecuted(jobExecutionContext);

        JobExecutionException executionException = null;
        try {
            job.execute(jobExecutionContext);

        } catch (JobExecutionException exception) {
            executionException = exception;
        }

        jobContextListener.jobWasExecuted(jobExecutionContext, executionException);
    }

    public static class Append42ShopIdTestSuccessExecutor implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            ContextRequestIdAppender.appendOrReplaceShopRequestId(42L);
        }
    }

    public static class Append42ShopIdTestFailedExecutor implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            ContextRequestIdAppender.appendOrReplaceShopRequestId(42L);

            throw new JobExecutionException("test job execution exception");
        }
    }

    public static class EmptyTestSuccessExecutor implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
        }
    }
}
