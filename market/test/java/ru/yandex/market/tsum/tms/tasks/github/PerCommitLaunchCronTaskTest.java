package ru.yandex.market.tsum.tms.tasks.github;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.PullRequestMarker;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.annotation.DirtiesContext;
import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.clients.github.model.ExtendedPullRequest;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.PerCommitLaunchType;
import ru.yandex.market.tsum.entity.project.PerCommitSettingsEntity;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.multitesting.MultitestingService;
import ru.yandex.market.tsum.per_commit.PerCommitService;
import ru.yandex.market.tsum.per_commit.dao.PerCommitBranchStateDao;
import ru.yandex.market.tsum.per_commit.dao.PerCommitBranchStateHistoryDao;
import ru.yandex.market.tsum.per_commit.entity.PerCommitBranchLaunchSetting;
import ru.yandex.market.tsum.per_commit.entity.PerCommitBranchStateEntity;
import ru.yandex.market.tsum.per_commit.subscribers.PerCommitStatusCheckResource;
import ru.yandex.market.tsum.per_commit.subscribers.PerCommitSubscriber;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.PipeProvider;
import ru.yandex.market.tsum.pipe.engine.runtime.di.ResourceService;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeStateService;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipelines.test.TestSimplePipeline;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 03.09.18
 */
