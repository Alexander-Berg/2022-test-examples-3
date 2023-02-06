package ru.yandex.market.tsum.release.merge.strategies;

import java.util.Arrays;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.Repository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.clients.github.model.Branch;
import ru.yandex.market.tsum.clients.github.model.MergeResult;
import ru.yandex.market.tsum.core.notify.common.NotificationCenter;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobProgressContext;
import ru.yandex.market.tsum.pipelines.test_data.TestVersionBuilder;
import ru.yandex.market.tsum.release.GitHubService;
import ru.yandex.market.tsum.release.ReleaseIssueService;
import ru.yandex.market.tsum.release.merge.MergeOptions;
import ru.yandex.market.tsum.release.merge.Merger;
import ru.yandex.market.tsum.release.merge.PullRequestService;
import ru.yandex.market.tsum.release.merge.ReleasePullRequestCreator;
import ru.yandex.market.tsum.release.merge.TargetBranchProvider;
import ru.yandex.market.tsum.release.merge.filters.AcceptAllPullRequestValidator;
import ru.yandex.market.tsum.release.merge.filters.PullRequestValidator;
import ru.yandex.startrek.client.model.Version;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.pipelines.test_data.TestPullRequestFactory.pullRequest;
import static ru.yandex.market.tsum.release.merge.MergerTest.RELEASE_BRANCH_NAME;
import static ru.yandex.market.tsum.release.merge.MergerTest.RELEASE_ISSUE;
import static ru.yandex.market.tsum.release.merge.MergerTest.REPO_FULL_NAME;
import static ru.yandex.market.tsum.release.merge.MergerTest.TARGET_BRANCH_NAME;
import static ru.yandex.market.tsum.release.merge.MergerTest.branch;

/**
 * Тесты проверяющие разные поведение мержа при разных параметрах.
 *
 * @author s-ermakov
 */
public class MergeStrategyTest {
    // TODO: notifications
//    private EventBus eventBus;
//    private EventBusHelper eventBusHelper;
    private Version version;
    private GitHubClient gitHubClient;
    private Merger merger;
    //    private TextResourcesFactory textResourcesFactory;
    private ReleaseIssueService releaseIssueService;
    private PullRequestService pullRequestService;

    private Repository repository;

    private Branch releaseBranch;

    private PullRequest pullRequest1;
    private PullRequest pullRequest2;
    private PullRequest pullRequest3;
    private PullRequest pullRequest4;
    private PullRequest pullRequest5;
    private MergeOptions tryAllMergeOptions;
    private MergeOptions toFirstFailedMergeOptions;
    private NotificationCenter notificationCenter;
    private JobContext jobContext;
    private GitHubService gitHubService;

    @Before
    public void setUp() throws Throwable {
//        eventBus = new EventBus();
//        eventBusHelper = new EventBusHelper(eventBus);
        version = TestVersionBuilder.aVersion().withId(48471L).withName("2016.45").build();

        releaseBranch = branch(RELEASE_BRANCH_NAME);

        gitHubClient = mock(GitHubClient.class);
        when(gitHubClient.createPullRequest(any(), any(Branch.class), any(Branch.class), any(), any()))
            .thenReturn(pullRequest(RELEASE_BRANCH_NAME, TARGET_BRANCH_NAME));
        when(gitHubClient.getBranch(any(), any())).thenReturn(null);

        repository = new Repository();
        repository.setHtmlUrl("https://github.yandex-team.ru/" + REPO_FULL_NAME);
        when(gitHubClient.getRepository(REPO_FULL_NAME)).thenReturn(repository);

//        textResourcesFactory = mock(TextResourcesFactory.class);
        releaseIssueService = mock(ReleaseIssueService.class);
        when(releaseIssueService.getReleaseIssue(version)).thenReturn(RELEASE_ISSUE);

        MergeStrategy tryAllMergeStrategy = new TryAllMergeStrategy(gitHubClient);
        MergeStrategy toFirstFailedMergeStrategy = new ToFirstFailedMergeStrategy(gitHubClient);
        PullRequestValidator allPullRequestValidator = new AcceptAllPullRequestValidator();

        when(gitHubClient.getBranch(REPO_FULL_NAME, TARGET_BRANCH_NAME)).thenReturn(branch(TARGET_BRANCH_NAME));
        when(gitHubClient.createBranch(REPO_FULL_NAME, TARGET_BRANCH_NAME, RELEASE_BRANCH_NAME))
            .thenReturn(releaseBranch);

        tryAllMergeOptions = new MergeOptions(tryAllMergeStrategy, allPullRequestValidator);
        toFirstFailedMergeOptions = new MergeOptions(toFirstFailedMergeStrategy, allPullRequestValidator);

        pullRequestService = mock(PullRequestService.class);
        notificationCenter = mock(NotificationCenter.class);
        JobProgressContext progressContext = mock(JobProgressContext.class);
        jobContext = mock(JobContext.class);
        when(jobContext.progress()).thenReturn(progressContext);
        gitHubService = mock(GitHubService.class);
        when(gitHubService.createReleaseBranch(eq(REPO_FULL_NAME), any(Branch.class), eq(version)))
            .thenReturn(releaseBranch);

        pullRequest1 = pullRequest("feature/feature1", TARGET_BRANCH_NAME);
        pullRequest2 = pullRequest("feature/feature2", TARGET_BRANCH_NAME);
        pullRequest3 = pullRequest("feature/feature3", TARGET_BRANCH_NAME);
        pullRequest4 = pullRequest("feature/feature4", TARGET_BRANCH_NAME);
        pullRequest5 = pullRequest("feature/feature5", TARGET_BRANCH_NAME);

        merger = createMerger();
    }

