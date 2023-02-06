package ru.yandex.market.tsum.percommit;

import java.util.Collections;
import java.util.Date;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitUser;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.config.PipelineScanConfiguration;
import ru.yandex.market.tsum.entity.project.NewCommitActionType;
import ru.yandex.market.tsum.entity.project.PerCommitLaunchType;
import ru.yandex.market.tsum.entity.project.PerCommitSettingsEntity;
import ru.yandex.market.tsum.multitesting.MultitestingService;
import ru.yandex.market.tsum.per_commit.PerCommitService;
import ru.yandex.market.tsum.per_commit.dao.PerCommitBranchStateDao;
import ru.yandex.market.tsum.per_commit.dao.PerCommitBranchStateHistoryDao;
import ru.yandex.market.tsum.per_commit.entity.PerCommitBranchLaunchSetting;
import ru.yandex.market.tsum.per_commit.entity.PerCommitBranchStateEntity;
import ru.yandex.market.tsum.per_commit.entity.PerCommitBranchStateHistoryEntity;
import ru.yandex.market.tsum.per_commit.entity.PerCommitPipeLaunchState;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipelines.test.TestSimplePipeline;

/**
 * @author Mishunin Andrei <a href="mailto:mishunin@yandex-team.ru"></a>
 * @date 15.05.2019
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    PipeServicesConfig.class,
    PerCommitServiceTest.Config.class,
    PerCommitService.class,
    TestConfig.class,
    PipelineScanConfiguration.class,
    MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PerCommitServiceTest {
    public static final String REPOSITORY_ID = "market-infra/test";
    public static final String PROJECT_ID = "test";
    public static final String BRANCH_NAME = "develop";

    @Autowired
    private PerCommitService perCommitService;
    @Autowired
    private PipeTester pipeTester;
    @Autowired
    private PipeLaunchDao pipeLaunchDao;
    @Autowired
    private PerCommitBranchStateDao stateDao;
    @Autowired
    private PerCommitBranchStateHistoryDao stateHistoryDao;
    @Autowired
    private GitHubClient gitHubClient;

    @Test
    public void runPerCommitPipelineToTheEnd() throws Exception {
        PerCommitBranchStateEntity state = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME)
            .withMultitesting(false)
            .withPipelineId(TestSimplePipeline.SIMPLE_RELEASE_PIPE_ID)
            .withPullRequestId(null)
            .withPullRequestNumber(null)
            .withStatusCheckName(null)
            .build();

        PerCommitSettingsEntity setting = createPerCommitSettings(
            TestSimplePipeline.SIMPLE_RELEASE_PIPE_ID, NewCommitActionType.WAITING_PREV_LAUNCH);
        runPerCommitPipeline(setting, "r1", state);

        PipeLaunch pipeLaunch = pipeLaunchDao.getById(state.getPipeLaunch());
        Assert.assertTrue(pipeLaunch.isDisabled());
        Assert.assertEquals(PerCommitPipeLaunchState.FINISHED, state.getPipeLaunchState());
        Mockito.verify(gitHubClient, Mockito.never())
            .createStatusChecks(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void perCommitWaitingPrevLaunchTest() throws Exception {
        PerCommitBranchStateEntity state = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME)
            .withMultitesting(false)
            .withPipelineId(TestSimplePipeline.SIMPLE_PIPE_WITH_MANUAL_TRIGGER_ID)
            .withPullRequestId(null)
            .withPullRequestNumber(null)
            .withStatusCheckName(null)
            .build();

        PerCommitSettingsEntity setting = createPerCommitSettings(
            TestSimplePipeline.SIMPLE_PIPE_WITH_MANUAL_TRIGGER_ID, NewCommitActionType.WAITING_PREV_LAUNCH);
        runPerCommitPipeline(setting, "r1", state);

        String firstLaunchId = state.getPipeLaunch();
        Assert.assertFalse(pipeLaunchDao.getById(firstLaunchId).isDisabled());

        PerCommitBranchStateHistoryEntity historyState = addBranchStateToQueue(state);

        runPerCommitPipeline(setting, "r2", state);

        Assert.assertFalse(pipeLaunchDao.getById(firstLaunchId).isDisabled());
        Assert.assertTrue(state.isQueued());
        Assert.assertTrue(historyState.isInProgress());
    }

    @Test
    public void perCommitClosePrevLaunchTest() throws Exception {
        PerCommitBranchStateEntity state = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME)
            .withMultitesting(false)
            .withPipelineId(TestSimplePipeline.SIMPLE_PIPE_WITH_MANUAL_TRIGGER_ID)
            .withPullRequestId(null)
            .withPullRequestNumber(null)
            .withStatusCheckName(null)
            .build();

        PerCommitSettingsEntity setting = createPerCommitSettings(
            TestSimplePipeline.SIMPLE_PIPE_WITH_MANUAL_TRIGGER_ID, NewCommitActionType.CLOSE_PREV_LAUNCH);
        runPerCommitPipeline(setting, "r1", state);

        String firstLaunchId = state.getPipeLaunch();
        Assert.assertFalse(pipeLaunchDao.getById(firstLaunchId).isDisabled());

        PerCommitBranchStateHistoryEntity historyState = addBranchStateToQueue(state);

        runPerCommitPipeline(setting, "r2", state);

        Assert.assertTrue(pipeLaunchDao.getById(firstLaunchId).isDisabled());
        Assert.assertEquals(PerCommitPipeLaunchState.FINISHED, historyState.getPipeLaunchState());
    }

    @Test
    public void perCommitStatusChecksTest() throws Exception {
        PerCommitBranchStateEntity perCommitBranchStateEntity = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME)
            .withMultitesting(false)
            .withPipelineId(TestSimplePipeline.SIMPLE_RELEASE_PIPE_ID)
            .withPullRequestId(null)
            .withPullRequestNumber(null)
            .withStatusCheckName(null)
            .build();

        PerCommitSettingsEntity setting = createPerCommitSettings(
            TestSimplePipeline.SIMPLE_RELEASE_PIPE_ID, NewCommitActionType.WAITING_PREV_LAUNCH);
        setting.setSendChecks(true);
        RepositoryCommit repositoryCommit = commit("r1");

        PerCommitBranchLaunchSetting launchSetting = new PerCommitBranchLaunchSetting(setting);
        perCommitBranchStateEntity.setLaunchSetting(launchSetting);
        perCommitService.launchPipelineForQueuedState(perCommitBranchStateEntity, repositoryCommit, null, true);
        Mockito.when(stateDao.get(perCommitBranchStateEntity.getId())).thenReturn(perCommitBranchStateEntity);
        Mockito.verify(gitHubClient, Mockito.times(1))
            .createStatusChecks(Mockito.eq(REPOSITORY_ID), Mockito.eq("r1"), Mockito.any());

        pipeTester.runScheduledJobsToCompletion();
        Mockito.verify(gitHubClient, Mockito.times(2))
            .createStatusChecks(Mockito.eq(REPOSITORY_ID), Mockito.eq("r1"), Mockito.any());
    }

    @Test
    public void perCommitWaitingFinishJobTest() throws Exception {
        PerCommitBranchStateEntity state = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME)
            .withMultitesting(false)
            .withPipelineId(TestSimplePipeline.SIMPLE_PIPE_WITH_FINAL_JOB_ID)
            .withPullRequestId(null)
            .withPullRequestNumber(null)
            .withStatusCheckName(null)
            .build();

        PerCommitSettingsEntity setting = createPerCommitSettings(
            TestSimplePipeline.SIMPLE_PIPE_WITH_FINAL_JOB_ID, NewCommitActionType.WAITING_FINISH_JOBS);
        runPerCommitPipeline(setting, "r1", state);

        String firstLaunchId = state.getPipeLaunch();
        Assert.assertFalse(pipeLaunchDao.getById(firstLaunchId).isDisabled());

        PerCommitBranchStateHistoryEntity historyState = addBranchStateToQueue(state);

        runPerCommitPipeline(setting, "r2", state);

        Assert.assertFalse(pipeLaunchDao.getById(firstLaunchId).isDisabled());
        Assert.assertEquals(PerCommitPipeLaunchState.FINISHED, state.getPipeLaunchState());
        Assert.assertFalse(pipeLaunchDao.getById(state.getPipeLaunch()).isDisabled());
        Assert.assertEquals(PerCommitPipeLaunchState.FINISHED, historyState.getPipeLaunchState());
    }

    private PerCommitSettingsEntity createPerCommitSettings(String pipelineId,
                                                            NewCommitActionType newCommitActionType) {
        PerCommitSettingsEntity setting = new PerCommitSettingsEntity();
        setting.setPipeline(pipelineId);
        setting.setRepository(REPOSITORY_ID);
        setting.setLaunchType(PerCommitLaunchType.PER_COMMIT);
        setting.setNewCommitActionType(newCommitActionType);
        setting.setMultitesting(false);
        return setting;
    }

    private void runPerCommitPipeline(PerCommitSettingsEntity setting, String revisionNumber,
                                      PerCommitBranchStateEntity perCommitBranchStateEntity) {
        RepositoryCommit repositoryCommit = commit(revisionNumber);

        PerCommitBranchLaunchSetting launchSetting = new PerCommitBranchLaunchSetting(setting);
        perCommitBranchStateEntity.setLaunchSetting(launchSetting);

        Mockito.when(stateDao.get(Mockito.eq(perCommitBranchStateEntity.getId())))
            .thenReturn(perCommitBranchStateEntity);
        perCommitService.launchPipelineForQueuedState(perCommitBranchStateEntity, repositoryCommit, null, true);
        pipeTester.runScheduledJobsToCompletion();
    }

    public static RepositoryCommit commit(String revision) {
        RepositoryCommit repositoryCommit = Mockito.mock(RepositoryCommit.class);
        Mockito.when(repositoryCommit.getSha()).thenReturn(revision);

        Commit commit = Mockito.mock(Commit.class);
        Mockito.when(commit.getMessage()).thenReturn("");

        CommitUser commitUser = Mockito.mock(CommitUser.class);
        Mockito.when(commitUser.getDate()).thenReturn(new Date());
        Mockito.when(commit.getCommitter()).thenReturn(commitUser);
        Mockito.when(repositoryCommit.getCommit()).thenReturn(commit);

        return repositoryCommit;
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

    @Configuration
    public static class Config {
        @Bean
        public MultitestingService multitestingService() {
            return Mockito.mock(MultitestingService.class);
        }

        @Bean
        public PerCommitBranchStateDao perCommitBranchStateDao() {
            return Mockito.mock(PerCommitBranchStateDao.class);
        }

        @Bean
        public PerCommitBranchStateHistoryDao perCommitBranchStateHistoryDao() {
            return Mockito.mock(PerCommitBranchStateHistoryDao.class);
        }

        @Bean
        public GitHubClient gitHubClient() {
            return Mockito.mock(GitHubClient.class);
        }
    }
}
