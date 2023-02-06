package ru.yandex.market.tsum.release.dao;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import org.apache.curator.framework.CuratorFramework;
import org.bson.types.ObjectId;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.commune.bazinga.impl.FullJobId;
import ru.yandex.market.tsum.arcadia.ArcadiaCache;
import ru.yandex.market.tsum.clients.arcadia.TrunkArcadiaClient;
import ru.yandex.market.tsum.clients.conductor.ConductorBranch;
import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.clients.sandbox.SandboxReleaseType;
import ru.yandex.market.tsum.clients.startrek.StartrekClient;
import ru.yandex.market.tsum.config.PipelineScanConfiguration;
import ru.yandex.market.tsum.config.ReleaseConfiguration;
import ru.yandex.market.tsum.core.StoredObject;
import ru.yandex.market.tsum.dao.PipelinesDao;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.pipeline.PipelineEntity;
import ru.yandex.market.tsum.entity.project.DeliveryMachineEntity;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.notifications.PipeLaunchNotificationSettingsDao;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.stage.StageRef;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.di.ResourceService;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.events.JobRunningEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.settings.ProjectNotificationSettingsDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.StageGroupDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunchRef;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StageGroupState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipelines.common.jobs.conductor_deploy.ConductorDeployJob;
import ru.yandex.market.tsum.pipelines.common.jobs.conductor_deploy.ConductorDeployJobConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.nanny.NannyReleaseJob;
import ru.yandex.market.tsum.pipelines.common.jobs.nanny.NannyReleaseJobConfig;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogEntry;
import ru.yandex.market.tsum.pipelines.common.resources.DeliveryPipelineParams;
import ru.yandex.market.tsum.pipelines.test.TestSimplePipeline;
import ru.yandex.market.tsum.release.ReleaseJobTags;
import ru.yandex.market.tsum.release.ReleaseStages;
import ru.yandex.market.tsum.release.RepositoryType;
import ru.yandex.market.tsum.release.dao.title_providers.OrdinalTitleProvider;
import ru.yandex.market.tsum.release.delivery.ArcadiaVcsChange;
import ru.yandex.market.tsum.release.utils.ReleaseTimelineUtils;
import ru.yandex.market.tsum.test_data.TestRepositoryCommitFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 31.07.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    PipeServicesConfig.class,
    ReleaseServiceTest.Config.class,
    ReleaseConfiguration.class,
    TestConfig.class,
    PipelineScanConfiguration.class,
    MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ReleaseServiceTest {
    private static final String GITHUB_COMMIT_REVISION = "1fff5d";
    private static final ResourceContainer DELIVERY_PIPELINE_RESOURCES = new ResourceContainer(
        new DeliveryPipelineParams(GITHUB_COMMIT_REVISION, "0fff5d", "0fff5d")
    );
    private static final String PROJECT_ID = "test";
    private static final String TRIGGERED_BY = "user42";
    private static final Date COMMIT_DATE = new Date();
    private static final String STAGE_GROUP_ID = "STAGE_GROUP_ID";

    private final CreateReleaseCommandBuilder commandBuilder = CreateReleaseCommandBuilder.create()
        .withProjectId(PROJECT_ID)
        .withResourceContainer(DELIVERY_PIPELINE_RESOURCES)
        .withTriggeredBy(TRIGGERED_BY);

    @Autowired
    private ReleaseService sut;

    @Autowired
    private PipeTester pipeTester;

    @Autowired
    private PipeLaunchDao pipeLaunchDao;

    @Autowired
    private ReleaseDao releaseDao;

    @Autowired
    private StageGroupDao stageService;

    @Autowired
    private ChangelogCommitDao changelogCommitDao;

    @Autowired
    private GitHubClient gitHubClient;

    @Autowired
    private ProjectsDao projectsDao;

    @Autowired
    private PipelinesDao pipelinesDao;

    @Autowired
    private MongoConverter mongoConverter;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private ArcadiaCache arcadiaCache;

    @Autowired
    private StartrekClient startrekClient;

    @Before
    public void setUp() {
        RepositoryCommit commit = TestRepositoryCommitFactory.commit(
            GITHUB_COMMIT_REVISION, "algebraic", "TEST-1", COMMIT_DATE
        );

        Mockito.when(gitHubClient.getCommit(Mockito.any(), Mockito.any())).thenReturn(commit);

        Mockito.when(projectsDao.get(Mockito.anyString())).thenReturn(new ProjectEntity());
        Mockito.when(pipelinesDao.get(Mockito.anyString())).thenReturn(new PipelineEntity());

        pipeTester.createStageGroupState(STAGE_GROUP_ID, ReleaseStages.INSTANCE.getStages());
    }

    @Test
    public void runReleaseToTheEnd() throws Exception {
        Release release = sut.launchRelease(commandBuilder.withPipeId(TestSimplePipeline.SIMPLE_RELEASE_PIPE_ID));

        pipeTester.runScheduledJobsToCompletion();

        ensureReleaseFinished(release.getId());
    }

    @Test
    public void runReleaseToTheEnd_WithFinalJobTag() throws Exception {
        Release release =
            sut.launchRelease(commandBuilder.withPipeId(TestSimplePipeline.SIMPLE_PIPE_WITH_FINAL_JOB_ID));

        pipeTester.runScheduledJobsToCompletion();

        ensureReleaseFinished(release.getId());
    }

    @Test
    public void runStagedReleaseToTheEnd() throws Exception {
        ProjectEntity project = new ProjectEntity();
        project.setDeliveryMachines(
            Collections.singletonList(
                new DeliveryMachineEntity(
                    DeliveryMachineSettings.builder()
                        .withTitle("title")
                        .withStageGroupId(STAGE_GROUP_ID)
                        .withGithubSettings("")
                        .withPipeline("simple-staged-pipeline", "title", new OrdinalTitleProvider(null))
                        .build(),
                    mongoConverter
                )
            )
        );
        Mockito.when(projectsDao.get(Mockito.anyString())).thenReturn(project);

        Release release = sut.launchRelease(
            commandBuilder
                .withStageGroupId(STAGE_GROUP_ID)
                .withPipeId(TestSimplePipeline.SIMPLE_STAGED_PIPELINE_ID)
                .withDisabledManualTriggers(Collections.singletonList(TestSimplePipeline.JOB_ID))
        );
        pipeTester.runScheduledJobsToCompletion();

        ensureReleaseFinished(release.getId());
        Assert.assertEquals(COMMIT_DATE.toInstant(), release.getCommit().getCreatedDate());
        Assert.assertEquals(GITHUB_COMMIT_REVISION, release.getCommit().getRevision());
    }

    @Test
    public void cancelRunningStagedRelease() throws Exception {
        Release release = sut.launchRelease(
            commandBuilder
                .withStageGroupId(STAGE_GROUP_ID)
                .withPipeId(TestSimplePipeline.TWO_JOB_STAGED_PIPELINE_ID)
        );

        String pipeLaunchId = release.getPipeLaunchIds().get(0);
        pipeTester.recalcPipeLaunch(
            pipeLaunchId, new JobRunningEvent(TestSimplePipeline.FIRST_JOB_ID, 1, Mockito.mock(FullJobId.class))
        );

        FinishCause finishCause = FinishCause.cancelledByFix("100500");
        sut.cancelRelease(release, finishCause, null);

        Assert.assertFalse(pipeTester.getPipeLaunch(pipeLaunchId).isDisabled());
        release = releaseDao.getRelease(release.getId());
        Assert.assertEquals(finishCause, release.getFinishCause());
        Assert.assertEquals(ReleaseState.FINISHING_WAITING_FOR_JOB_DISABLING, release.getState());

        pipeTester.runScheduledJobsToCompletion();
        ensureReleaseFinished(release.getId());
    }

    @Test
    public void cancelRunningStagedReleaseOnUninterruptableStage() throws InterruptedException {
        // arrange
        Release release = sut.launchRelease(
            commandBuilder
                .withStageGroupId(STAGE_GROUP_ID)
                .withPipeId(TestSimplePipeline.TWO_JOB_STAGED_PIPELINE_ID)
        );

        String pipeLaunchId = release.getPipeLaunchIds().get(0);
        pipeTester.raiseJobExecuteEventsChain(pipeLaunchId, TestSimplePipeline.FIRST_JOB_ID);

        // act
        sut.cancelRelease(release, FinishCause.cancelledByFix("100500"), null);

        // assert
        Assert.assertFalse(pipeTester.getPipeLaunch(pipeLaunchId).isDisabled());
        release = releaseDao.getRelease(release.getId());
        Assert.assertEquals(FinishCause.cancelledByFix("100500"), release.getFinishCause());
        Assert.assertEquals(ReleaseState.FINISHING_WAITING_FOR_JOB_DISABLING, release.getState());

        // act again
        pipeTester.runScheduledJobsToCompletion();

        // assert
        release = releaseDao.getRelease(release.getId());
        Assert.assertEquals(FinishCause.completed(), release.getFinishCause());
        ensureReleaseFinished(release.getId());
    }

    @Test
    public void cancelWaitingStagedRelease() throws InterruptedException {
        // arrange
        launchBlockingRelease();

        Release blockedRelease = sut.launchRelease(
            commandBuilder
                .withStageGroupId(STAGE_GROUP_ID)
                .withPipeId(TestSimplePipeline.TWO_JOB_STAGED_PIPELINE_ID)
        );

        String blockedPipeLaunchId = blockedRelease.getPipeLaunchId();
        pipeTester.runScheduledJobToCompletion(TestSimplePipeline.FIRST_JOB_ID);

        Assert.assertEquals(
            StatusChangeType.WAITING_FOR_STAGE,
            pipeTester.getJobLastLaunch(blockedPipeLaunchId, TestSimplePipeline.SECOND_JOB_ID).getLastStatusChangeType()
        );

        // act
        FinishCause finishCause = FinishCause.cancelledByFix("100500");
        sut.cancelRelease(blockedRelease, finishCause, null);

        // assert
        Assert.assertEquals(
            ReleaseState.FINISHING_CLEANING_UP,
            releaseDao.getRelease(blockedRelease.getId()).getState()
        );

        // act again
        pipeTester.runScheduledJobsToCompletion();

        // assert
        ensureReleaseFinished(blockedRelease.getId());
    }

    private void launchBlockingRelease() throws InterruptedException {
        Release blockingRelease = sut.launchRelease(
            commandBuilder
                .withStageGroupId(STAGE_GROUP_ID)
                .withPipeId(TestSimplePipeline.TWO_JOB_STAGED_PIPELINE_ID)
        );

        pipeTester.runScheduledJobToCompletion(TestSimplePipeline.FIRST_JOB_ID);
    }

    @Test
    public void cancelRegularRelease() throws Exception {
        Release release = sut.launchRelease(
            commandBuilder.withPipeId(TestSimplePipeline.SIMPLE_PIPE_WITH_MANUAL_TRIGGER_ID)
        );

        sut.cancelRelease(release, FinishCause.cancelledManually("algebraic"), null);

        ensureReleaseFinished(release.getId());
    }

    @Test
    public void disablesManualTrigger() throws Exception {
        Release release = sut.launchRelease(
            commandBuilder
                .withStageGroupId(STAGE_GROUP_ID)
                .withPipeId(TestSimplePipeline.SIMPLE_PIPE_WITH_MANUAL_TRIGGER_ID)
                .withDisabledManualTriggers(Collections.singletonList(TestSimplePipeline.JOB_ID))
        );

        pipeTester.runScheduledJobsToCompletion();

        ensureReleaseFinished(release.getId());
    }

    @Test
    public void saveChangelog() {
        // arrange
        Release release = launchRelease();

        // act
        List<ChangelogEntry> changelogEntries = Arrays.asList(
            changelogEntry(1L), changelogEntry(2L), changelogEntry(3L)
        );

        sut.saveChangelog(new ObjectId(release.getPipeLaunchId()), changelogEntries, RepositoryType.ARCADIA);

        // assert
        release = releaseDao.getRelease(release.getId());
        Assert.assertEquals(Arrays.asList("3", "2", "1"), release.getChangelogRevisions());

        List<ChangelogCommit> commits = changelogCommitDao.getByRevisions(release.getChangelogRevisions());
        Assert.assertEquals(3, commits.size());

        ChangelogCommit firstCommit = commits.get(0);
        Assert.assertEquals("change 3", firstCommit.getMessage());
        Assert.assertEquals("algebraic", firstCommit.getAuthorLogin());
        Assert.assertEquals(3L, firstCommit.getDate().getEpochSecond());
    }

    @Test
    public void saveOverlappingChangelog() {
        // arrange
        Release firstRelease = launchRelease();
        Release secondRelease = launchRelease();

        // act
        sut.saveChangelog(
            new ObjectId(firstRelease.getPipeLaunchId()),
            Arrays.asList(changelogEntry(1L), changelogEntry(2L)),
            RepositoryType.ARCADIA
        );

        sut.saveChangelog(
            new ObjectId(secondRelease.getPipeLaunchId()),
            Arrays.asList(changelogEntry(1L), changelogEntry(2L), changelogEntry(3L)),
            RepositoryType.ARCADIA
        );

        // assert
        secondRelease = releaseDao.getRelease(secondRelease.getId());
        Assert.assertEquals(
            3,
            changelogCommitDao.getByRevisions(secondRelease.getChangelogRevisions()).size()
        );
    }

    @Test
    public void launchRelease_ShouldAddCleanupJob_IfItHasJobProducingReleaseInfo() {
        // act
        Release release = sut.launchRelease(
            commandBuilder
                .withStageGroupId(STAGE_GROUP_ID)
                .withPipeId(TestSimplePipeline.SIMPLE_STAGED_PIPELINE_ID)
                .withDisabledManualTriggers(Collections.singletonList(TestSimplePipeline.JOB_ID))
        );

        // assert
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(release.getPipeLaunchId());
        JobState cleanupJob = getCleanupJob(pipeLaunch);
        Assert.assertNotNull(cleanupJob);
        Assert.assertFalse(cleanupJob.isVisible());
        Assert.assertTrue(cleanupJob.isDisabled());
    }

    @Test
    public void launchRelease_ShouldNotAddCleanupJob_IfItHasNoJobProducingReleaseInfo() {
        // act
        Release release = sut.launchRelease(
            commandBuilder.withPipeId(TestSimplePipeline.SIMPLE_PIPE_ID)
        );

        // assert
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(release.getPipeLaunchId());
        JobState cleanupJob = getCleanupJob(pipeLaunch);
        Assert.assertNull(cleanupJob);
    }

    @Test
    public void cancelReleaseManually_ShouldDisableJobsImmediately_IfThereAreNoRunningJobs() {
        // arrange
        Release release = sut.launchRelease(
            commandBuilder
                .withStageGroupId(STAGE_GROUP_ID)
                .withPipeId(TestSimplePipeline.SIMPLE_STAGED_PIPELINE_ID)
        );

        // act
        sut.cancelReleaseManually(release, "algebraic", null, false);

        // assert
        release = releaseDao.getRelease(release.getId());
        Assert.assertEquals(ReleaseState.FINISHING_CLEANING_UP, release.getState());

        PipeLaunch pipeLaunch = pipeLaunchDao.getById(release.getPipeLaunchId());

        assertJobsDisabled(getJobsExceptCleanup(pipeLaunch));
        Assert.assertTrue(getCleanupJob(pipeLaunch).isVisible());
        Assert.assertFalse(getCleanupJob(pipeLaunch).isDisabled());

        // act again
        pipeTester.runScheduledJobsToCompletion();

        // assert
        Assert.assertEquals(1, getCleanupJob(pipeLaunch).getLaunches().size());
        release = releaseDao.getRelease(release.getId());
        ensureReleaseFinished(release.getId());
    }

    @Test
    public void cancelReleaseManually_ShouldDisableJobsEventually_IfThereAreRunningJobs() throws InterruptedException {
        // arrange
        Release release = sut.launchRelease(
            commandBuilder
                .withStageGroupId(STAGE_GROUP_ID)
                .withPipeId(TestSimplePipeline.TWO_JOB_STAGED_PIPELINE_ID)
        );

        // act
        sut.cancelReleaseManually(release, "algebraic", null, false);
        pipeTester.runScheduledJobToCompletion(TestSimplePipeline.FIRST_JOB_ID);

        // assert
        release = releaseDao.getRelease(release.getId());
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(release.getPipeLaunchId());

        Assert.assertEquals(ReleaseState.FINISHING_CLEANING_UP, release.getState());
        assertJobsDisabled(getJobsExceptCleanup(pipeLaunch));
        Assert.assertTrue(getCleanupJob(pipeLaunch).isVisible());
        Assert.assertFalse(getCleanupJob(pipeLaunch).isDisabled());

        Assert.assertEquals(
            Sets.newHashSet("testing"),
            getAcquiredStageIds(pipeLaunch)
        );

        // act again
        pipeTester.runScheduledJobsToCompletion();

        // assert
        Assert.assertEquals(1, getCleanupJob(pipeLaunch).getLaunches().size());
        release = releaseDao.getRelease(release.getId());
        ensureReleaseFinished(release.getId());
    }

    @Test
    public void completeReleaseManually_ShouldDisableJobsImmediately_IfThereAreNoRunningJobs() {
        // arrange
        Release release = sut.launchRelease(
            commandBuilder
                .withStageGroupId(STAGE_GROUP_ID)
                .withPipeId(TestSimplePipeline.SIMPLE_STAGED_PIPELINE_ID)
        );

        // act
        sut.completeReleaseManually(release, "algebraic");

        // assert
        release = releaseDao.getRelease(release.getId());
        Assert.assertEquals(ReleaseState.FINISHED, release.getState());

        PipeLaunch pipeLaunch = pipeLaunchDao.getById(release.getPipeLaunchId());

        assertJobsDisabled(pipeLaunch.getJobs().values());
        Assert.assertFalse(getCleanupJob(pipeLaunch).isVisible());
    }

    @Test
    public void completeReleaseManually_ShouldDisableJobsEventually_IfThereAreRunningJobs() {
        // arrange
        Release release = sut.launchRelease(
            commandBuilder
                .withStageGroupId(STAGE_GROUP_ID)
                .withPipeId(TestSimplePipeline.TWO_JOB_STAGED_PIPELINE_ID)
        );

        // act
        sut.completeReleaseManually(release, "algebraic");

        // assert
        release = releaseDao.getRelease(release.getId());
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(release.getPipeLaunchId());
        Assert.assertFalse(getCleanupJob(pipeLaunch).isVisible());

        // act again
        pipeTester.runScheduledJobsToCompletion();

        // assert
        release = releaseDao.getRelease(release.getId());
        ensureReleaseFinished(release.getId());
    }

    @Test
    public void getStableJobStates() {
        List<String> packagesForConductor = Arrays.asList("package1", "package2");
        List<String> nannyResources = Arrays.asList("resource1", "resource2");

        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(DummyJob.class, "notStableJob");
        builder.withJob(DummyJob.class, "stableJobByTag").withTags(ReleaseJobTags.STABLE_DEPLOY);
        builder.withJob(ConductorDeployJob.class, "notStableConductor")
            .withResources(ConductorDeployJobConfig.newBuilder(ConductorBranch.TESTING).build());
        builder.withJob(ConductorDeployJob.class, "stableConductor")
            .withResources(
                ConductorDeployJobConfig.newBuilder(ConductorBranch.STABLE)
                    .setPackagesToDeploy(packagesForConductor)
                    .build()
            );
        builder.withJob(NannyReleaseJob.class, "notStableNanny")
            .withResources(NannyReleaseJobConfig.builder(SandboxReleaseType.TESTING).build());
        builder.withJob(NannyReleaseJob.class, "stableNanny")
            .withResources(
                NannyReleaseJobConfig.builder(SandboxReleaseType.STABLE)
                    .withSandboxResourceType(nannyResources.toArray(new String[2]))
                    .build()
            );

        Release release = sut.launchRelease(
            commandBuilder.withCustomPipeline(builder.build())
                .withPipeId("getStableJobStatesTest")
        );
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(release.getPipeLaunchId());

        List<JobState> stableJobs = ReleaseTimelineUtils.getStableJobStates(pipeLaunch, resourceService);

        Assert.assertEquals(
            new HashSet<>(Arrays.asList("stableConductor", "stableNanny", "stableJobByTag")),
            new HashSet<>(stableJobs.stream().map(JobState::getJobId).collect(Collectors.toList()))
        );
    }

    @Test
    public void createReleaseLinks() throws ExecutionException, InterruptedException {
        PipeLaunch pipeLaunchprev = new PipeLaunch();
        pipeLaunchDao.save(pipeLaunchprev);

        Release prevRelease = Release.builder()
            .withCommit("122", new Date().toInstant())
            .withDeliveryMachineId("d1")
            .withPipeLaunchId(pipeLaunchprev.getId().toString())
            .withPipeId("pip2")
            .withProjectId("pj1")
            .withId("51c32547e3cbb673dey0b62a")
            .build();

        releaseDao.save(prevRelease);

        PipeLaunch pipeLaunch = PipeLaunch.builder()
            .withLaunchRef(new PipeLaunchRef() {
                @Override
                public ObjectId getId() {
                    return new ObjectId("507f191e810c19729de860ea");
                }

                @Override
                public String getPipeId() {
                    return "pip1";
                }
            })
            .withProjectId("pj1")
            .withTriggeredBy("someone")
            .withStagesGroupId("stg1")
            .withStages(List.of())
            .build();
        pipeLaunchDao.save(pipeLaunch);

        Release currentRelease = Release.builder()
            .withCommit("123", new Date().toInstant())
            .withDeliveryMachineId("d1")
            .withPipeLaunchId(pipeLaunch.getId().toString())
            .withPipeId("pip1")
            .withProjectId("pj1")
            .withId("61c42547e3cca665dda0b77f")
            .build();
        releaseDao.save(currentRelease);

        ProjectEntity project = new ProjectEntity();
        project.setLinkReleasesToStartrekTicket(true);
        DeliveryMachineEntity deliveryMachine = new DeliveryMachineEntity();
        deliveryMachine.setVcsSettings(new StoredObject<>("", new ArcadiaSettings(), null));
        deliveryMachine.setPipelineId("pip1");
        project.setDeliveryMachines(List.of(deliveryMachine));
        project.setId("pj1");

        Mockito.when(projectsDao.get("pj1")).thenReturn(project);
        Mockito.when(arcadiaCache.get(123L)).thenReturn(
            new ArcadiaVcsChange(123L, new Date().toInstant(), null,  "123 MARKETDX-377 comment", null)
        );
        Mockito.doNothing().when(startrekClient).createRemoteLink(anyString(), any(), anyBoolean());
        List<ChangelogEntry> changelogEntries = List.of(
            new ChangelogEntry("122", ""), new ChangelogEntry("123", "")
        );
        Assert.assertEquals(List.of("123"), sut.getCommits(pipeLaunch, changelogEntries));
        sut.saveChangelog(
            new ObjectId(pipeLaunch.getId().toString()), changelogEntries, RepositoryType.ARCADIA
        );
        Mockito.verify(startrekClient).createRemoteLink(eq("MARKETDX-377"), any(), eq(false));
    }

    private Set<String> getAcquiredStageIds(PipeLaunch pipeLaunch) {
        Set<? extends StageRef> acquiredStages = stageService.get(pipeLaunch.getStageGroupId())
            .getAcquiredStages(pipeLaunch.getIdString());

        return acquiredStages.stream().map(StageRef::getId).collect(Collectors.toSet());
    }

    private void assertJobsDisabled(Collection<JobState> jobsExceptCleanup) {
        Assert.assertTrue(
            "Expected jobs to be disabled, got: " +
                jobsExceptCleanup.stream().collect(Collectors.toMap(JobState::getJobId, JobState::isDisabled)),
            jobsExceptCleanup.stream().allMatch(JobState::isDisabled)
        );
    }

    private JobState getCleanupJob(PipeLaunch pipeLaunch) {
        return pipeLaunch.getJobs().values().stream()
            .filter(ReleaseJobTags.IS_CLEANUP_JOB)
            .findFirst()
            .orElse(null);
    }

    private List<JobState> getJobsExceptCleanup(PipeLaunch pipeLaunch) {
        return pipeLaunch.getJobs().values().stream()
            .filter(ReleaseJobTags.IS_CLEANUP_JOB.negate())
            .collect(Collectors.toList());
    }

    public Release launchRelease() {
        return sut.launchRelease(
            commandBuilder
                .withStageGroupId(STAGE_GROUP_ID)
                .withPipeId(TestSimplePipeline.SIMPLE_STAGED_PIPELINE_ID)
                .withDisabledManualTriggers(Collections.singletonList(TestSimplePipeline.JOB_ID))
        );
    }

    private ChangelogEntry changelogEntry(long revision) {
        return new ChangelogEntry(
            Long.toString(revision), revision, "change " + revision, "algebraic", null
        );
    }

    private void ensureReleaseFinished(String releaseId) {
        Release release = releaseDao.getRelease(releaseId);
        Assert.assertEquals(ReleaseState.FINISHED, release.getState());
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(release.getPipeLaunchIds().get(0));
        Assert.assertTrue(pipeLaunch.isDisabled());

        if (pipeLaunch.isStaged()) {
            StageGroupState stageGroupState = stageService.get(pipeLaunch.getStageGroupId());
            Assert.assertFalse(stageGroupState.getQueueItem(pipeLaunch.getIdString()).isPresent());
        }
    }

    @Configuration
    public static class Config {
        @Autowired
        private MongoTemplate mongoTemplate;

        @Bean
        public GitHubClient gitHubClient() {
            return Mockito.mock(GitHubClient.class);
        }

        @Bean
        public TrunkArcadiaClient arcadiaClient() {
            return Mockito.mock(TrunkArcadiaClient.class);
        }

        @Bean
        public CuratorFramework curatorFramework() {
            return Mockito.mock(CuratorFramework.class);
        }

        @Bean
        public ProjectNotificationSettingsDao projectNotificationSettingsDao() {
            return new ProjectNotificationSettingsDao(mongoTemplate);
        }

        @Bean
        public PipeLaunchNotificationSettingsDao pipeLaunchNotificationSettingsDao() {
            return new PipeLaunchNotificationSettingsDao(mongoTemplate);
        }

        @Bean
        public ProjectsDao projectsDao() {
            return mock(ProjectsDao.class);
        }
    }
}
