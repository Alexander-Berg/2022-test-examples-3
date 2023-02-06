package ru.yandex.market.tsum.pipe.engine.runtime;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.stage.StageGroup;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobExecutorSucceededEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobRunningEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobSucceededEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.ScheduleChangeEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.SubscribersSucceededEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.ToggleSchedulerConstraintModifyEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.TriggerEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.BeanRegistrar;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.DummyFullJobIdFactory;
import ru.yandex.market.tsum.pipe.engine.runtime.state.calculator.PipeStateCalculatorTestBase;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;

import javax.inject.Named;
import java.time.Instant;

/**
 * @author Mishunin Andrei <a href="mailto:mishunin@yandex-team.ru"></a>
 * @date 25.03.2019
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class, WaitingForSchedulerTest.Config.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class WaitingForSchedulerTest extends PipeStateCalculatorTestBase {
    private static final String FIRST_JOB = "first";
    private static final String SECOND_JOB = "second";
    private static final String FIRST_STAGE = "first";
    private static final String SECOND_STAGE = "second";
    private static final String STAGE_GROUP_ID = "test-stages";

    @Autowired
    @Named(STAGE_GROUP_ID)
    private StageGroup stageGroup;

    @Test
    public void blocksExecution() {
        String pipeId = BeanRegistrar.registerNamedBean(pipeline(false, false), applicationContext);
        String launchId = activateLaunch(pipeId);

        pipeStateService.recalc(launchId, new JobRunningEvent(FIRST_JOB, 1, DummyFullJobIdFactory.create()));
        pipeStateService.recalc(launchId, new JobExecutorSucceededEvent(FIRST_JOB, 1));
        pipeStateService.recalc(launchId, new SubscribersSucceededEvent(FIRST_JOB, 1));
        pipeStateService.recalc(launchId, new JobSucceededEvent(FIRST_JOB, 1));

        Assert.assertEquals(1, testJobScheduler.getTriggeredJobs().size());
        Assert.assertEquals(1, testJobWaitingScheduler.getTriggeredJobs().size());
        Assert.assertEquals(
            StatusChangeType.WAITING_FOR_SCHEDULE,
            pipeTester.getPipeLaunch(launchId).getJobState(SECOND_JOB).getLastStatusChangeType()
        );

        pipeStateService.recalc(launchId, new ScheduleChangeEvent(SECOND_JOB));
        Assert.assertEquals(2, testJobScheduler.getTriggeredJobs().size());
    }

    @Test
    public void triggerEventTest() {
        String pipeId = BeanRegistrar.registerNamedBean(pipeline(false, false), applicationContext);
        String launchId = activateLaunch(pipeId);

        pipeStateService.recalc(launchId, new JobRunningEvent(FIRST_JOB, 1, DummyFullJobIdFactory.create()));
        pipeStateService.recalc(launchId, new JobExecutorSucceededEvent(FIRST_JOB, 1));
        pipeStateService.recalc(launchId, new SubscribersSucceededEvent(FIRST_JOB, 1));
        pipeStateService.recalc(launchId, new JobSucceededEvent(FIRST_JOB, 1));

        Assert.assertEquals(1, testJobScheduler.getTriggeredJobs().size());
        Assert.assertEquals(1, testJobWaitingScheduler.getTriggeredJobs().size());

        pipeStateService.recalc(launchId, new TriggerEvent(SECOND_JOB, "test_user", false));
        Assert.assertEquals(2, testJobScheduler.getTriggeredJobs().size());
        Assert.assertEquals(1, pipeLaunchDao.get(launchId).getJobState(SECOND_JOB).getLaunches().size());
        Assert.assertEquals(
            "test_user",
            pipeLaunchDao.get(launchId).getJobState(SECOND_JOB).getLastLaunch().getTriggeredBy()
        );
    }

    @Test
    public void toggleSchedulerConstraintTest() {
        String pipeId = BeanRegistrar.registerNamedBean(pipeline(false, false), applicationContext);
        String launchId = activateLaunch(pipeId);

        pipeStateService.recalc(launchId, new JobRunningEvent(FIRST_JOB, 1, DummyFullJobIdFactory.create()));
        pipeStateService.recalc(launchId, new JobExecutorSucceededEvent(FIRST_JOB, 1));
        pipeStateService.recalc(launchId, new SubscribersSucceededEvent(FIRST_JOB, 1));
        pipeStateService.recalc(launchId,
            new ToggleSchedulerConstraintModifyEvent(SECOND_JOB, USERNAME, Instant.now()));
        pipeStateService.recalc(launchId, new JobSucceededEvent(FIRST_JOB, 1));

        Assert.assertEquals(2, testJobScheduler.getTriggeredJobs().size());
        Assert.assertEquals(0, testJobWaitingScheduler.getTriggeredJobs().size());
    }

    @Test
    public void schedulerForStageTest() {
        String pipeId = BeanRegistrar.registerNamedBean(pipeline(true, false), applicationContext);
        Pipeline pipeline = pipeProvider.get(pipeId);
        pipeTester.createStageGroupState(STAGE_GROUP_ID, pipeline.getStages());
        String launchId = activateLaunch(pipeId, STAGE_GROUP_ID);

        pipeStateService.recalc(launchId, new JobRunningEvent(FIRST_JOB, 1, DummyFullJobIdFactory.create()));
        pipeStateService.recalc(launchId, new JobExecutorSucceededEvent(FIRST_JOB, 1));
        pipeStateService.recalc(launchId, new SubscribersSucceededEvent(FIRST_JOB, 1));
        pipeStateService.recalc(launchId, new JobSucceededEvent(FIRST_JOB, 1));

        Assert.assertEquals(1, testJobScheduler.getTriggeredJobs().size());
        Assert.assertEquals(1, testJobWaitingScheduler.getTriggeredJobs().size());

        pipeStateService.recalc(launchId, new ScheduleChangeEvent(SECOND_JOB));
        Assert.assertEquals(2, testJobScheduler.getTriggeredJobs().size());
    }

    @Test
    public void skipSchedulerForManualTriggerTest() {
        String pipeId = BeanRegistrar.registerNamedBean(pipeline(true, true), applicationContext);
        Pipeline pipeline = pipeProvider.get(pipeId);
        pipeTester.createStageGroupState(STAGE_GROUP_ID, pipeline.getStages());
        String launchId = activateLaunch(pipeId, STAGE_GROUP_ID);

        pipeStateService.recalc(launchId, new JobRunningEvent(FIRST_JOB, 1, DummyFullJobIdFactory.create()));
        pipeStateService.recalc(launchId, new JobExecutorSucceededEvent(FIRST_JOB, 1));
        pipeStateService.recalc(launchId, new SubscribersSucceededEvent(FIRST_JOB, 1));
        pipeStateService.recalc(launchId, new JobSucceededEvent(FIRST_JOB, 1));

        Assert.assertEquals(1, testJobScheduler.getTriggeredJobs().size());
        Assert.assertEquals(0, testJobWaitingScheduler.getTriggeredJobs().size());

        pipeStateService.recalc(launchId, new TriggerEvent(SECOND_JOB, "test_user", false));

        pipeStateService.recalc(launchId, new ScheduleChangeEvent(SECOND_JOB));
        Assert.assertEquals(2, testJobScheduler.getTriggeredJobs().size());
        Assert.assertEquals(0, testJobWaitingScheduler.getTriggeredJobs().size());
    }

    @Test
    public void schedulerForManualTriggerTest() {
        String pipeId = BeanRegistrar.registerNamedBean(pipeline(false, true), applicationContext);
        String launchId = activateLaunch(pipeId);

        pipeStateService.recalc(launchId, new JobRunningEvent(FIRST_JOB, 1, DummyFullJobIdFactory.create()));
        pipeStateService.recalc(launchId, new JobExecutorSucceededEvent(FIRST_JOB, 1));
        pipeStateService.recalc(launchId, new SubscribersSucceededEvent(FIRST_JOB, 1));
        pipeStateService.recalc(launchId, new JobSucceededEvent(FIRST_JOB, 1));

        Assert.assertEquals(1, testJobScheduler.getTriggeredJobs().size());
        Assert.assertEquals(0, testJobWaitingScheduler.getTriggeredJobs().size());

        pipeStateService.recalc(launchId, new TriggerEvent(SECOND_JOB, "test_user", false));

        pipeStateService.recalc(launchId, new ScheduleChangeEvent(SECOND_JOB));
        Assert.assertEquals(2, testJobScheduler.getTriggeredJobs().size());
        Assert.assertEquals(0, testJobWaitingScheduler.getTriggeredJobs().size());
    }

    private Pipeline pipeline(boolean staged, boolean manualTrigger) {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder first = builder.withJob(DummyJob.class, FIRST_JOB);
        if (staged) {
            first.beginStage(stageGroup.getStage(FIRST_STAGE));
        }

        JobBuilder second = builder.withJob(DummyJob.class, SECOND_JOB)
            .withUpstreams(first)
            .withScheduler()
            .build();

        if (staged) {
            second.beginStage(stageGroup.getStage(SECOND_STAGE));
        }

        if (manualTrigger) {
            second.withManualTrigger();
        }

        return builder.build();
    }

    @Configuration
    public static class Config {
        @Bean(name = WaitingForSchedulerTest.STAGE_GROUP_ID)
        public StageGroup stages() {
            return new StageGroup(FIRST_STAGE, SECOND_STAGE);
        }
    }

}
