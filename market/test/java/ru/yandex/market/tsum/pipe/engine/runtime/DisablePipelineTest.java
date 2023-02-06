package ru.yandex.market.tsum.pipe.engine.runtime;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.stage.StageBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.stage.StageGroup;
import ru.yandex.market.tsum.pipe.engine.definition.subscriber.PipeSubscriber;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.events.ForceSuccessTriggerEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.ToggleJobManualSwitchEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.exceptions.PipelineDisabledException;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeStateService;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.simple.SimplePipe;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 26.07.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DisablePipelineTest {
    private static final String START_JOB_ID = "START";
    private static final String FIRST_JOB_ID = "FIRST_JOB";
    private static final String SECOND_JOB_ID = "SECOND_JOB";
    private static final String STAGE_GROUP_ID = "STAGE_GROUP_ID";

    @Autowired
    private PipeTester pipeTester;

    @Autowired
    private PipeStateService pipeStateService;

    @Autowired
    private PipeLaunchDao pipeLaunchDao;

    @Test(expected = PipelineDisabledException.class)
    public void manualTriggersDoesNotWorkOnDisabledPipeline() throws Exception {
        // arrange
        String pipeLaunchId = pipeTester.runPipeToCompletion(SimplePipe.PIPE_ID, Collections.emptyList());
        pipeStateService.disableLaunch(pipeLaunchId, false);

        // act
        pipeTester.triggerJob(pipeLaunchId, SimplePipe.JOB_ID);
    }

    @Test(expected = PipelineDisabledException.class)
    public void manualTriggerTogglingDoesNotWorkOnDisabledPipeline() throws Exception {
        // arrange
        String pipeLaunchId = pipeTester.runPipeToCompletion(SimplePipe.PIPE_ID, Collections.emptyList());
        pipeStateService.disableLaunch(pipeLaunchId, false);

        // act
        pipeTester.recalcPipeLaunch(
            pipeLaunchId, new ToggleJobManualSwitchEvent(SimplePipe.JOB_ID, "silly_user", Instant.now())
        );
    }

    @Test(expected = PipelineDisabledException.class)
    public void forceSuccessDoesNotWorkOnDisabledPipeline() throws Exception {
        // arrange
        String pipeLaunchId = pipeTester.runPipeToCompletion(SimplePipe.PIPE_ID, Collections.emptyList());
        pipeStateService.disableLaunch(pipeLaunchId, false);

        // act
        pipeTester.recalcPipeLaunch(
            pipeLaunchId, new ForceSuccessTriggerEvent(SimplePipe.JOB_ID, "silly_user")
        );
    }

    @Test
    public void enablePipeline() throws Exception {
        // arrange
        String pipeLaunchId = pipeTester.runPipeToCompletion(SimplePipe.PIPE_ID, Collections.emptyList());
        pipeStateService.disableLaunch(pipeLaunchId, false);

        // act
        pipeStateService.enableLaunch(pipeLaunchId);

        // assert
        pipeTester.triggerJob(pipeLaunchId, SimplePipe.JOB_ID);
        Assert.assertEquals(
            StatusChangeType.QUEUED,
            pipeTester.getJobLastLaunch(pipeLaunchId, SimplePipe.JOB_ID).getLastStatusChange().getType()
        );
    }

    @Test
    public void pipelineDisabledFromSubscriber() throws Exception {
        // arrange
        PipelineBuilder builder = twoJobPipeline();
        builder.withSubscriber(DisablingSubscriber.class);
        Pipeline pipeline = builder.build();

        // act
        String pipeLaunchId = pipeTester.runPipeToCompletion(pipeline, Collections.emptyList());

        // assert
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(pipeLaunchId);
        Assert.assertEquals(
            StatusChangeType.SUBSCRIBERS_FAILED, pipeLaunch.getJobState(SECOND_JOB_ID).getLastStatusChangeType()
        );
    }

    @Test
    public void disablePipelineGracefully_ShouldBeginDisabling_IfJobIsQueued() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(DummyJob.class).withId(FIRST_JOB_ID);
        Pipeline pipeline = builder.build();

        String launchId = pipeTester.activateLaunch(pipeline, Collections.emptyList());
        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(launchId);
        Assert.assertEquals(
            StatusChangeType.QUEUED,
            pipeLaunch.getJobState(FIRST_JOB_ID).getLastStatusChangeType()
        );

        pipeStateService.disableLaunchGracefully(launchId, false);
        pipeLaunch = pipeTester.getPipeLaunch(launchId);

        assertDisabling(pipeLaunch);
    }

    @Test
    public void disablePipelineGracefully_ShouldDisableImmediately_IfNoJobsRunning() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(DummyJob.class).withId(FIRST_JOB_ID).withManualTrigger();
        Pipeline pipeline = builder.build();

        String launchId = pipeTester.activateLaunch(pipeline, Collections.emptyList());
        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(launchId);
        Assert.assertNull(pipeLaunch.getJobState(FIRST_JOB_ID).getLastLaunch());

        pipeStateService.disableLaunchGracefully(launchId, false);
        pipeLaunch = pipeTester.getPipeLaunch(launchId);

        assertDisabled(pipeLaunch);
    }

    @Test
    public void disablePipelineGracefully_ShouldNotInterruptNonInterruptableStage() throws Exception {
        StageGroup stageGroup = new StageGroup(StageBuilder.create("first_stage").uninterruptable());

        PipelineBuilder builder = twoJobPipeline();
        builder.getJobBuilder(FIRST_JOB_ID).beginStage(stageGroup.getStages().get(0));
        Pipeline pipeline = builder.build();

        String launchId = pipeTester.activateLaunch(pipeline, STAGE_GROUP_ID, Collections.emptyList());
        pipeStateService.disableLaunchGracefully(launchId, false);
        pipeTester.runScheduledJobToCompletion(FIRST_JOB_ID);

        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(launchId);
        assertDisabling(pipeLaunch);

        pipeTester.runScheduledJobToCompletion(SECOND_JOB_ID);
        pipeLaunch = pipeTester.getPipeLaunch(launchId);
        assertDisabled(pipeLaunch);
    }

    @Test
    public void disablePipelineGracefully_ShouldNotInterruptNonInterruptableStage_IfItStoppedOnManualTrigger() throws Exception {
        StageGroup stageGroup = new StageGroup(StageBuilder.create("first_stage").uninterruptable());

        PipelineBuilder builder = twoJobPipeline();
        builder.getJobBuilder(FIRST_JOB_ID).beginStage(stageGroup.getStages().get(0));
        ((JobBuilder) builder.getJobBuilder(SECOND_JOB_ID)).withManualTrigger();
        Pipeline pipeline = builder.build();

        String launchId = pipeTester.activateLaunch(pipeline, STAGE_GROUP_ID, Collections.emptyList());
        pipeStateService.disableLaunchGracefully(launchId, false);
        pipeTester.runScheduledJobsToCompletion();

        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(launchId);
        assertDisabling(pipeLaunch);

        pipeTester.triggerJob(launchId, SECOND_JOB_ID);
        pipeTester.runScheduledJobsToCompletion();
        pipeLaunch = pipeTester.getPipeLaunch(launchId);
        assertDisabled(pipeLaunch);
    }

    @Test
    public void disablePipelineGracefully_ShouldInterruptNonInterruptableStage_IfManualInterruption() throws Exception {
        StageGroup stageGroup = new StageGroup(StageBuilder.create("first_stage").uninterruptable());

        PipelineBuilder builder = twoJobPipeline();
        builder.getJobBuilder(FIRST_JOB_ID).beginStage(stageGroup.getStages().get(0));
        Pipeline pipeline = builder.build();

        String launchId = pipeTester.activateLaunch(pipeline, STAGE_GROUP_ID, Collections.emptyList());
        pipeStateService.disableLaunchGracefully(launchId, true);
        pipeTester.runScheduledJobToCompletion(FIRST_JOB_ID);

        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(launchId);
        assertDisabled(pipeLaunch);
    }

    @Test
    public void cancelGracefulDisabling() {
        PipelineBuilder builder = twoJobPipeline();
        Pipeline pipeline = builder.build();

        String launchId = pipeTester.activateLaunch(pipeline, Collections.emptyList());
        pipeStateService.disableLaunchGracefully(launchId, false);
        pipeStateService.cancelGracefulDisabling(launchId);

        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(launchId);
        Assert.assertFalse(pipeLaunch.isDisablingGracefully());
        Assert.assertFalse(pipeLaunch.isDisabled());
    }

    @Test(expected = IllegalStateException.class)
    public void cancelGracefulDisabling_ShouldThrowException_IfAlreadyDisabled() throws InterruptedException {
        PipelineBuilder builder = twoJobPipeline();
        Pipeline pipeline = builder.build();

        String launchId = pipeTester.activateLaunch(pipeline, Collections.emptyList());
        pipeTester.runScheduledJobsToCompletion();
        pipeStateService.disableLaunchGracefully(launchId, false);

        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(launchId);
        Assert.assertTrue(pipeLaunch.isDisabled());

        pipeStateService.cancelGracefulDisabling(launchId);
    }

    @Test
    public void disableJobsInLaunchGracefullyForParallelJobs() throws Exception {
        PipelineBuilder builder = parallelPipeline();
        Pipeline pipeline = builder.build();

        String launchId = pipeTester.activateLaunch(pipeline, Collections.emptyList());
        pipeTester.runScheduledJobToCompletion(START_JOB_ID);
        pipeTester.runScheduledJobToCompletion(FIRST_JOB_ID);
        pipeTester.runScheduledJobToCompletion(SECOND_JOB_ID);

        pipeStateService.disableJobsInLaunchGracefully(launchId, job -> true, true, false);
        pipeTester.runScheduledJobToCompletion(FIRST_JOB_ID);
        pipeTester.runScheduledJobToCompletion(SECOND_JOB_ID);

        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(launchId);
        Assert.assertEquals(StatusChangeType.KILLED, pipeLaunch.getJobs().get(FIRST_JOB_ID).getLastStatusChangeType());
        Assert.assertEquals(StatusChangeType.KILLED, pipeLaunch.getJobs().get(SECOND_JOB_ID).getLastStatusChangeType());
    }

    @Test
    public void enableAfterGracefulDisabling_ShouldTriggerNextJob_IfItHasNotManualTrigger()
        throws InterruptedException {

        Pipeline pipeline = twoJobPipeline().build();
        String launchId = pipeTester.activateLaunch(pipeline, Collections.emptyList());
        pipeStateService.disableLaunchGracefully(launchId, false);
        pipeTester.runScheduledJobsToCompletion();

        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(launchId);
        Assert.assertTrue(pipeLaunch.isDisabled());
        pipeStateService.enableLaunch(launchId);

        pipeLaunch = pipeTester.getPipeLaunch(launchId);
        Assert.assertFalse(pipeLaunch.isDisablingGracefully());
        Assert.assertFalse(pipeLaunch.isDisabled());
        Assert.assertEquals(
            StatusChangeType.QUEUED, pipeLaunch.getJobState(SECOND_JOB_ID).getLastStatusChangeType()
        );
    }

    private PipelineBuilder twoJobPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder firstJob = builder.withJob(DummyJob.class).withId(FIRST_JOB_ID);
        builder.withJob(DummyJob.class).withUpstreams(firstJob).withId(SECOND_JOB_ID);
        return builder;
    }


    private PipelineBuilder parallelPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder singleJob = builder.withJob(DummyJob.class, START_JOB_ID);

        builder.withJob(DummyJob.class, FIRST_JOB_ID)
            .withUpstreams(singleJob)
            .withScheduler()
            .build();
        builder.withJob(DummyJob.class, SECOND_JOB_ID)
            .withUpstreams(singleJob)
            .withScheduler()
            .build();

        return builder;
    }

    private void assertDisabled(PipeLaunch pipeLaunch) {
        Assert.assertFalse(pipeLaunch.isDisablingGracefully());
        Assert.assertTrue(pipeLaunch.isDisabled());
    }

    private void assertDisabling(PipeLaunch pipeLaunch) {
        Assert.assertTrue(pipeLaunch.isDisablingGracefully());
        Assert.assertFalse(pipeLaunch.isDisabled());
    }

    public static class DisablingSubscriber implements PipeSubscriber {
        @Autowired
        private PipeStateService pipeStateService;

        @Override
        public void jobExecutorHasFinished(FullJobLaunchId fullJobLaunchId, PipeLaunch pipeLaunch) {
            if (fullJobLaunchId.getJobId().equals(FIRST_JOB_ID) &&
                pipeLaunch.getJobState(FIRST_JOB_ID).isExecutorSuccessful()) {
                pipeStateService.disableLaunch(fullJobLaunchId.getPipeLaunchId(), false);
            }
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("1d03a6e6-133e-40fd-973e-e07ba48fd214");
        }
    }
}
