package ru.yandex.market.tsum.release.merge.filters;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.egit.github.core.Label;
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
import ru.yandex.market.tsum.release.merge.strategies.MergeStrategy;
import ru.yandex.market.tsum.release.merge.strategies.ToFirstFailedMergeStrategy;
import ru.yandex.startrek.client.model.Version;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.pipelines.test_data.TestPullRequestFactory.pullRequest;
import static ru.yandex.market.tsum.release.merge.MergerTest.RELEASE_BRANCH_NAME;
import static ru.yandex.market.tsum.release.merge.MergerTest.RELEASE_ISSUE;
import static ru.yandex.market.tsum.release.merge.MergerTest.REPO_FULL_NAME;
import static ru.yandex.market.tsum.release.merge.MergerTest.TARGET_BRANCH_NAME;
import static ru.yandex.market.tsum.release.merge.MergerTest.branch;

/**
 * Тесты, проверяющие корректность фильтрации пулл реквестов.
 *
 * @author s-ermakov
 */
public class PullRequestValidatorTest {
    // TODO: notifications
//    private EventBus eventBus;
//    private EventBusHelper eventBusHelper;
    private Version version;
    private GitHubClient gitHubClient;
    //    private TextResourcesFactory textResourcesFactory;
    private ReleaseIssueService releaseIssueService;
    private MergeStrategy mergeStrategy;
    private PullRequestService pullRequestService;

    private Repository repository;
    private Branch releaseBranch;

    private PullRequest pullRequest1;
    private PullRequest pullRequest2;
    private PullRequest pullRequest3;
    private PullRequest pullRequest4;
    private PullRequest pullRequest5;
    private JobContext jobContext;
    private NotificationCenter notificationCenter;
    private GitHubService gitHubService; // TODO: 02.08.17 нужно проинициализировать!

    @Before
    public void setUp() throws Throwable {
        version = TestVersionBuilder.aVersion().withId(48471L).withName("2016.45").build();

        releaseBranch = branch(RELEASE_BRANCH_NAME);

        gitHubClient = mock(GitHubClient.class);
        when(gitHubClient.mergePullRequest(any(), any(), any(), any())).thenReturn(MergeResult.MERGED);
        when(gitHubClient.createPullRequest(any(), any(Branch.class), any(Branch.class), any(), any()))
            .thenReturn(pullRequest(RELEASE_BRANCH_NAME, TARGET_BRANCH_NAME));
        when(gitHubClient.getBranch(any(), any())).thenReturn(null);

        repository = new Repository();
        repository.setHtmlUrl("https://github.yandex-team.ru/" + REPO_FULL_NAME);
        when(gitHubClient.getRepository(REPO_FULL_NAME)).thenReturn(repository);

//        textResourcesFactory = mock(TextResourcesFactory.class);
        releaseIssueService = mock(ReleaseIssueService.class);
        when(releaseIssueService.getReleaseIssue(version)).thenReturn(RELEASE_ISSUE);

        mergeStrategy = new ToFirstFailedMergeStrategy(gitHubClient);

        when(gitHubClient.getBranch(REPO_FULL_NAME, TARGET_BRANCH_NAME)).thenReturn(branch(TARGET_BRANCH_NAME));
        when(gitHubClient.createBranch(REPO_FULL_NAME, TARGET_BRANCH_NAME, RELEASE_BRANCH_NAME))
            .thenReturn(releaseBranch);

        pullRequestService = mock(PullRequestService.class);

        pullRequest1 = pullRequest(1, "feature/feature1", TARGET_BRANCH_NAME);
        pullRequest2 = pullRequest(2, "feature/feature2", TARGET_BRANCH_NAME);
        pullRequest3 = pullRequest(3, "feature/feature3", TARGET_BRANCH_NAME);
        pullRequest4 = pullRequest(4, "feature/feature4", TARGET_BRANCH_NAME);
        pullRequest5 = pullRequest(5, "feature/feature5", TARGET_BRANCH_NAME);

        JobProgressContext progressContext = mock(JobProgressContext.class);
        jobContext = mock(JobContext.class);
        when(jobContext.progress()).thenReturn(progressContext);
        notificationCenter = mock(NotificationCenter.class);

        gitHubService = mock(GitHubService.class);
        when(gitHubService.createReleaseBranch(eq(REPO_FULL_NAME), any(Branch.class), eq(version)))
            .thenReturn(releaseBranch);
    }