    @Test
    public void toFirstFailedTest() throws Exception {
        when(pullRequestService.getPullRequests(REPO_FULL_NAME, version)).thenReturn(
            Arrays.asList(pullRequest1, pullRequest2, pullRequest3, pullRequest4, pullRequest5)
        );
        when(gitHubClient.mergePullRequest(eq(REPO_FULL_NAME), eq(pullRequest1.getHead().getRef()),
            eq(RELEASE_BRANCH_NAME), any()))
            .thenReturn(MergeResult.MERGED);
        when(gitHubClient.mergePullRequest(eq(REPO_FULL_NAME), eq(pullRequest2.getHead().getRef()),
            eq(RELEASE_BRANCH_NAME), any()))
            .thenReturn(MergeResult.NOTHING_TO_MERGE);
        when(gitHubClient.mergePullRequest(eq(REPO_FULL_NAME), eq(pullRequest3.getHead().getRef()),
            eq(RELEASE_BRANCH_NAME), any()))
            .thenReturn(MergeResult.CONFLICT);

        Merger.MergeResult result = runMerge(this.toFirstFailedMergeOptions);

        Assert.assertEquals(result.getStatus(), Merger.Status.FAILED);
        verify(gitHubClient, never()).mergePullRequest(eq(REPO_FULL_NAME), eq(pullRequest4.getHead().getRef()),
            eq(TARGET_BRANCH_NAME), any());
        verify(gitHubClient, never()).mergePullRequest(eq(REPO_FULL_NAME), eq(pullRequest5.getHead().getRef()),
            eq(TARGET_BRANCH_NAME), any());

//        MergeFinishedInfo mergeFinishedEvent = eventBusHelper.nextValidate(MergeFinishedInfo.class);
//        PullRequestStatus pullRequestStatus = mergeFinishedEvent.getPullRequestsStatus();
//
//        Assert.assertEquals(Collections.singletonList(pullRequest1), pullRequestStatus.getSuccessfulPullRequests());
//        Assert.assertEquals(Collections.singletonList(pullRequest3), pullRequestStatus.getFailedPullRequests());
    }

    @Test
    public void severalFailedTest() throws Exception {
        when(pullRequestService.getPullRequests(REPO_FULL_NAME, version)).thenReturn(
            Arrays.asList(pullRequest1, pullRequest2, pullRequest3, pullRequest4, pullRequest5)
        );
        when(gitHubClient.mergePullRequest(eq(REPO_FULL_NAME), eq(pullRequest1.getHead().getRef()),
            eq(RELEASE_BRANCH_NAME), any()))
            .thenReturn(MergeResult.MERGED);
        when(gitHubClient.mergePullRequest(eq(REPO_FULL_NAME), eq(pullRequest2.getHead().getRef()),
            eq(RELEASE_BRANCH_NAME), any()))
            .thenReturn(MergeResult.NOTHING_TO_MERGE);
        when(gitHubClient.mergePullRequest(eq(REPO_FULL_NAME), eq(pullRequest3.getHead().getRef()),
            eq(RELEASE_BRANCH_NAME), any()))
            .thenReturn(MergeResult.CONFLICT);
        when(gitHubClient.mergePullRequest(eq(REPO_FULL_NAME), eq(pullRequest4.getHead().getRef()),
            eq(RELEASE_BRANCH_NAME), any()))
            .thenReturn(MergeResult.MERGED);
        when(gitHubClient.mergePullRequest(eq(REPO_FULL_NAME), eq(pullRequest5.getHead().getRef()),
            eq(RELEASE_BRANCH_NAME), any()))
            .thenReturn(MergeResult.CONFLICT);

        MergeOptions mergeOptions = this.tryAllMergeOptions;
        Merger.MergeResult result = runMerge(mergeOptions);

        Assert.assertEquals(result.getStatus(), Merger.Status.FAILED);

//        MergeFinishedInfo mergeFinishedEvent = eventBusHelper.nextValidate(MergeFinishedInfo.class);
//        PullRequestStatus pullRequestStatus = mergeFinishedEvent.getPullRequestsStatus();
//
//        Assert.assertEquals(Arrays.asList(pullRequest1, pullRequest4), pullRequestStatus.getSuccessfulPullRequests());
//        Assert.assertEquals(Arrays.asList(pullRequest3, pullRequest5), pullRequestStatus.getFailedPullRequests());
    }

    private Merger.MergeResult runMerge(MergeOptions mergeOptions) throws InterruptedException {
        return merger.merge(
            jobContext, REPO_FULL_NAME, version, mergeOptions,
            event -> {
            }, event -> {
            }, null
        );
    }

    private Merger createMerger() {
        TargetBranchProvider targetBranchProvider = new TargetBranchProvider(gitHubClient);
        ReleasePullRequestCreator releasePullRequestCreator = new ReleasePullRequestCreator(
            gitHubClient, releaseIssueService
        );

        return new Merger(
            gitHubClient,
            releaseIssueService,
            releasePullRequestCreator,
            targetBranchProvider,
            pullRequestService,
            gitHubService
        );
    }

}
