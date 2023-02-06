package ru.yandex.market.tsum.percommit;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.egit.github.core.RepositoryCommit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.config.MultitestingCleanupConfiguration;
import ru.yandex.market.tsum.config.MultitestingConfiguration;
import ru.yandex.market.tsum.config.PipelineScanConfiguration;
import ru.yandex.market.tsum.config.ReleaseConfiguration;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.JanitorSettingEntity;
import ru.yandex.market.tsum.entity.project.NewCommitActionType;
import ru.yandex.market.tsum.entity.project.PerCommitLaunchType;
import ru.yandex.market.tsum.entity.project.PerCommitSettingsEntity;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.multitesting.MultitestingService;
import ru.yandex.market.tsum.multitesting.model.JanitorEventType;
import ru.yandex.market.tsum.multitesting.model.MultitestingEnvironment;
import ru.yandex.market.tsum.per_commit.PerCommitService;
import ru.yandex.market.tsum.per_commit.dao.PerCommitBranchStateDao;
import ru.yandex.market.tsum.per_commit.dao.PerCommitBranchStateHistoryDao;
import ru.yandex.market.tsum.per_commit.entity.PerCommitBranchLaunchSetting;
import ru.yandex.market.tsum.per_commit.entity.PerCommitBranchStateEntity;
import ru.yandex.market.tsum.per_commit.entity.PerCommitBranchStateHistoryEntity;
import ru.yandex.market.tsum.per_commit.entity.PerCommitPipeLaunchState;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.di.ResourceService;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipelines.common.jobs.multitesting.MultitestingTags;
import ru.yandex.market.tsum.pipelines.common.resources.PerCommitLaunchParams;
import ru.yandex.market.tsum.pipelines.multitesting.MultitestingTestConfig;
import ru.yandex.startrek.client.Comments;
import ru.yandex.startrek.client.Events;