    @Test
    public void allPassPullRequests() throws Exception {
        when(pullRequestService.getPullRequests(REPO_FULL_NAME, version)).thenReturn(
            Arrays.asList(pullRequest1, pullRequest2, pullRequest3, pullRequest4, pullRequest5)
        );
        when(gitHubClient.getPullRequestLabels(REPO_FULL_NAME, pullRequest1)).thenReturn(getLabels("hotfix", "code " +
            "review"));
        when(gitHubClient.getPullRequestLabels(REPO_FULL_NAME, pullRequest2)).thenReturn(getLabels());
        when(gitHubClient.getPullRequestLabels(REPO_FULL_NAME, pullRequest3)).thenReturn(getLabels("code review ok"));
        when(gitHubClient.getPullRequestLabels(REPO_FULL_NAME, pullRequest4)).thenReturn(getLabels("code review ok",
            "without review"));
        when(gitHubClient.getPullRequestLabels(REPO_FULL_NAME, pullRequest5)).thenReturn(getLabels("needs fixing"));

        MergeOptions mergeOptions = new MergeOptions(mergeStrategy, new AcceptAllPullRequestValidator());
        Merger.MergeResult result = runMerge(mergeOptions);

        Assert.assertEquals(result.getStatus(), Merger.Status.SUCCESSFUL);
    }

    private Merger.MergeResult runMerge(MergeOptions mergeOptions) throws InterruptedException {
        Merger merger = createMerger();
        return merger.merge(jobContext, REPO_FULL_NAME, version, mergeOptions, event -> {
        }, event -> {
        }, null);
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

    @Test
    public void filterLabelsInPullRequests() throws Exception {
        when(pullRequestService.getPullRequests(REPO_FULL_NAME, version)).thenReturn(
            Arrays.asList(pullRequest1, pullRequest2, pullRequest3, pullRequest4, pullRequest5)
        );

        when(gitHubClient.getPullRequestLabels(REPO_FULL_NAME, pullRequest1)).thenReturn(getLabels("hotfix", "code " +
            "review"));
        when(gitHubClient.getPullRequestLabels(REPO_FULL_NAME, pullRequest2)).thenReturn(getLabels());
        when(gitHubClient.getPullRequestLabels(REPO_FULL_NAME, pullRequest3)).thenReturn(getLabels("code review ok"));
        when(gitHubClient.getPullRequestLabels(REPO_FULL_NAME, pullRequest4)).thenReturn(getLabels("code review ok",
            "without review"));
        when(gitHubClient.getPullRequestLabels(REPO_FULL_NAME, pullRequest5)).thenReturn(getLabels("needs fixing"));

        MergeOptions mergeOptions = new MergeOptions(
            mergeStrategy,
            new LabelPullRequestValidator(gitHubClient, Arrays.asList("code review ok", "without review"))
        );

        Merger.MergeResult result = runMerge(mergeOptions);

        Assert.assertEquals(result.getStatus(), Merger.Status.FAILED);

//        MergeFinishedInfo mergeFinishedEvent = eventBusHelper.nextValidate(MergeFinishedInfo.class);
//        PullRequestStatus pullRequestStatus = mergeFinishedEvent.getPullRequestsStatus();

//        Assert.assertEquals(Arrays.asList(pullRequest1, pullRequest2, pullRequest5), pullRequestStatus
//        .getRejectedPullRequests());
//        Assert.assertEquals(Arrays.asList(pullRequest3, pullRequest4), pullRequestStatus.getSuccessfulPullRequests());
    }

    @Test
    public void noAcceptedPullRequests() throws Exception {
        when(pullRequestService.getPullRequests(REPO_FULL_NAME, version)).thenReturn(
            Arrays.asList(pullRequest1, pullRequest2, pullRequest3, pullRequest4, pullRequest5)
        );
        when(gitHubClient.getPullRequestLabels(REPO_FULL_NAME, pullRequest1)).thenReturn(getLabels("hotfix", "code " +
            "review"));
        when(gitHubClient.getPullRequestLabels(REPO_FULL_NAME, pullRequest2)).thenReturn(getLabels());
        when(gitHubClient.getPullRequestLabels(REPO_FULL_NAME, pullRequest3)).thenReturn(getLabels("code review ok"));
        when(gitHubClient.getPullRequestLabels(REPO_FULL_NAME, pullRequest4)).thenReturn(getLabels("code review ok",
            "without review"));
        when(gitHubClient.getPullRequestLabels(REPO_FULL_NAME, pullRequest5)).thenReturn(getLabels("needs fixing"));

        MergeOptions mergeOptions = new MergeOptions(
            mergeStrategy,
            new LabelPullRequestValidator(gitHubClient, Collections.emptyList())
        );

        Merger.MergeResult result = runMerge(mergeOptions);

        Assert.assertEquals(result.getStatus(), Merger.Status.FAILED);

//        MergeFinishedInfo mergeFinishedEvent = eventBusHelper.nextValidate(MergeFinishedInfo.class);
//        PullRequestStatus pullRequestStatus = mergeFinishedEvent.getPullRequestsStatus();

//        Assert.assertEquals(Arrays.asList(pullRequest1, pullRequest2, pullRequest3, pullRequest4, pullRequest5),
//            pullRequestStatus.getRejectedPullRequests());
    }

    private List<Label> getLabels(String... labels) {
        return Stream.of(labels)
            .map(s -> {
                Label label = new Label();
                label.setName(s);
                return label;
            })
            .collect(Collectors.toList());
    }

}
