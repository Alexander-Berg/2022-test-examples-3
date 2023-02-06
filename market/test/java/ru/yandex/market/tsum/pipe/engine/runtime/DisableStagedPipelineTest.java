package ru.yandex.market.tsum.pipe.engine.runtime;

import org.hamcrest.collection.IsEmptyCollection;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.stage.StageGroup;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeStateService;
import ru.yandex.market.tsum.pipe.engine.runtime.state.StageGroupDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StageGroupState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 24.01.18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DisableStagedPipelineTest {
    private static final String FIRST_JOB_ID = "FIRST_JOB";
    private static final String SECOND_JOB_ID = "SECOND_JOB";
    private static final String THIRD_JOB_ID = "THIRD_JOB";
    private static final String STAGE_GROUP_ID = "STAGE_GROUP_ID";

    @Autowired
    private PipeTester pipeTester;

    @Autowired
    private StageGroupDao stageService;

    @Autowired
    private PipeStateService pipeStateService;

    private StageGroup stageGroup;
    @Before
    public void setUp() {
        stageGroup = new StageGroup("first_stage", "second_stage");
    }

    @Test
    public void shouldDisableFinishedPipelineAndRemoveItFromQueue() throws Exception {
        // arrange
        String pipeLaunchId = pipeTester.runPipeToCompletion(pipeline(), STAGE_GROUP_ID);

        // act
        pipeTester.triggerJob(pipeLaunchId, SECOND_JOB_ID);
        pipeTester.runScheduledJobsToCompletion();

        // assert
        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(pipeLaunchId);
        Assert.assertTrue(pipeLaunch.isDisabled());

        StageGroupState stageGroupState = stageService.get(STAGE_GROUP_ID);
        Assert.assertThat(stageGroupState.getQueue(), IsEmptyCollection.empty());
    }

    @Test
    public void shouldDisableFinishedPipeline_WithSeveralLastJobs() throws Exception {
        // arrange
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder firstJob = builder.withJob(DummyJob.class)
            .beginStage(stageGroup.getStages().get(0))
            .withId(FIRST_JOB_ID);

        builder.withJob(DummyJob.class)
            .withUpstreams(firstJob)
            .withId(SECOND_JOB_ID);

        builder.withJob(DummyJob.class)
            .withUpstreams(firstJob)
            .withId(THIRD_JOB_ID);

        // act
        String pipeLaunchId = pipeTester.runPipeToCompletion(builder.build(), STAGE_GROUP_ID);
        pipeTester.runScheduledJobsToCompletion();

        // assert
        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(pipeLaunchId);
        Assert.assertTrue(pipeLaunch.isDisabled());

        StageGroupState stageGroupState = stageService.get(STAGE_GROUP_ID);
        Assert.assertThat(stageGroupState.getQueue(), IsEmptyCollection.empty());
    }

    @Test
    public void shouldDisablePipelineIfItHasWaitingForStageJobs() throws Exception {
        // arrange
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder firstJob = builder.withJob(DummyJob.class)
            .beginStage(stageGroup.getStages().get(0))
            .withId(FIRST_JOB_ID);

        JobBuilder secondJob = builder.withJob(DummyJob.class)
            .withUpstreams(firstJob)
            .beginStage(stageGroup.getStages().get(1))
            .withId(SECOND_JOB_ID);

        builder.withJob(DummyJob.class)
            .withManualTrigger()
            .withUpstreams(secondJob);

        Pipeline twoStagePipeline = builder.build();

        // act
        String firstLaunchId = pipeTester.runPipeToCompletion(twoStagePipeline, STAGE_GROUP_ID);
        String secondLaunchId = pipeTester.runPipeToCompletion(twoStagePipeline, STAGE_GROUP_ID);
        pipeStateService.disableLaunch(secondLaunchId, true);

        // assert
        PipeLaunch secondPipeLaunch = pipeTester.getPipeLaunch(secondLaunchId);
        Assert.assertTrue(secondPipeLaunch.isDisabled());
        Assert.assertEquals(
            StatusChangeType.WAITING_FOR_STAGE,
            secondPipeLaunch.getJobState(SECOND_JOB_ID).getLastStatusChangeType()
        );

        StageGroupState stageGroupState = stageService.get(STAGE_GROUP_ID);
        Assert.assertEquals(1, stageGroupState.getQueue().size());
    }

    @Test
    public void shouldRemoveFromQueueWhenDisabledManually() throws Exception {
        // arrange
        String pipeLaunchId = pipeTester.runPipeToCompletion(pipeline(), STAGE_GROUP_ID);

        // act
        pipeStateService.disableLaunch(pipeLaunchId, true);

        // assert
        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(pipeLaunchId);
        Assert.assertTrue(pipeLaunch.isDisabled());

        StageGroupState stageGroupState = stageService.get(STAGE_GROUP_ID);
        Assert.assertThat(stageGroupState.getQueue(), IsEmptyCollection.empty());
    }

    private Pipeline pipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder firstJob = builder.withJob(DummyJob.class)
            .beginStage(stageGroup.getStages().get(0))
            .withId(FIRST_JOB_ID);

        builder.withJob(DummyJob.class)
            .withManualTrigger()
            .withUpstreams(firstJob)
            .withId(SECOND_JOB_ID);

        return builder.build();
    }
}