/**
 * @author Mishunin Andrei <a href="mailto:mishunin@yandex-team.ru"></a>
 * @date 05.06.2019
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    PipeServicesConfig.class,
    MultitestingPerCommitServiceTest.Config.class,
    PerCommitService.class,
    MultitestingConfiguration.class,
    MultitestingTestConfig.class,
    MultitestingCleanupConfiguration.class,
    ReleaseConfiguration.class,
    TestConfig.class,
    PipelineScanConfiguration.class,
    MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MultitestingPerCommitServiceTest {
    public static final String REPOSITORY_ID = "market-infra/test";
    public static final String PROJECT_ID = "test";
    public static final String BRANCH_NAME = "develop";

    @Autowired
    private PerCommitService perCommitService;
    @Autowired
    private PipeTester pipeTester;
    @Autowired
    private MultitestingService multitestingService;
    @Autowired
    private ProjectsDao projectsDao;
    @Autowired
    private PipeLaunchDao pipeLaunchDao;
    @Autowired
    private ResourceService resourceService;
    @Autowired
    private PerCommitBranchStateDao stateDao;
    @Autowired
    private PerCommitBranchStateHistoryDao stateHistoryDao;
    @Autowired
    private GitHubClient gitHubClient;
    @Autowired
    private Semaphore semaphore;

    @Before
    public void setUp() {
        Mockito.when(projectsDao.get(Mockito.anyString())).thenReturn(new ProjectEntity());
    }

    @Test
    public void runMultitestingPipelineToTheEnd() throws Exception {
        PerCommitBranchStateEntity state = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME)
            .withMultitesting(true)
            .withPipelineId(MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID)
            .withPullRequestId(null)
            .withPullRequestNumber(null)
            .withStatusCheckName(null)
            .build();

        PerCommitSettingsEntity setting = createPerCommitSettings(MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID,
            NewCommitActionType.WAITING_PREV_LAUNCH, false);
        runPerCommitPipeline(setting, "r1", state);
        pipeTester.runScheduledJobsToCompletion();

        MultitestingEnvironment environment = multitestingService.getEnvironment(
            state.getProject(), state.getMultitestingName());
        Assert.assertEquals(environment.getStatus(), MultitestingEnvironment.Status.READY);
    }

    @Test
    public void runMultitestingPipelineToTheEndWithCleanup() throws Exception {
        PerCommitBranchStateEntity state = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME)
            .withMultitesting(true)
            .withPipelineId(MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID)
            .withPullRequestId(null)
            .withPullRequestNumber(null)
            .withStatusCheckName(null)
            .build();

        PerCommitSettingsEntity setting = createPerCommitSettings(MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID,
            NewCommitActionType.WAITING_PREV_LAUNCH, false);
        setting.setJanitorSettings(
            Collections.singletonList(new JanitorSettingEntity(JanitorEventType.AFTER_READY, 0))
        );
        runPerCommitPipeline(setting, "r1", state);
        pipeTester.runScheduledJobsToCompletion();

        MultitestingEnvironment environment = multitestingService.getEnvironment(
            state.getProject(), state.getMultitestingName());
        Assert.assertEquals(environment.getStatus(), MultitestingEnvironment.Status.ARCHIVED);
        Assert.assertEquals(PerCommitPipeLaunchState.FINISHED, state.getPipeLaunchState());
    }

    @Test
    public void multitestingClosePrevLaunchTest() throws Exception {
        PerCommitBranchStateEntity state = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME)
            .withMultitesting(true)
            .withPipelineId(Config.PIPE_WITH_SLEEP_JOB_ID)
            .withPullRequestId(null)
            .withPullRequestNumber(null)
            .withStatusCheckName(null)
            .build();

        PerCommitSettingsEntity setting = createPerCommitSettings(
            Config.PIPE_WITH_SLEEP_JOB_ID, NewCommitActionType.CLOSE_PREV_LAUNCH, false
        );

        runPerCommitPipeline(setting, "r1", state);
        Thread thread = pipeTester.runScheduledJobsToCompletionAsync();

        semaphore.acquire();

        // После запуска мультитестинга должна работать джоба SimpleSleepyJob
        // Статус мультитестинга DEPLOYING
        String firstMultitestingName = state.getMultitestingName();
        MultitestingEnvironment firstEnvironment = multitestingService.getEnvironment(
            state.getProject(), state.getMultitestingName()
        );
        Assert.assertEquals(firstEnvironment.getStatus(), MultitestingEnvironment.Status.DEPLOYING);

        PerCommitBranchStateHistoryEntity historyState = addBranchStateToQueue(state);

        runPerCommitPipeline(setting, "r2", state);
        thread.join();

        // После попытки запуска нового покоммитного пайплайна джоба SimpleSleepyJob завершится
        // DummyJob не стартует из-за блокировки
        // Статус мультитестинга DEPLOYING
        firstEnvironment = multitestingService.getEnvironment(state.getProject(), firstMultitestingName);
        Assert.assertEquals(firstEnvironment.getStatus(), MultitestingEnvironment.Status.DEPLOYING);

        runPerCommitPipeline(setting, "r2", state);
        // После очередной попытки запуска нового покоммитного пайплайна запустится очистка, так как нет
        // исполняющихся джоб
        // Статус мультитестинга CLEANUP_TO_ARCHIVED
        firstEnvironment = multitestingService.getEnvironment(state.getProject(), firstMultitestingName);
        Assert.assertEquals(firstEnvironment.getStatus(), MultitestingEnvironment.Status.CLEANUP_TO_ARCHIVED);

        pipeTester.runScheduledJobsToCompletion();
        // После очистки статус мультитестинга ARCHIVED
        firstEnvironment = multitestingService.getEnvironment(state.getProject(), firstMultitestingName);
        Assert.assertEquals(firstEnvironment.getStatus(), MultitestingEnvironment.Status.ARCHIVED);

        // После архивирования предыдущего запуска мультитестинга доступен новый запуск
        runPerCommitPipeline(setting, "r2", state);
        pipeTester.runScheduledJobsToCompletion();

        MultitestingEnvironment secondEnvironment = multitestingService.getEnvironment(
            state.getProject(), state.getMultitestingName());
        Assert.assertEquals(secondEnvironment.getStatus(), MultitestingEnvironment.Status.READY);
        Assert.assertEquals(PerCommitPipeLaunchState.FINISHED, historyState.getPipeLaunchState());
    }

    @Test
    public void restartJobsInLaunchTest() {
        PerCommitBranchStateEntity state = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME)
            .withMultitesting(true)
            .withPipelineId(Config.PIPE_WITH_REBUILD_JOB_ID)
            .withPullRequestId(null)
            .withPullRequestNumber(null)
            .withStatusCheckName(null)
            .build();

        PerCommitSettingsEntity setting = createPerCommitSettings(Config.PIPE_WITH_REBUILD_JOB_ID,
            NewCommitActionType.RESTART_LAUNCH, false);
        runPerCommitPipeline(setting, "r1", state);
        pipeTester.runScheduledJobsToCompletion();

        MultitestingEnvironment environment = multitestingService.getEnvironment(
            state.getProject(), state.getMultitestingName());
        Assert.assertEquals(environment.getStatus(), MultitestingEnvironment.Status.READY);

        Assert.assertEquals("r1", getPerCommitLaunchParamsForJob(environment, "build").getCommitSha());

        PerCommitBranchStateHistoryEntity historyState = addBranchStateToQueue(state);

        runPerCommitPipeline(setting, "r2", state);
        pipeTester.runScheduledJobsToCompletion();

        Assert.assertEquals("r2", getPerCommitLaunchParamsForJob(environment, "build").getCommitSha());
        Mockito.verify(stateHistoryDao, Mockito.times(1)).delete(Mockito.eq(historyState));
    }

    @Test
    public void perCommitStatusChecksTest() throws Exception {
        PerCommitBranchStateEntity state = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME)
            .withMultitesting(true)
            .withPipelineId(MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID)
            .withPullRequestId(null)
            .withPullRequestNumber(null)
            .withStatusCheckName(null)
            .build();

        PerCommitSettingsEntity setting = createPerCommitSettings(MultitestingTestConfig.PIPE_WITH_CUSTOM_CLEANUP_ID,
            NewCommitActionType.WAITING_PREV_LAUNCH, true);

        runPerCommitPipeline(setting, "r1", state);
        Mockito.when(stateDao.get(state.getId())).thenReturn(state);
        Mockito.verify(gitHubClient, Mockito.times(1))
            .createStatusChecks(Mockito.eq(REPOSITORY_ID), Mockito.eq("r1"), Mockito.any());

        pipeTester.runScheduledJobsToCompletion();

        Mockito.verify(gitHubClient, Mockito.times(2))
            .createStatusChecks(Mockito.eq(REPOSITORY_ID), Mockito.eq("r1"), Mockito.any());
    }

    @Test
    public void restartPerCommitStatusChecksTest() {
        PerCommitBranchStateEntity state = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME)
            .withMultitesting(true)
            .withPipelineId(Config.PIPE_WITH_REBUILD_JOB_ID)
            .withPullRequestId(null)
            .withPullRequestNumber(null)
            .withStatusCheckName(null)
            .build();

        PerCommitSettingsEntity setting = createPerCommitSettings(Config.PIPE_WITH_REBUILD_JOB_ID,
            NewCommitActionType.RESTART_LAUNCH, true);

        runPerCommitPipeline(setting, "r1", state);
        Mockito.when(stateDao.get(state.getId())).thenReturn(state);
        Mockito.verify(gitHubClient, Mockito.times(1))
            .createStatusChecks(Mockito.eq(REPOSITORY_ID), Mockito.eq("r1"), Mockito.any());

        pipeTester.runScheduledJobsToCompletion();
        Mockito.verify(gitHubClient, Mockito.times(2))
            .createStatusChecks(Mockito.eq(REPOSITORY_ID), Mockito.eq("r1"), Mockito.any());

        PerCommitBranchStateHistoryEntity historyState = addBranchStateToQueue(state);

        runPerCommitPipeline(setting, "r2", state);
        Mockito.verify(gitHubClient, Mockito.times(1))
            .createStatusChecks(Mockito.eq(REPOSITORY_ID), Mockito.eq("r2"), Mockito.any());
        Mockito.verify(stateHistoryDao, Mockito.times(1)).delete(historyState);

        pipeTester.runScheduledJobsToCompletion();
        Mockito.verify(gitHubClient, Mockito.times(2))
            .createStatusChecks(Mockito.eq(REPOSITORY_ID), Mockito.eq("r2"), Mockito.any());

        Mockito.verify(gitHubClient, Mockito.times(4))
            .createStatusChecks(Mockito.any(), Mockito.any(), Mockito.any());
    }

    private PerCommitBranchStateHistoryEntity addBranchStateToQueue(PerCommitBranchStateEntity state) {
        PerCommitBranchStateHistoryEntity historyState = new PerCommitBranchStateHistoryEntity(state);
        perCommitService.addBranchStateToQueue(state, state.getLaunchSetting(),
            PerCommitBranchStateEntity.LOW_PRIORITY);
        Mockito.when(stateHistoryDao.getLastByBranchStateId(Mockito.eq(state.getId())))
            .thenReturn(historyState);
        Mockito.when(stateHistoryDao.getByBranchStateId(Mockito.eq(state.getId())))
            .thenReturn(Collections.singletonList(historyState));

        return historyState;
    }

    private void runPerCommitPipeline(PerCommitSettingsEntity setting, String revisionNumber,
                                      PerCommitBranchStateEntity perCommitBranchStateEntity) {
        RepositoryCommit repositoryCommit = PerCommitServiceTest.commit(revisionNumber);

        PerCommitBranchLaunchSetting launchSetting = new PerCommitBranchLaunchSetting(setting);
        perCommitBranchStateEntity.setLaunchSetting(launchSetting);
        Mockito.when(stateDao.get(Mockito.eq(perCommitBranchStateEntity.getId())))
            .thenReturn(perCommitBranchStateEntity);
        perCommitService.launchPipelineForQueuedState(perCommitBranchStateEntity, repositoryCommit, null, true);
    }

    private PerCommitSettingsEntity createPerCommitSettings(String pipelineId, NewCommitActionType newCommitActionType,
                                                            boolean sendStatusChecks) {
        PerCommitSettingsEntity setting = new PerCommitSettingsEntity();
        setting.setRepository(REPOSITORY_ID);
        setting.setPipeline(pipelineId);
        setting.setNewCommitActionType(newCommitActionType);
        setting.setMultitesting(true);
        setting.setSendChecks(sendStatusChecks);
        setting.setLaunchType(PerCommitLaunchType.PULL_REQUEST_MERGE);
        return setting;
    }

    private PerCommitLaunchParams getPerCommitLaunchParamsForJob(MultitestingEnvironment environment, String jobId) {
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(environment.getLastLaunch().getPipeLaunchId());
        return resourceService.loadResources(
                pipeLaunch.getJobs().get(jobId).getLastLaunch().getConsumedResources())
            .getSingleOfType(PerCommitLaunchParams.class);
    }

    private static class PerCommitExampleJob implements JobExecutor {
        @WiredResource
        private PerCommitLaunchParams params;

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("e5e72725-f25b-45e9-ac9d-8706b6f4d038");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            context.progress().update(progress -> progress.setText(params.toString()));
        }
    }

    private static class SimpleSleepyJob implements JobExecutor {
        @Autowired
        private Semaphore semaphore;
        @WiredResource
        private PerCommitLaunchParams params;

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("c183e920-763d-41b2-bb9a-2e76b6ee7143");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            semaphore.release();

            context.progress().update(progress -> progress.setText("Sleeping"));
            Thread.sleep(TimeUnit.SECONDS.toMillis(3));

            context.progress().update(progress -> progress.setText(params.toString()));
        }
    }

    @Configuration
    @TestPropertySource({"classpath:test.properties"})
    public static class Config {
        public static final String PIPE_WITH_REBUILD_JOB_ID = "mt-rebuild-job-pipe-test";
        public static final String PIPE_WITH_SLEEP_JOB_ID = "mt-sleep-job-pipe-test";

        @Bean
        public PerCommitBranchStateDao perCommitBranchStateDao() {
            return Mockito.mock(PerCommitBranchStateDao.class);
        }

        @Bean
        public PerCommitBranchStateHistoryDao perCommitBranchStateHistoryDao() {
            return Mockito.mock(PerCommitBranchStateHistoryDao.class);
        }

        @Bean
        public Events startrekEvents() {
            return Mockito.mock(Events.class);
        }

        @Bean
        public Comments startrekComments() {
            return Mockito.mock(Comments.class);
        }

        @Bean
        public GitHubClient gitHubClient() {
            return Mockito.mock(GitHubClient.class);
        }

        @Bean
        public Semaphore semaphore() {
            return new Semaphore(0, true);
        }

        @Bean(name = PIPE_WITH_REBUILD_JOB_ID)
        public Pipeline mtRebuildTestPipeline() {
            PipelineBuilder builder = PipelineBuilder.create();
            JobBuilder buildJob = builder.withJob(PerCommitExampleJob.class, "build")
                .withTags(MultitestingTags.BUILD);
            JobBuilder deployJob = builder.withJob(PerCommitExampleJob.class, "deploy");
            JobBuilder cleanupJob = builder.withJob(PerCommitExampleJob.class, "cleanup")
                .withManualTrigger()
                .withTags(MultitestingTags.CLEANUP);
            return builder.build();
        }

        @Bean(name = PIPE_WITH_SLEEP_JOB_ID)
        public Pipeline mtSleepTestPipeline() {
            PipelineBuilder builder = PipelineBuilder.create();
            JobBuilder buildJob = builder.withJob(PerCommitExampleJob.class, "build")
                .withTags(MultitestingTags.BUILD);
            JobBuilder deployJob = builder.withJob(SimpleSleepyJob.class, "deploy");
            JobBuilder job = builder.withJob(DummyJob.class, "job")
                .withUpstreams(deployJob);
            JobBuilder cleanupJob = builder.withJob(PerCommitExampleJob.class, "cleanup")
                .withManualTrigger()
                .withTags(MultitestingTags.CLEANUP);
            return builder.build();
        }
    }
}
