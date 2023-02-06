package ru.yandex.market.tsum.pipe.engine.runtime;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.common.CanRunWhen;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.stage.StageGroup;
import ru.yandex.market.tsum.pipe.engine.definition.stage.StageRef;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.StageGroupDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StageGroupState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.FailingJob;

import javax.inject.Named;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 27.11.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class, StagedPipelinesTest.Config.class,
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class StagedPipelinesTest {
    private static final String STAGE_GROUP_ID = "test-stages";
    private static final String FIRST_JOB_ID = "FIRST_JOB";
    private static final String SECOND_JOB_ID = "SECOND_JOB";
    private static final String THIRD_JOB_ID = "THIRD_JOB";

    private static final String FIRST_STAGE = "first";
    private static final String SECOND_STAGE = "second";

    @Autowired
    PipeTester pipeTester;

    @Autowired
    PipeLaunchDao pipeLaunchDao;

    @Autowired
    StageGroupDao stageService;

    @Autowired
    @Named(STAGE_GROUP_ID)
    StageGroup stageGroup;
    private static final String MIDDLE_JOB_ID = "MIDDLE_JOB";

    @Test
    public void failedJobShouldNotReleaseStage() throws Exception {
        // arrange
        PipelineBuilder builder = PipelineBuilder.create();

        builder.withJob(FailingJob.class)
            .withId(FIRST_JOB_ID)
            .beginStage(stageGroup.getStage(FIRST_STAGE));

        Pipeline pipeline = builder.build();

        // act
        String launchId = pipeTester.runPipeToCompletion(pipeline, STAGE_GROUP_ID);

        // assert
        StageGroupState stageGroupState = stageService.get(STAGE_GROUP_ID);
        Assert.assertEquals(
            Sets.newHashSet(FIRST_STAGE),
            getAcquiredStageIds(launchId, stageGroupState)
        );
    }

    @Test
    public void stageShouldNotBeReleasedUntilNextJobIsTriggered() throws Exception {
        Pipeline pipeline = simpleTwoStagePipeline();
        String launchId = pipeTester.runPipeToCompletion(pipeline, STAGE_GROUP_ID);

        StageGroupState stageGroupState = stageService.get(STAGE_GROUP_ID);
        Assert.assertEquals(
            Sets.newHashSet(FIRST_STAGE),
            getAcquiredStageIds(launchId, stageGroupState)
        );

        Assert.assertTrue(pipeLaunchDao.getById(launchId).getJobState(SECOND_JOB_ID).isReadyToRun());
    }

    @Test
    public void secondPipeWaitsForLockedStage() throws Exception {
        Pipeline pipeline = simpleTwoStagePipeline();
        String firstLaunchId = pipeTester.runPipeToCompletion(pipeline, STAGE_GROUP_ID);
        String secondLaunchId = pipeTester.runPipeToCompletion(pipeline, STAGE_GROUP_ID);

        PipeLaunch firstLaunch = pipeLaunchDao.getById(firstLaunchId);
        Assert.assertEquals(StatusChangeType.SUCCESSFUL, firstLaunch.getJobState(FIRST_JOB_ID).getLastStatusChangeType());
        Assert.assertNull(firstLaunch.getJobState(SECOND_JOB_ID).getLastLaunch());
        Assert.assertTrue(firstLaunch.getJobState(SECOND_JOB_ID).isReadyToRun());

        PipeLaunch secondLaunch = pipeLaunchDao.getById(secondLaunchId);
        Assert.assertEquals(StatusChangeType.WAITING_FOR_STAGE, secondLaunch.getJobState(FIRST_JOB_ID).getLastStatusChangeType());
        Assert.assertFalse(secondLaunch.getJobState(FIRST_JOB_ID).isReadyToRun());
    }

    @Test
    public void manyPipelinesWaitingForFirstStage() throws Exception {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder firstJob = builder.withJob(DummyJob.class)
            .withId(FIRST_JOB_ID)
            .beginStage(stageGroup.getStage(FIRST_STAGE));

        builder.withJob(DummyJob.class)
            .withId(SECOND_JOB_ID)
            .withUpstreams(firstJob)
            .withManualTrigger();

        Pipeline pipeline = builder.build();

        String firstLaunchId = pipeTester.runPipeToCompletion(pipeline, STAGE_GROUP_ID);
        String secondLaunchId = pipeTester.runPipeToCompletion(pipeline, STAGE_GROUP_ID);
        String thirdLaunchId = pipeTester.runPipeToCompletion(pipeline, STAGE_GROUP_ID);
        String fourthLaunchId = pipeTester.runPipeToCompletion(pipeline, STAGE_GROUP_ID);

        StageGroupState stageGroupState = stageService.get(STAGE_GROUP_ID);
        Assert.assertEquals(
            Sets.newHashSet(FIRST_STAGE),
            getAcquiredStageIds(firstLaunchId, stageGroupState)
        );

        Assert.assertEquals(
            Collections.emptySet(),
            getAcquiredStageIds(secondLaunchId, stageGroupState)
        );

        Assert.assertEquals(
            Collections.emptySet(),
            getAcquiredStageIds(thirdLaunchId, stageGroupState)
        );

        Assert.assertEquals(
            Collections.emptySet(),
            getAcquiredStageIds(fourthLaunchId, stageGroupState)
        );
    }

    private Set<String> getAcquiredStageIds(String firstLaunchId, StageGroupState stageGroupState) {
        return stageGroupState.getAcquiredStages(firstLaunchId).stream().map(StageRef::getId).collect(Collectors.toSet());
    }

    @Test
    public void twoPipesFullCycle() throws Exception {
        Pipeline pipeline = simpleTwoStagePipeline();
        String firstLaunchId = pipeTester.runPipeToCompletion(pipeline);
        String secondLaunchId = pipeTester.runPipeToCompletion(pipeline);

        // act
        pipeTester.triggerJob(firstLaunchId, SECOND_JOB_ID);
        pipeTester.runScheduledJobsToCompletion();

        // assert first pipe finished, second waits SECOND_JOB_ID trigger
        PipeLaunch firstLaunch = pipeLaunchDao.getById(firstLaunchId);
        PipeLaunch secondLaunch = pipeLaunchDao.getById(secondLaunchId);
        Assert.assertEquals(StatusChangeType.SUCCESSFUL, firstLaunch.getJobState(SECOND_JOB_ID).getLastStatusChangeType());
        Assert.assertNull(secondLaunch.getJobState(SECOND_JOB_ID).getLastLaunch());

        // act
        pipeTester.triggerJob(secondLaunchId, SECOND_JOB_ID);
        pipeTester.runScheduledJobsToCompletion();

        // assert both pipelines finished
        secondLaunch = pipeLaunchDao.getById(secondLaunchId);
        Assert.assertEquals(StatusChangeType.SUCCESSFUL, secondLaunch.getJobState(SECOND_JOB_ID).getLastStatusChangeType());
    }

    @Test
    public void cannotTriggerMiddleJobInPastStage() throws Exception {
        // arrange
        Pipeline pipeline = longTwoStagePipeline();

        // act
        String pipeLaunchId = pipeTester.runPipeToCompletion(pipeline);

        // assert
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(pipeLaunchId);
        Assert.assertFalse(pipeLaunch.getJobState(MIDDLE_JOB_ID).isReadyToRun());
    }

    @Test
    // это тест на случай, когда механизм rollback'а ещё не готов
    public void cannotTriggerAnyJobInPasteStage() throws Exception {
        // arrange
        Pipeline pipeline = longTwoStagePipeline();

        // act
        String pipeLaunchId = pipeTester.runPipeToCompletion(pipeline);

        // assert
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(pipeLaunchId);
        Assert.assertFalse(pipeLaunch.getJobState(FIRST_JOB_ID).isReadyToRun());
    }

    @Test
    public void unlocksOldStagesWhenAcquiringNewStage() {
        Pipeline firstPipeline = secondStageWithAnyLinkPipeline();
        Pipeline secondPipeline = secondStageWithAnyLinkPipeline();

        String firstPipeLaunchId = pipeTester.runPipeToCompletion(firstPipeline, STAGE_GROUP_ID);
        String secondPipeLaunchId = pipeTester.runPipeToCompletion(secondPipeline, STAGE_GROUP_ID);
        PipeLaunch firstPipeLaunch = pipeLaunchDao.getById(firstPipeLaunchId);
        StageGroupState stageGroupState = stageService.get(STAGE_GROUP_ID);

        Assert.assertEquals(StatusChangeType.FAILED, firstPipeLaunch.getJobState(THIRD_JOB_ID).getLastStatusChangeType());

        Assert.assertEquals(
            Collections.singleton(SECOND_STAGE),
            stageGroupState.getAcquiredStages(firstPipeLaunchId).stream().map(StageRef::getId).collect(Collectors.toSet())
        );
        Assert.assertEquals(
            Collections.singleton(FIRST_STAGE),
            stageGroupState.getAcquiredStages(secondPipeLaunchId).stream().map(StageRef::getId).collect(Collectors.toSet())
        );

        pipeTester.triggerJob(firstPipeLaunchId, THIRD_JOB_ID);
        pipeTester.runScheduledJobsToCompletion();
        stageGroupState = stageService.get(STAGE_GROUP_ID);

        Assert.assertEquals(
            Collections.singleton(SECOND_STAGE),
            stageGroupState.getAcquiredStages(secondPipeLaunchId).stream().map(StageRef::getId).collect(Collectors.toSet())
        );
    }

    @Test
    public void DoesNotUnlockOldStagesWhenAcquiringNewStageAndRunningJobsOnOldStages() {
        Pipeline firstPipeline = oneFirstStageJobTwoSecondWithManualEnd();
        Pipeline secondPipeline = oneFirstStageJobTwoSecondWithManualEnd();

        String firstPipeLaunchId = pipeTester.runPipeToCompletion(firstPipeline, STAGE_GROUP_ID);
        String secondPipeLaunchId = pipeTester.runPipeToCompletion(secondPipeline, STAGE_GROUP_ID);

        pipeTester.triggerJob(secondPipeLaunchId, FIRST_JOB_ID);
        pipeTester.triggerJob(firstPipeLaunchId, THIRD_JOB_ID);
        pipeTester.runScheduledJobToCompletion(THIRD_JOB_ID);

        StageGroupState stageGroupState = stageService.get(STAGE_GROUP_ID);

        Assert.assertEquals(
            Collections.emptySet(),
            stageGroupState.getAcquiredStages(firstPipeLaunchId).stream().map(StageRef::getId).collect(Collectors.toSet())
        );

        Assert.assertEquals(
            new HashSet<>(Arrays.asList(FIRST_STAGE, SECOND_STAGE)),
            stageGroupState.getAcquiredStages(secondPipeLaunchId).stream().map(StageRef::getId).collect(Collectors.toSet())
        );
    }

    public Pipeline secondStageWithAnyLinkPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder startingStageJob = builder.withJob(DummyJob.class)
            .beginStage(stageGroup.getStage(FIRST_STAGE));

        JobBuilder firstParallelJob = builder.withJob(DummyJob.class)
            .withId(FIRST_JOB_ID)
            .withUpstreams(startingStageJob)
            .beginStage(stageGroup.getStage(FIRST_STAGE));

        JobBuilder secondParallelJob = builder.withJob(DummyJob.class)
            .withId(SECOND_JOB_ID)
            .withUpstreams(startingStageJob);

        JobBuilder thirdJob = builder.withJob(OnceFailingJob.class)
            .withId(THIRD_JOB_ID)
            .withUpstreams(CanRunWhen.ANY_COMPLETED, firstParallelJob, secondParallelJob)
            .beginStage(stageGroup.getStage(SECOND_STAGE));

        return builder.build();
    }

    // TODO: тест отката пайплайна
    // TODO: кейс, когда один пайп дизаблится, а другой должен захватить его секцию

    private Pipeline simpleTwoStagePipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder firstJob = builder.withJob(DummyJob.class)
            .withId(FIRST_JOB_ID)
            .beginStage(stageGroup.getStage(FIRST_STAGE));

        JobBuilder secondJob = builder.withJob(DummyJob.class)
            .withId(SECOND_JOB_ID)
            .withUpstreams(firstJob)
            .withManualTrigger()
            .beginStage(stageGroup.getStage(SECOND_STAGE));

        return builder.build();
    }

    private Pipeline longTwoStagePipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder firstJob = builder.withJob(DummyJob.class)
            .withId(FIRST_JOB_ID)
            .beginStage(stageGroup.getStage(FIRST_STAGE));

        JobBuilder middleJob = builder.withJob(DummyJob.class)
            .withId(MIDDLE_JOB_ID)
            .withUpstreams(firstJob);

        builder.withJob(DummyJob.class)
            .withId("LAST_JOB")
            .beginStage(stageGroup.getStage(SECOND_STAGE))
            .withUpstreams(middleJob);

        return builder.build();
    }

    private Pipeline oneFirstStageJobTwoSecondWithManualEnd() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder firstJob = builder.withJob(DummyJob.class)
            .withId(FIRST_JOB_ID)
            .beginStage(stageGroup.getStage(FIRST_STAGE));

        JobBuilder middleJob = builder.withJob(DummyJob.class)
            .beginStage(stageGroup.getStage(SECOND_STAGE))
            .withId(SECOND_JOB_ID)
            .withUpstreams(firstJob);

        builder.withJob(DummyJob.class)
            .withId(THIRD_JOB_ID)
            .withManualTrigger()
            .withUpstreams(middleJob);

        return builder.build();
    }

    @Configuration
    public static class Config {
        @Bean(name = StagedPipelinesTest.STAGE_GROUP_ID)
        public StageGroup stages() {
            return new StageGroup(FIRST_STAGE, SECOND_STAGE);
        }
    }

    public static class OnceFailingJob implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("fbfff8b0-cf9a-4a5c-bae7-ecc36189c180");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            if (context.getJobState().getLastLaunch().getNumber() == 1) {
                throw new RuntimeException("I am once failing job!");
            }
        }
    }
}
