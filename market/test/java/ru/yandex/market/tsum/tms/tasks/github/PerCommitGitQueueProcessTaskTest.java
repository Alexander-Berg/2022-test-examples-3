package ru.yandex.market.tsum.tms.tasks.github;

import org.eclipse.egit.github.core.PullRequestMarker;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.service.IssueService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.clients.github.model.Branch;
import ru.yandex.market.tsum.clients.github.model.ExtendedPullRequest;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.PerCommitLaunchType;
import ru.yandex.market.tsum.entity.project.PerCommitSettingsEntity;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.multitesting.MultitestingService;
import ru.yandex.market.tsum.per_commit.PerCommitService;
import ru.yandex.market.tsum.per_commit.dao.PerCommitBranchStateDao;
import ru.yandex.market.tsum.per_commit.dao.PerCommitBranchStateHistoryDao;
import ru.yandex.market.tsum.per_commit.entity.PerCommitBranchStateEntity;
import ru.yandex.market.tsum.per_commit.entity.PerCommitPipeLaunchState;
import ru.yandex.market.tsum.pipe.engine.runtime.PipeProvider;
import ru.yandex.market.tsum.pipe.engine.runtime.di.ResourceService;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeStateService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Mishunin Andrei <a href="mailto:mishunin@yandex-team.ru"></a>
 * @date 15.10.2019
 */
@RunWith(MockitoJUnitRunner.class)
public class PerCommitGitQueueProcessTaskTest {
    public static final String REPOSITORY_ID = "market-infra/test";
    public static final String PROJECT_ID = "test";
    public static final String BRANCH_NAME = "develop";
    public static final String OTHER_BRANCH_NAME = "other";
    public static final String PIPELINE_ID = "my-pipeline";
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
    private ResourceService resourceService;

    @InjectMocks
    private PerCommitService perCommitService;

    private PerCommitSettingsEntity settings = new PerCommitSettingsEntity();

    @Before
    public void setup() {
        settings = new PerCommitSettingsEntity();
        settings.setPattern("(feature/.+)|(develop)");
        settings.setRepository(REPOSITORY_ID);
        settings.setPipeline(PIPELINE_ID);

        ProjectEntity projectEntity = new ProjectEntity();
        projectEntity.setId(PROJECT_ID);
        projectEntity.setPerCommitSettings(Collections.singletonList(settings));

        when(projectsDao.list())
            .thenReturn(Collections.singletonList(projectEntity));

        initGitHubClient();
    }

    private void initGitHubClient() {
        when(gitHubClient.getBranches(eq(REPOSITORY_ID)))
            .thenReturn(Collections.singletonList(new Branch(BRANCH_NAME)));

        RepositoryCommit branchCommit1 = commit(BRANCH_COMMIT_1);
        RepositoryCommit branchCommit2 = commit(BRANCH_COMMIT_2);
        List<RepositoryCommit> commits = Arrays.asList(branchCommit1, branchCommit2);
        when(gitHubClient.getCommitIterator(
            eq(REPOSITORY_ID),
            eq(BRANCH_NAME)
        ))
            .thenReturn(commits.iterator());
    }

    @Test
    public void addStateForBranchToQueueOneTime() {
        PerCommitGitQueueProcessTask task = new PerCommitGitQueueProcessTask(
            stateDao, perCommitService, gitHubClient, projectsDao
        );

        task.execute(null);

        verify(stateDao, times(1)).save(Mockito.any());
    }

    @Test
    public void updateSecondTimeWhenStateInQueue() {
        PerCommitGitQueueProcessTask task = new PerCommitGitQueueProcessTask(
            stateDao, perCommitService, gitHubClient, projectsDao
        );

        task.execute(null);

        verify(stateDao, times(1)).save(Mockito.any());

        PerCommitBranchStateEntity state = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME)
            .withMultitesting(false)
            .withPipelineId(PIPELINE_ID)
            .withPullRequestId(null)
            .withPullRequestNumber(null)
            .withStatusCheckName(null)
            .build();

        when(stateDao.get(eq(PerCommitBranchStateEntity.generateId(PROJECT_ID, REPOSITORY_ID,
            BRANCH_NAME, PIPELINE_ID, null))))
            .thenReturn(state);

        task.execute(null);

        verify(stateDao, times(1)).save(Mockito.eq(state));
    }

    @Test
    public void addSecondTimeWhenStateInNotQueue() {
        PerCommitGitQueueProcessTask task = new PerCommitGitQueueProcessTask(
            stateDao, perCommitService, gitHubClient, projectsDao
        );

        task.execute(null);

        verify(stateDao, times(1)).save(Mockito.any());

        PerCommitBranchStateEntity state = PerCommitBranchStateEntity.builder()
            .withProject(PROJECT_ID)
            .withLaunchSetting(null)
            .withRepository(REPOSITORY_ID)
            .withBranch(BRANCH_NAME)
            .withMultitesting(false)
            .withPipelineId(PIPELINE_ID)
            .withPullRequestId(null)
            .withPullRequestNumber(null)
            .withStatusCheckName(null)
            .withPipeLaunchState(PerCommitPipeLaunchState.IN_PROGRESS)
            .build();

        when(stateDao.get(eq(PerCommitBranchStateEntity.generateId(PROJECT_ID, REPOSITORY_ID,
            BRANCH_NAME, PIPELINE_ID, null))))
            .thenReturn(state);

        task.execute(null);

        verify(stateDao, times(2)).save(Mockito.any());
        verify(stateHistoryDao, times(1)).save(Mockito.any());
    }

    @Test
    public void launchForPullRequest() {
        launchForPullRequestWithType(PerCommitLaunchType.PULL_REQUEST);

        verify(stateDao, times(1)).save(Mockito.any());
    }

    @Test
    public void launchForPullRequestMerge() {
        launchForPullRequestWithType(PerCommitLaunchType.PULL_REQUEST_MERGE);

        verify(stateDao, times(1)).save(Mockito.any());
    }

    private void launchForPullRequestWithType(PerCommitLaunchType launchType) {
        initPullRequest();

        settings.setLaunchType(launchType);
        settings.setSendChecks(true);

        PerCommitGitQueueProcessTask task = new PerCommitGitQueueProcessTask(
            stateDao, perCommitService, gitHubClient, projectsDao
        );

        task.execute(null);
    }

    private void initPullRequest() {
        ExtendedPullRequest pullRequest = new ExtendedPullRequest();
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

        RepositoryCommit mergeCommit = commit(MERGE_COMMIT);
        when(gitHubClient.getCommit(eq(REPOSITORY_ID), eq(MERGE_COMMIT)))
            .thenReturn(mergeCommit);
        when(gitHubClient.getCommit(eq(REPOSITORY_ID), eq(OTHER_BRANCH_COMMIT)))
            .thenReturn(mergeCommit);

        when(gitHubClient.getPullRequestsExtended(eq(REPOSITORY_ID), eq(IssueService.STATE_OPEN)))
            .thenReturn(Collections.singletonList(pullRequest));
    }

    private static RepositoryCommit commit(String revision) {
        RepositoryCommit repositoryCommit = Mockito.mock(RepositoryCommit.class);
        Mockito.when(repositoryCommit.getSha()).thenReturn(revision);

        return repositoryCommit;
    }

}
