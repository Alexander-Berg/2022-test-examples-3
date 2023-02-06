package ru.yandex.market.tsum.pipe.engine.runtime;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.builder.BaseJobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.common.CanRunWhen;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.stage.StageGroup;
import ru.yandex.market.tsum.pipe.engine.definition.stage.StageRef;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.StoredResourceContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.BeanRegistrar;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobScheduler;
import ru.yandex.market.tsum.pipe.engine.runtime.state.calculator.PipeStateCalculatorTestBase;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StageGroupState;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.FailingJob;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.jobs.ConvertMultipleRes1ToRes2;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.jobs.JobThatShouldProduceRes1ButFailsInstead;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.jobs.ProduceRes1;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.jobs.ProduceRes1AndFail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType.FAILED;
import static ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType.QUEUED;
import static ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType.SUBSCRIBERS_FAILED;
import static ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType.SUCCESSFUL;

/**
 * @author Nikolay Firov
 * @date 14.12.2017
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RunWhenAnyTest extends PipeStateCalculatorTestBase {
    private static final String ZERO_JOB = "zero_job";
    private static final String FIRST_JOB = "first_job";
    private static final String SECOND_JOB = "second_job";
    private static final String THIRD_JOB = "third_job";
    private static final String FIRST_STAGE = "first_stage";
    private static final String SECOND_STAGE = "second_stage";
    private static final String THIRD_STAGE = "third_stage";
    private static final String STAGE_GROUP_ID = "stage_group";

    private StageGroup stageGroup;

    @Before
    public void setUp() {
        stageGroup = new StageGroup(FIRST_STAGE, SECOND_STAGE, THIRD_STAGE);
        stageService.save(StageGroupState.create(STAGE_GROUP_ID));
    }

    @Test
    public void schedulesJobWithOrUpstream() {
        String pipeId = BeanRegistrar.registerNamedBean(trianglePipe(false), applicationContext);
        String launchId = activateLaunch(pipeId);

        pipeTester.raiseJobExecuteEventsChain(launchId, FIRST_JOB);

        List<TestJobScheduler.TriggeredJob> queuedCommands = new ArrayList<>(testJobScheduler.getTriggeredJobs());

        Assert.assertEquals(3, queuedCommands.size());
        Assert.assertEquals(THIRD_JOB, queuedCommands.get(2).getJobLaunchId().getJobId());
    }

    @Test
    public void setsReadyToRun() {
        String pipeId = BeanRegistrar.registerNamedBean(trianglePipe(true), applicationContext);
        String launchId = activateLaunch(pipeId);

        pipeTester.raiseJobExecuteEventsChain(launchId, FIRST_JOB);

        List<TestJobScheduler.TriggeredJob> queuedCommands = new ArrayList<>(testJobScheduler.getTriggeredJobs());
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(launchId);
        JobState jobState = pipeLaunch.getJobState(THIRD_JOB);

        Assert.assertEquals(2, queuedCommands.size());
        Assert.assertTrue(jobState.isReadyToRun());
    }

    @Test
    public void restartsOnlyOnce() throws Throwable {
        String launchId = pipeTester.runPipeToCompletion(trianglePipe(true));

        pipeTester.triggerJob(launchId, FIRST_JOB);
        Queue<TestJobScheduler.TriggeredJob> queuedCommands = testJobScheduler.getTriggeredJobs();
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(launchId);
        JobState jobState = pipeLaunch.getJobState(THIRD_JOB);

        Assert.assertEquals(1, queuedCommands.size());
        Assert.assertFalse(jobState.isOutdated());
    }

    @Test
    public void producesTwoResourceWhenBothUpstreamsReady() throws Throwable {
        String launchId = pipeTester.runPipeToCompletion(trianglePipe(true));

        pipeTester.triggerJob(launchId, THIRD_JOB);
        pipeTester.runScheduledJobsToCompletion();

        StoredResourceContainer producedResources = pipeTester.getProducedResources(launchId, THIRD_JOB);
        Assert.assertEquals(2, producedResources.getResources().size());
    }

    @Test
    public void producesOneResourceWhenOneUpstreamReady() throws Throwable {
        String launchId = pipeTester.runPipeToCompletion(trianglePipeWithFailureJob(true));

        pipeTester.triggerJob(launchId, THIRD_JOB);
        pipeTester.runScheduledJobsToCompletion();

        StoredResourceContainer producedResources = pipeTester.getProducedResources(launchId, THIRD_JOB);
        Assert.assertEquals(1, producedResources.getResources().size());
    }

    @Test
    public void marksAsOutdatedAfterBothUpstreamRestart() throws Throwable {
        String launchId = pipeTester.runPipeToCompletion(trianglePipe(true));

        pipeTester.triggerJob(launchId, THIRD_JOB);
        pipeTester.runScheduledJobsToCompletion();

        PipeLaunch pipeLaunch = pipeLaunchDao.getById(launchId);
        JobState jobState = pipeLaunch.getJobState(THIRD_JOB);
        Assert.assertFalse(jobState.isOutdated());

        pipeTester.triggerJob(launchId, SECOND_JOB);
        pipeTester.triggerJob(launchId, FIRST_JOB);
        pipeTester.runScheduledJobsToCompletion();

        pipeLaunch = pipeLaunchDao.getById(launchId);
        jobState = pipeLaunch.getJobState(THIRD_JOB);
        Assert.assertTrue(jobState.isOutdated());
    }

    static Pipeline trianglePipe(boolean withManualTrigger) {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder first = builder.withJob(ProduceRes1.class)
            .withId(FIRST_JOB);

        JobBuilder second = builder.withJob(ProduceRes1.class)
            .withId(SECOND_JOB);

        JobBuilder third = builder.withJob(ConvertMultipleRes1ToRes2.class)
            .withUpstreams(CanRunWhen.ANY_COMPLETED, first, second)
            .withId(THIRD_JOB);

        if (withManualTrigger) {
            third.withManualTrigger();
        }

        return builder.build();
    }

    static Pipeline trianglePipeWithFailureJob(boolean withManualTrigger) {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder first = builder.withJob(ProduceRes1.class)
            .withId(FIRST_JOB);

        JobBuilder second = builder.withJob(JobThatShouldProduceRes1ButFailsInstead.class)
            .withId(SECOND_JOB);

        JobBuilder third = builder.withJob(ConvertMultipleRes1ToRes2.class)
            .withUpstreams(CanRunWhen.ANY_COMPLETED, first, second)
            .withId(THIRD_JOB);

        if (withManualTrigger) {
            third.withManualTrigger();
        }

        return builder.build();
    }

    @Test
    public void whenOneOptionalUpstreamWasNotLaunched_LastJobShouldTriggersSuccessfully() throws Throwable {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder first = builder.withJob(DummyJob.class);
        JobBuilder second = builder.withJob(DummyJob.class).withManualTrigger();
        JobBuilder third = builder.withJob(ConvertMultipleRes1ToRes2.class)
            .withUpstreams(CanRunWhen.ANY_COMPLETED, first, second);

        builder.withJob(ConvertMultipleRes1ToRes2.class).withUpstreams(third).withId("last");

        String launchId = pipeTester.runPipeToCompletion(builder.build());
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(launchId);
        JobState jobState = pipeLaunch.getJobState("last");
        Assert.assertEquals(SUCCESSFUL, jobState.getLastStatusChangeType());
    }

    /**
     * https://github.yandex-team.ru/market-infra/tsum/pull/1794#issuecomment-862258
     */
    @Test
    public void whenOneOptionalUpstreamProducesResourcesAndFails_ItsResourcesShouldNotBePassedToDownstreams()
        throws InterruptedException {

        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder first = builder.withJob(DummyJob.class);
        JobBuilder second = builder.withJob(ProduceRes1AndFail.class);

        JobBuilder third = builder.withJob(ConvertMultipleRes1ToRes2.class)
            .withUpstreams(CanRunWhen.ANY_COMPLETED, first, second);

        String launchId = pipeTester.runPipeToCompletion(builder.build());
        JobState jobState = pipeLaunchDao.getById(launchId).getJobState(third.getId());
        Assert.assertEquals(SUCCESSFUL, jobState.getLastStatusChangeType());
        Assert.assertTrue(jobState.getLastLaunch().getConsumedResources().getResources().isEmpty());
    }

    // region Staged pipelines

    @Test
    public void shouldNotUnlockStage_WhenAnyItsJobIsInProgress() throws Exception {
        // arrange
        Pipeline pipeline = stagedDiamondPipeline();

        // act
        String pipeLaunchId = pipeTester.activateLaunch(pipeline, STAGE_GROUP_ID, Collections.emptyList());
        pipeTester.runScheduledJobToCompletion(ZERO_JOB);
        pipeTester.runScheduledJobToCompletion(FIRST_JOB);

        // assert
        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(pipeLaunchId);
        Assert.assertEquals(SUCCESSFUL, pipeLaunch.getJobState(FIRST_JOB).getLastStatusChangeType());
        Assert.assertEquals(QUEUED, pipeLaunch.getJobState(SECOND_JOB).getLastStatusChangeType());
        Assert.assertEquals(QUEUED, pipeLaunch.getJobState(THIRD_JOB).getLastStatusChangeType());

        StageGroupState stageGroupState = stageService.get(STAGE_GROUP_ID);
        Assert.assertEquals(
            Arrays.asList(FIRST_STAGE, SECOND_STAGE), getAcquiredStageNames(stageGroupState, pipeLaunchId)
        );
    }

    @Test
    public void shouldNotUnlockStage_WhenJobInStageIsNotStartedYet() throws Exception {
        // arrange
        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder zeroJob = builder.withJob(DummyJob.class)
            .beginStage(stageGroup.getStage(FIRST_STAGE))
            .withId(ZERO_JOB);

        JobBuilder firstJob = builder.withJob(DummyJob.class)
            .withUpstreams(zeroJob)
            .withId(FIRST_JOB);

        JobBuilder secondJob = builder.withJob(DummyJob.class)
            .withUpstreams(firstJob)
            .withId(SECOND_JOB);

        JobBuilder thirdJob = builder.withJob(DummyJob.class)
            .withUpstreams(CanRunWhen.ANY_COMPLETED, zeroJob, secondJob)
            .beginStage(stageGroup.getStage(SECOND_STAGE))
            .withId(THIRD_JOB);

        // act
        String pipeLaunchId = pipeTester.activateLaunch(builder.build(), STAGE_GROUP_ID, Collections.emptyList());
        pipeTester.runScheduledJobToCompletion(ZERO_JOB);
        pipeTester.runScheduledJobToCompletion(FIRST_JOB);

        // assert
        StageGroupState stageGroupState = stageService.get(STAGE_GROUP_ID);
        Assert.assertEquals(
            Arrays.asList(FIRST_STAGE, SECOND_STAGE), getAcquiredStageNames(stageGroupState, pipeLaunchId)
        );
    }

    @Test
    public void shouldNotDisablePipeline_IfJobOnFirstStageIsStillRunning() throws Exception {
        // arrange
        Pipeline pipeline = stagedDiamondPipeline();

        // act
        String pipeLaunchId = pipeTester.activateLaunch(pipeline, STAGE_GROUP_ID, Collections.emptyList());
        pipeTester.runScheduledJobToCompletion(ZERO_JOB);
        pipeTester.runScheduledJobToCompletion(FIRST_JOB);
        pipeTester.runScheduledJobToCompletion(THIRD_JOB);

        // assert
        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(pipeLaunchId);
        Assert.assertFalse(pipeLaunch.isDisabled());

        StageGroupState stageGroupState = stageService.get(STAGE_GROUP_ID);

        // Здесь не очень очевидный момент, почему не отпускается SECOND_STAGE, ведь на ней все джобы завершились.
        // Дело в том, что последняя стадия не пытается отпуститься при завершении последней джобы в ней,
        // потому что полагается на то что сразу же следом весь пайплайн будет выкинут из очереди пайплайнов.
        Assert.assertEquals(
            Arrays.asList(FIRST_STAGE, SECOND_STAGE), getAcquiredStageNames(stageGroupState, pipeLaunchId)
        );
    }

    @Test
    public void shouldReleaseSecondStage_WhenJobOnSecondsStageIsFinished_EvenIfJobOnFirstStageIsStillRunning()
        throws Exception {

        // arrange
        PipelineBuilder builder = new PipelineBuilder(stagedDiamondPipeline());

        List<BaseJobBuilder<?>> upstreams = builder.getJobBuildersWithoutDownstreams();
        builder.withJob(DummyJob.class).withUpstreams(upstreams).beginStage(stageGroup.getStage(THIRD_STAGE));
        Pipeline pipeline = builder.build();

        // act
        String pipeLaunchId = pipeTester.activateLaunch(pipeline, STAGE_GROUP_ID, Collections.emptyList());
        pipeTester.runScheduledJobToCompletion(ZERO_JOB);
        pipeTester.runScheduledJobToCompletion(FIRST_JOB);
        pipeTester.runScheduledJobToCompletion(THIRD_JOB);

        // assert
        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(pipeLaunchId);
        Assert.assertFalse(pipeLaunch.isDisabled());

        StageGroupState stageGroupState = stageService.get(STAGE_GROUP_ID);

        // Здесь стоит заметить, что в захваченных стадиях появляется "дырка", но это не страшно,
        // SECOND_STAGE всё равно никто не сможет занять, иначе нарушился бы инвариант.
        Assert.assertEquals(
            Arrays.asList(FIRST_STAGE, THIRD_STAGE), getAcquiredStageNames(stageGroupState, pipeLaunchId)
        );
    }

    @Test
    public void shouldDisablePipeline_WhenLastRunningJobOnFirstStageIsFinished() throws Exception {
        // arrange
        Pipeline pipeline = stagedDiamondPipeline();

        // act
        String pipeLaunchId = pipeTester.activateLaunch(pipeline, STAGE_GROUP_ID, Collections.emptyList());
        pipeTester.runScheduledJobToCompletion(ZERO_JOB);
        pipeTester.runScheduledJobToCompletion(FIRST_JOB);
        pipeTester.runScheduledJobToCompletion(THIRD_JOB);
        pipeTester.runScheduledJobToCompletion(SECOND_JOB);

        // assert
        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(pipeLaunchId);
        Assert.assertTrue(pipeLaunch.isDisabled());
        ensureNoFailedJobs(pipeLaunch);

        StageGroupState stageGroupState = stageService.get(STAGE_GROUP_ID);
        Assert.assertFalse(stageGroupState.getQueueItem(pipeLaunchId).isPresent());
    }

    @Test
    public void shouldReleaseStage_WhenAllItsJobsAreFinished() throws Exception {
        // arrange
        Pipeline pipeline = stagedDiamondPipeline();

        // act
        String pipeLaunchId = pipeTester.activateLaunch(pipeline, STAGE_GROUP_ID, Collections.emptyList());
        pipeTester.runScheduledJobToCompletion(ZERO_JOB);
        pipeTester.runScheduledJobToCompletion(FIRST_JOB);
        pipeTester.runScheduledJobToCompletion(SECOND_JOB);

        // assert
        StageGroupState stageGroupState = stageService.get(STAGE_GROUP_ID);
        Assert.assertEquals(
            Collections.singletonList(SECOND_STAGE), getAcquiredStageNames(stageGroupState, pipeLaunchId)
        );
    }

    @Test
    public void shouldReleaseStageEven_WhenFirstJobFailed_IfItCanPassToNextStage() throws Exception {
        // arrange
        Pipeline pipeline = stagedDiamondPipeline(FIRST_JOB);

        // act
        String pipeLaunchId = pipeTester.activateLaunch(pipeline, STAGE_GROUP_ID, Collections.emptyList());
        pipeTester.runScheduledJobToCompletion(ZERO_JOB);
        pipeTester.runScheduledJobToCompletion(FIRST_JOB);
        pipeTester.runScheduledJobToCompletion(SECOND_JOB);

        // assert
        StageGroupState stageGroupState = stageService.get(STAGE_GROUP_ID);
        Assert.assertEquals(
            Collections.singletonList(SECOND_STAGE), getAcquiredStageNames(stageGroupState, pipeLaunchId)
        );
    }

    @Test
    public void shouldReleaseStageEven_WhenSecondJobFailed_IfItCanPassToNextStage() throws Exception {
        // arrange
        Pipeline pipeline = stagedDiamondPipeline(SECOND_JOB);

        // act
        String pipeLaunchId = pipeTester.activateLaunch(pipeline, STAGE_GROUP_ID, Collections.emptyList());
        pipeTester.runScheduledJobToCompletion(ZERO_JOB);
        pipeTester.runScheduledJobToCompletion(FIRST_JOB);
        pipeTester.runScheduledJobToCompletion(SECOND_JOB);

        // assert
        StageGroupState stageGroupState = stageService.get(STAGE_GROUP_ID);
        Assert.assertEquals(
            Collections.singletonList(SECOND_STAGE), getAcquiredStageNames(stageGroupState, pipeLaunchId)
        );
    }

    private Pipeline stagedDiamondPipeline(String... failingJobIds) {
        HashSet<String> failingJobIdSet = new HashSet<>(Arrays.asList(failingJobIds));
        PipelineBuilder builder = PipelineBuilder.create();

        Function<String, JobBuilder> jobBuilder = (jobId) -> builder
            .withJob(failingJobIdSet.contains(jobId) ? FailingJob.class : DummyJob.class)
            .withId(jobId);

        JobBuilder zeroJob = jobBuilder.apply(ZERO_JOB)
            .beginStage(stageGroup.getStage(FIRST_STAGE));

        JobBuilder firstJob = jobBuilder.apply(FIRST_JOB).withUpstreams(zeroJob);
        JobBuilder secondJob = jobBuilder.apply(SECOND_JOB).withUpstreams(zeroJob);

        JobBuilder thirdJob = jobBuilder.apply(THIRD_JOB)
            .withUpstreams(CanRunWhen.ANY_COMPLETED, firstJob, secondJob)
            .beginStage(stageGroup.getStage(SECOND_STAGE));

        return builder.build();
    }

    private List<String> getAcquiredStageNames(StageGroupState stageGroupState, String pipeLaunchId) {
        return stageGroupState.getAcquiredStages(pipeLaunchId).stream()
            .map(StageRef::getId)
            .sorted()
            .collect(Collectors.toList());
    }

    // endregion

    private void ensureNoFailedJobs(PipeLaunch pipeLaunch) {
        List<String> failedJobIds = pipeLaunch.getJobs().values().stream()
            .filter(j -> EnumSet.of(FAILED, SUBSCRIBERS_FAILED).contains(j.getLastStatusChangeType()))
            .map(JobState::getJobId)
            .collect(Collectors.toList());

        Assert.assertEquals(Collections.emptyList(), failedJobIds);
    }
}