@RunWith(MockitoJUnitRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PerCommitLaunchCronTaskTest {
    public static final String REPOSITORY_ID = "market-infra/test";
    public static final String PROJECT_ID = "test";
    public static final String BRANCH_NAME = "develop";
    public static final String OTHER_BRANCH_NAME = "other";
    public static final String PIPELINE_ID = "my-pipeline";
    public static final String PIPELINE_ID_2 = "my-pipeline-2";
    public static final String PIPELINE_ID_3 = "my-pipeline-3";
    public static final String JOB_ID = "job";
    public static final String BRANCH_COMMIT_1 = "r1";
    public static final String BRANCH_COMMIT_2 = "r2";
    public static final String OTHER_BRANCH_COMMIT = "r3";
    public static final String MERGE_COMMIT = "m1";

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private PerCommitBranchStateDao stateDao;

    @Mock
    private PerCommitBranchStateHistoryDao stateHistoryDao;

    @Mock
    private PipeLaunchDao pipeLaunchDao;

    @Mock
    private MultitestingService multitestingService;

    @Mock
    private ProjectsDao projectsDao;

    @Mock
    private PipeStateService pipeStateService;

    @Mock
    private PipeProvider pipeProvider;

    @Mock
    private PipeLaunch pipeLaunch;

    @Mock
    private ResourceService resourceService;

    private TestPerCommitService perCommitService;

    private Pipeline pipeline;
    private ProjectEntity projectEntity = new ProjectEntity();
    private PerCommitSettingsEntity settings = new PerCommitSettingsEntity();
    private ExtendedPullRequest pullRequest = new ExtendedPullRequest();

    @Before
    public void setup() {
        settings = new PerCommitSettingsEntity();
        settings.setPattern("(feature/.+)|(develop)");
        settings.setRepository(REPOSITORY_ID);
        settings.setPipeline(PIPELINE_ID);

        projectEntity.setId(PROJECT_ID);
        projectEntity.setPerCommitSettings(Collections.singletonList(settings));

        when(projectsDao.list())
            .thenReturn(Collections.singletonList(projectEntity));

        initGitHubClient();

        when(pipeLaunch.getIdString()).thenReturn("pipeLaunch");

        when(pipeStateService.prepareLaunch(any())).thenReturn(pipeLaunch);

        PipelineBuilder pipelineBuilder = PipelineBuilder.create();
        pipelineBuilder.withJob(TestSimplePipeline.ReleaseInfoProducer.class, JOB_ID);
        pipelineBuilder.withCustomCleanupJob(DummyJob.class);
        pipeline = pipelineBuilder.build();

        when(pipeProvider.get(any())).thenReturn(pipeline);

        perCommitService = new TestPerCommitService(pipeStateService, pipeProvider, stateDao,
            stateHistoryDao, pipeLaunchDao, multitestingService, resourceService, gitHubClient);

        initPullRequest();
    }

    private void initGitHubClient() {
        RepositoryCommit branchCommit1 = commit(BRANCH_COMMIT_1);
        RepositoryCommit branchCommit2 = commit(BRANCH_COMMIT_2);
        List<RepositoryCommit> commits = Arrays.asList(branchCommit1, branchCommit2);
        when(gitHubClient.getCommitIterator(
            eq(REPOSITORY_ID),
            eq(BRANCH_NAME)
        ))
            .thenAnswer((Answer<Iterator<RepositoryCommit>>) invocation -> commits.iterator());

        RepositoryCommit mergeCommit = commit(MERGE_COMMIT);
        when(gitHubClient.getCommit(eq(REPOSITORY_ID), eq(MERGE_COMMIT)))
            .thenReturn(mergeCommit);
        RepositoryCommit headCommit = commit(OTHER_BRANCH_COMMIT);
        when(gitHubClient.getCommit(eq(REPOSITORY_ID), eq(OTHER_BRANCH_COMMIT)))
            .thenReturn(headCommit);
    }

    private void initPullRequest() {
        pullRequest.setMergeCommitSha(MERGE_COMMIT);
        pullRequest.setId(1);
        pullRequest.setNumber(1);
        pullRequest.setTitle("Test");

        PullRequestMarker base = new PullRequestMarker();
        base.setSha(BRANCH_COMMIT_2);
        base.setRef(BRANCH_NAME);
        pullRequest.setBase(base);

        PullRequestMarker head = new PullRequestMarker();
        head.setSha(OTHER_BRANCH_COMMIT);
        head.setRef(OTHER_BRANCH_NAME);
        pullRequest.setHead(head);
    }

    @Test
    public void launchesPipelineForBranchState() {
        PerCommitLaunchCronTask task = new PerCommitLaunchCronTask(
            gitHubClient, stateDao, projectsDao, perCommitService
        );
        PerCommitBranchStateEntity state = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME)
            .withMultitesting(false)
            .withPipelineId(PIPELINE_ID)
            .withStatusCheckName(null)
            .withLaunchSetting(new PerCommitBranchLaunchSetting(settings))
            .build();

        when(stateDao.getWaitingStates()).thenReturn(Collections.singletonList(state));

        task.execute(null);

        verify(pipeStateService).prepareLaunch(Mockito.any());
        verify(stateDao, times(1)).save(Mockito.any());
    }

    @Test
    public void launchesPipelineForPullRequestOnHeadState() {
        launchesPipelineForPullRequestState(PerCommitLaunchType.PULL_REQUEST);
    }

    @Test
    public void launchesPipelineForPullRequestOnMergeState() {
        launchesPipelineForPullRequestState(PerCommitLaunchType.PULL_REQUEST_MERGE);
    }

    private void launchesPipelineForPullRequestState(PerCommitLaunchType launchType) {
        settings.setLaunchType(launchType);
        settings.setSendChecks(true);

        PerCommitLaunchCronTask task = new PerCommitLaunchCronTask(
            gitHubClient, stateDao, projectsDao, perCommitService
        );
        PerCommitBranchStateEntity state = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(OTHER_BRANCH_NAME)
            .withMultitesting(false)
            .withPipelineId(PIPELINE_ID)
            .withPullRequestId(1)
            .withPullRequestNumber(1)
            .withStatusCheckName(null)
            .withLaunchSetting(new PerCommitBranchLaunchSetting(settings))
            .build();

        when(stateDao.getWaitingStates()).thenReturn(Collections.singletonList(state));
        when(gitHubClient.getPullRequestExtended(eq(REPOSITORY_ID), eq(1)))
            .thenReturn(pullRequest);

        task.execute(null);

        verify(pipeStateService).prepareLaunch(Mockito.any());
        verify(stateDao, times(1)).save(Mockito.any());

        PerCommitStatusCheckResource statusCheckResource =
            (PerCommitStatusCheckResource) perCommitService.getPipeForLaunch().getSubscribers().stream()
                .filter(subscriber -> subscriber.getClazz() == PerCommitSubscriber.class)
                .findFirst().orElseThrow(RuntimeException::new)
                .getResources().stream()
                .filter(resource -> resource instanceof PerCommitStatusCheckResource)
                .findFirst().orElseThrow(RuntimeException::new);
        Assert.assertEquals(OTHER_BRANCH_COMMIT, statusCheckResource.getCommitSha());
    }

    @Test
    public void launchesPipelineWithQueueLimit() {
        projectEntity.setPerCommitLaunchesLimit(1);

        PerCommitBranchLaunchSetting setting1 = new PerCommitBranchLaunchSetting(settings);
        PerCommitBranchStateEntity state1 = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME)
            .withMultitesting(false)
            .withPipelineId(PIPELINE_ID)
            .withStatusCheckName(null)
            .withLaunchSetting(setting1)
            .withQueueWeight(PerCommitBranchStateEntity.MEDIUM_PRIORITY)
            .build();
        state1.setCreatedTime(1);

        PerCommitBranchLaunchSetting setting2 = new PerCommitBranchLaunchSetting(settings);
        setting2.setPipelineId(PIPELINE_ID_2);
        PerCommitBranchStateEntity state2 = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME)
            .withMultitesting(false)
            .withPipelineId(PIPELINE_ID_2)
            .withStatusCheckName(null)
            .withLaunchSetting(setting2)
            .withQueueWeight(PerCommitBranchStateEntity.MEDIUM_PRIORITY)
            .build();
        state2.setCreatedTime(10);

        PerCommitBranchLaunchSetting setting3 = new PerCommitBranchLaunchSetting(settings);
        setting3.setPipelineId(PIPELINE_ID_3);
        PerCommitBranchStateEntity state3 = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME)
            .withMultitesting(false)
            .withPipelineId(PIPELINE_ID_3)
            .withStatusCheckName(null)
            .withLaunchSetting(setting3)
            .withQueueWeight(PerCommitBranchStateEntity.LOW_PRIORITY)
            .build();
        state3.setCreatedTime(1);

        when(stateDao.getWaitingStates()).thenReturn(Arrays.asList(state3, state2, state1));

        PerCommitLaunchCronTask task = new PerCommitLaunchCronTask(
            gitHubClient, stateDao, projectsDao, perCommitService
        );

        task.execute(null);
        verify(stateDao, times(1)).save(Mockito.any());
        String lastPipeLaunchId = perCommitService.getSetting().getPipelineId();
        Assert.assertEquals(PIPELINE_ID, lastPipeLaunchId);

        when(stateDao.getWaitingStates()).thenReturn(Arrays.asList(state3, state2));
        task.execute(null);
        verify(stateDao, times(2)).save(Mockito.any());
        lastPipeLaunchId = perCommitService.getSetting().getPipelineId();
        Assert.assertEquals(PIPELINE_ID_2, lastPipeLaunchId);

        when(stateDao.getWaitingStates()).thenReturn(Collections.singletonList(state3));
        task.execute(null);
        verify(stateDao, times(3)).save(Mockito.any());
        lastPipeLaunchId = perCommitService.getSetting().getPipelineId();
        Assert.assertEquals(PIPELINE_ID_3, lastPipeLaunchId);
    }

    @Test
    public void notLaunchForQueueLimit() {
        projectEntity.setPerCommitLaunchesLimit(0);

        PerCommitLaunchCronTask task = new PerCommitLaunchCronTask(
            gitHubClient, stateDao, projectsDao, perCommitService
        );
        PerCommitBranchStateEntity state = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME)
            .withMultitesting(false)
            .withPipelineId(PIPELINE_ID)
            .withStatusCheckName(null)
            .withLaunchSetting(new PerCommitBranchLaunchSetting(settings))
            .build();

        when(stateDao.getWaitingStates()).thenReturn(Collections.singletonList(state));

        task.execute(null);

        verify(pipeStateService, Mockito.never()).prepareLaunch(Mockito.any());
        verify(stateDao, Mockito.never()).save(Mockito.any());
    }

    @Test
    public void testOrderForPipeLaunch() {
        settings.setLaunchType(PerCommitLaunchType.PULL_REQUEST_MERGE);
        settings.setSendChecks(true);

        PerCommitLaunchCronTask task = new PerCommitLaunchCronTask(
            gitHubClient, stateDao, projectsDao, perCommitService
        );
        PerCommitBranchStateEntity state = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(OTHER_BRANCH_NAME)
            .withMultitesting(false)
            .withPipelineId(PIPELINE_ID)
            .withPullRequestId(1)
            .withPullRequestNumber(1)
            .withStatusCheckName(null)
            .withLaunchSetting(new PerCommitBranchLaunchSetting(settings))
            .build();

        when(stateDao.getWaitingStates()).thenReturn(Collections.singletonList(state));
        when(gitHubClient.getPullRequestExtended(eq(REPOSITORY_ID), eq(1)))
            .thenReturn(pullRequest);

        task.execute(null);

        verify(pipeStateService).prepareLaunch(Mockito.any());
        verify(stateDao, times(1)).save(Mockito.any());

        PerCommitStatusCheckResource statusCheckResource =
            (PerCommitStatusCheckResource) perCommitService.getPipeForLaunch().getSubscribers().stream()
                .filter(subscriber -> subscriber.getClazz() == PerCommitSubscriber.class)
                .findFirst().orElseThrow(RuntimeException::new)
                .getResources().stream()
                .filter(resource -> resource instanceof PerCommitStatusCheckResource)
                .findFirst().orElseThrow(RuntimeException::new);
        Assert.assertEquals(OTHER_BRANCH_COMMIT, statusCheckResource.getCommitSha());
    }

    @Test
    public void testLaunchForQueueLimit() {
        projectEntity.setPerCommitLaunchesLimit(2);
        when(stateDao.getCountLaunchesByProjectId(Mockito.any())).thenReturn(1L);

        PerCommitBranchLaunchSetting setting1 = new PerCommitBranchLaunchSetting(settings);
        PerCommitBranchStateEntity state1 = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME)
            .withMultitesting(false)
            .withPipelineId(PIPELINE_ID)
            .withStatusCheckName(null)
            .withLaunchSetting(setting1)
            .withQueueWeight(PerCommitBranchStateEntity.MEDIUM_PRIORITY)
            .build();
        state1.setCreatedTime(1);

        PerCommitBranchLaunchSetting setting2 = new PerCommitBranchLaunchSetting(settings);
        setting2.setPipelineId(PIPELINE_ID_2);
        PerCommitBranchStateEntity state2 = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME)
            .withMultitesting(false)
            .withPipelineId(PIPELINE_ID_2)
            .withStatusCheckName(null)
            .withLaunchSetting(setting2)
            .withQueueWeight(PerCommitBranchStateEntity.MEDIUM_PRIORITY)
            .build();
        state2.setCreatedTime(10);

        when(stateDao.getWaitingStates()).thenReturn(Arrays.asList(state2, state1));

        PerCommitLaunchCronTask task = new PerCommitLaunchCronTask(
            gitHubClient, stateDao, projectsDao, perCommitService
        );

        task.execute(null);
        verify(pipeStateService, times(1)).prepareLaunch(Mockito.any());
        verify(stateDao, times(1)).save(Mockito.any());
        String lastPipeLaunchId = perCommitService.getSetting().getPipelineId();
        Assert.assertEquals(PIPELINE_ID, lastPipeLaunchId);
    }

    private static RepositoryCommit commit(String revision) {
        RepositoryCommit repositoryCommit = Mockito.mock(RepositoryCommit.class);
        Mockito.when(repositoryCommit.getSha()).thenReturn(revision);

        Commit commit = Mockito.mock(Commit.class);
        Mockito.when(commit.getMessage()).thenReturn("");
        Mockito.when(repositoryCommit.getCommit()).thenReturn(commit);

        return repositoryCommit;
    }

    private static class TestPerCommitService extends PerCommitService {
        private Pipeline pipeForLaunch = null;
        private PerCommitBranchLaunchSetting setting = null;

        public TestPerCommitService(PipeStateService pipeStateService, PipeProvider pipeProvider,
                                    PerCommitBranchStateDao stateDao, PerCommitBranchStateHistoryDao stateHistoryDao,
                                    PipeLaunchDao pipeLaunchDao, MultitestingService multitestingService,
                                    ResourceService resourceService, GitHubClient gitHubClient) {
            super(pipeStateService, pipeProvider, stateDao, stateHistoryDao, pipeLaunchDao, multitestingService,
                resourceService, gitHubClient);
        }


        public Pipeline getPipeForLaunch() {
            return pipeForLaunch;
        }

        public PerCommitBranchLaunchSetting getSetting() {
            return setting;
        }

        @Override
        protected Pipeline getPerCommitPipeline(PerCommitBranchLaunchSetting setting, PerCommitResourceContainer resourceContainer) {
            this.setting = setting;
            this.pipeForLaunch = super.getPerCommitPipeline(setting, resourceContainer);
            return pipeForLaunch;
        }
    }
}
