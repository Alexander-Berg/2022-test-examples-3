package ru.yandex.market.tsum.pipe.engine.runtime;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.subscriber.PipeSubscriber;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.stage.StageBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.stage.StageGroup;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.events.ExecutorFailedToInterruptEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobRunningEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.events.TriggerEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.exceptions.JobDisabledException;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.DummyFullJobIdFactory;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeStateService;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.simple.SimplePipe;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 26.07.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DisableJobsTest {
    private static final String FIRST_JOB_ID = "FIRST_JOB";
    private static final String SECOND_JOB_ID = "SECOND_JOB";
    private static final String STAGE_ID = "STAGE_ID";
    public static final String STAGE_GROUP_ID = "stageGroupId";

    @Autowired
    private PipeTester pipeTester;

    @Autowired
    private PipeStateService pipeStateService;

    @Autowired
    private PipeLaunchDao pipeLaunchDao;

    @Test(expected = JobDisabledException.class)
    public void manualTriggersDontWorkOnDisabledJobs() throws Exception {
        // arrange
        String pipeLaunchId = pipeTester.runPipeToCompletion(SimplePipe.PIPE_ID, Collections.emptyList());
        pipeStateService.disableJobsInLaunch(pipeLaunchId, true, jobState -> true);

        // act
        pipeStateService.recalc(pipeLaunchId, new TriggerEvent(SimplePipe.JOB_ID, "user42", false));
    }

    @Test
    public void disableJobsByPredicate() throws Exception {
        // arrange
        PipelineBuilder builder = pipelineBuilder();
        Pipeline pipeline = builder.build();

        // act
        String pipeLaunchId = pipeTester.runPipeToCompletion(pipeline, Collections.emptyList());
        pipeStateService.disableJobsInLaunch(
            pipeLaunchId,
            true,
            jobState -> FIRST_JOB_ID.equals(jobState.getJobId())
        );

        // assert
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(pipeLaunchId);
        assertTrue(pipeLaunch.getJobState(FIRST_JOB_ID).isDisabled());
        assertFalse(pipeLaunch.getJobState(SECOND_JOB_ID).isDisabled());
    }

    @Test
    public void disableJobsGracefully() throws Exception {
        // arrange
        PipelineBuilder builder = pipelineBuilder();
        Pipeline pipeline = builder.build();

        String launchId = pipeTester.activateLaunch(pipeline, Collections.emptyList());
        PipeLaunch launch = pipeStateService.recalc(
            launchId,
            new JobRunningEvent(FIRST_JOB_ID, 1, DummyFullJobIdFactory.create())
        );

        launch.getJobState(FIRST_JOB_ID).getLastLaunch().setInterruptAllowed(true);
        pipeLaunchDao.save(launch);

        // act
        pipeStateService.disableJobsInLaunchGracefully(launchId, jobState -> true, false, false);
        pipeStateService.recalc(launchId, new ExecutorFailedToInterruptEvent(FIRST_JOB_ID, 1));

        // assert
        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(launchId);
        Assert.assertTrue(pipeLaunch.getJobState(FIRST_JOB_ID).isDisabling());
        Assert.assertFalse(pipeLaunch.getJobState(FIRST_JOB_ID).isDisabled());
        Assert.assertTrue(pipeLaunch.getJobState(SECOND_JOB_ID).isDisabled());

        // act
        pipeTester.runScheduledJobsToCompletion();

        // assert
        pipeLaunch = pipeTester.getPipeLaunch(launchId);
        Assert.assertTrue(pipeLaunch.getJobState(FIRST_JOB_ID).isDisabled());
        Assert.assertEquals(StatusChangeType.SUCCESSFUL, pipeLaunch.getJobState(FIRST_JOB_ID).getLastStatusChangeType());
        Assert.assertNull(pipeLaunch.getJobState(SECOND_JOB_ID).getLastLaunch());
    }

    @Test
    public void disableJobsInStagedPipelineGracefully() throws Exception {
        // arrange
        StageGroup stageGroup = new StageGroup(STAGE_ID);

        PipelineBuilder builder = pipelineBuilder();
        builder.getJobBuilder(FIRST_JOB_ID).beginStage(stageGroup.getStage(STAGE_ID));
        Pipeline pipeline = builder.build();

        String launchId = pipeTester.activateLaunch(pipeline, STAGE_ID, Collections.emptyList());
        pipeStateService.recalc(
            launchId,
            new JobRunningEvent(FIRST_JOB_ID, 1, DummyFullJobIdFactory.create())
        );

        // act
        pipeStateService.disableJobsInLaunchGracefully(launchId, jobState -> true, false, false);
        pipeStateService.recalc(launchId, new ExecutorFailedToInterruptEvent(FIRST_JOB_ID, 1));

        // assert
        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(launchId);
        Assert.assertTrue(pipeLaunch.getJobState(FIRST_JOB_ID).isDisabling());
        Assert.assertFalse(pipeLaunch.getJobState(FIRST_JOB_ID).isDisabled());
        Assert.assertTrue(pipeLaunch.getJobState(SECOND_JOB_ID).isDisabled());

        // act
        pipeTester.runScheduledJobsToCompletion();

        // assert
        pipeLaunch = pipeTester.getPipeLaunch(launchId);
        Assert.assertTrue(pipeLaunch.getJobState(FIRST_JOB_ID).isDisabled());
        Assert.assertEquals(StatusChangeType.SUCCESSFUL, pipeLaunch.getJobState(FIRST_JOB_ID).getLastStatusChangeType());
        Assert.assertNull(pipeLaunch.getJobState(SECOND_JOB_ID).getLastLaunch());
    }

    @Test
    public void disableJobsOnUninterruptibleStage() throws Exception {
        // arrange
        StageGroup stageGroup = new StageGroup(StageBuilder.create(STAGE_ID).uninterruptable());

        PipelineBuilder builder = pipelineBuilder();
        builder.getJobBuilder(FIRST_JOB_ID).beginStage(stageGroup.getStage(STAGE_ID));
        Pipeline pipeline = builder.build();

        String launchId = pipeTester.activateLaunch(pipeline, STAGE_ID, Collections.emptyList());
        pipeStateService.recalc(
            launchId,
            new JobRunningEvent(FIRST_JOB_ID, 1, DummyFullJobIdFactory.create())
        );

        // act
        pipeStateService.disableJobsInLaunchGracefully(launchId, jobState -> true, false, false);
        pipeTester.runScheduledJobsToCompletion();

        // assert
        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(launchId);
        Assert.assertTrue(pipeLaunch.getJobState(FIRST_JOB_ID).isDisabled());
        Assert.assertTrue(pipeLaunch.getJobState(SECOND_JOB_ID).isDisabled());
        Assert.assertEquals(StatusChangeType.SUCCESSFUL, pipeLaunch.getJobState(FIRST_JOB_ID).getLastStatusChangeType());
        Assert.assertEquals(StatusChangeType.SUCCESSFUL, pipeLaunch.getJobState(SECOND_JOB_ID).getLastStatusChangeType());
    }

    @Test
    public void disableWaitingForStageJob() {
        // arrange
        StageGroup stageGroup = new StageGroup(STAGE_ID);

        PipelineBuilder builder = pipelineBuilder();
        builder.getJobBuilder(FIRST_JOB_ID).beginStage(stageGroup.getStage(STAGE_ID));
        Pipeline pipeline = builder.build();

        String firstLaunchId = pipeTester.activateLaunch(pipeline, STAGE_GROUP_ID, Collections.emptyList());
        pipeStateService.recalc(
            firstLaunchId,
            new JobRunningEvent(FIRST_JOB_ID, 1, DummyFullJobIdFactory.create())
        );

        String secondLaunchId = pipeTester.activateLaunch(pipeline, STAGE_GROUP_ID, Collections.emptyList());
        Assert.assertEquals(
            StatusChangeType.WAITING_FOR_STAGE,
            pipeTester.getPipeLaunch(secondLaunchId).getJobState(FIRST_JOB_ID).getLastStatusChangeType()
        );

        // act
        pipeStateService.disableJobsInLaunchGracefully(secondLaunchId, jobState -> true, false, false);

        // assert
        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(secondLaunchId);
        Assert.assertTrue(pipeLaunch.getJobState(FIRST_JOB_ID).isDisabled());
    }

    @Test
    public void disableJobsIgnoringUninterruptibleStage() throws Exception {
        // arrange
        StageGroup stageGroup = new StageGroup(StageBuilder.create(STAGE_ID).uninterruptable());

        PipelineBuilder builder = pipelineBuilder();
        builder.getJobBuilder(FIRST_JOB_ID).beginStage(stageGroup.getStage(STAGE_ID));
        Pipeline pipeline = builder.build();

        String launchId = pipeTester.activateLaunch(pipeline, STAGE_GROUP_ID, Collections.emptyList());
        PipeLaunch launch = pipeStateService.recalc(
            launchId,
            new JobRunningEvent(FIRST_JOB_ID, 1, DummyFullJobIdFactory.create())
        );

        launch.getJobState(FIRST_JOB_ID).getLastLaunch().setInterruptAllowed(true);
        pipeLaunchDao.save(launch);

        // act
        pipeStateService.disableJobsInLaunchGracefully(launchId, jobState -> true, true, false);
        pipeStateService.recalc(launchId, new ExecutorFailedToInterruptEvent(FIRST_JOB_ID, 1));
        pipeTester.runScheduledJobsToCompletion();

        // assert
        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(launchId);
        Assert.assertTrue(pipeLaunch.getJobState(FIRST_JOB_ID).isDisabled());
        Assert.assertEquals(StatusChangeType.SUCCESSFUL, pipeLaunch.getJobState(FIRST_JOB_ID).getLastStatusChangeType());
        Assert.assertNull(pipeLaunch.getJobState(SECOND_JOB_ID).getLastLaunch());
    }

    @Test
    public void jobsDisabledFromSubscriber() throws Exception {
        // arrange
        PipelineBuilder builder = pipelineBuilder();
        builder.withSubscriber(DisablingSubscriber.class);
        Pipeline pipeline = builder.build();

        // act
        String pipeLaunchId = pipeTester.runPipeToCompletion(pipeline, Collections.emptyList());

        // assert
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(pipeLaunchId);
        Assert.assertNull(pipeLaunch.getJobState(SECOND_JOB_ID).getLastLaunch());
    }

    private PipelineBuilder pipelineBuilder() {
        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder firstJob = builder.withJob(DummyJob.class).withId(FIRST_JOB_ID);
        JobBuilder secondJob = builder.withJob(DummyJob.class).withId(SECOND_JOB_ID).withUpstreams(firstJob);
        return builder;
    }

    public static class DisablingSubscriber implements PipeSubscriber {
        @Autowired
        private PipeStateService pipeStateService;

        @Override
        public void jobExecutorHasFinished(FullJobLaunchId fullJobLaunchId, PipeLaunch pipeLaunch) {
            if (fullJobLaunchId.getJobId().equals(FIRST_JOB_ID) &&
                pipeLaunch.getJobState(FIRST_JOB_ID).isExecutorSuccessful()) {
                pipeStateService.disableJobsInLaunch(
                    fullJobLaunchId.getPipeLaunchId(),
                    false,
                    jobState -> true
                );
            }
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("cc31b97a-d620-4b44-9e81-c01011a7939b");
        }
    }
}
