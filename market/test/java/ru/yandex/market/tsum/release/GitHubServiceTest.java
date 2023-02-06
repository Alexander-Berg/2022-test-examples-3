package ru.yandex.market.tsum.release;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.eclipse.egit.github.core.PullRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.clients.github.model.ExtendedPullRequest;
import ru.yandex.market.tsum.clients.github.model.MergeResult;
import ru.yandex.market.tsum.pipelines.common.jobs.merge.resources.ReleasePullRequest;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;
import ru.yandex.market.tsum.pipelines.test_data.TestPullRequestFactory;
import ru.yandex.market.tsum.release.merge.PullRequestService;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.release.GitHubService.getReleaseBranchName;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 02.06.17
 */
public class GitHubServiceTest {
    private static final Consumer<GitHubService.ReleaseBranchMergedEvent> EMPTY_CONSUMER = event -> {
    };
    private static final String REPO_FULL_NAME = "market/market";
    private static final int RELEASE_PULL_REQUEST_NUMBER = 666;
    private static final String TARGET_BRANCH = "master";
    private static final String RELEASE_BRANCH = "release/market-2016";
    private static final String FEATURE_BRANCH = "feature/feature";

    private GitHubService sut;
    private GitHubClient gitHubClient;
    private PullRequestService pullRequestService;
    private final ReleaseInfo releaseInfo = new ReleaseInfo(null, "TEST-1");

    @Before
    public void setUp() throws Exception {
        gitHubClient = mock(GitHubClient.class);
        pullRequestService = mock(PullRequestService.class);
        sut = new GitHubService(gitHubClient, pullRequestService);
    }

    @Test
    public void mergeBack_ShouldDoNothing_OnSingleRepoWithoutPR() throws Exception {
        ReleasePullRequest releasePullRequest = new ReleasePullRequest(REPO_FULL_NAME, 0);
        sut.mergeReleaseBranches(releaseInfo, Arrays.asList(releasePullRequest), EMPTY_CONSUMER);

        verifyZeroInteractions(gitHubClient);
    }

    @Test
    public void mergeBack_ShouldMergePR_OnSingleRepoWithPR() throws Exception {
        ReleasePullRequest releasePullRequest = new ReleasePullRequest(REPO_FULL_NAME, RELEASE_PULL_REQUEST_NUMBER);

        ExtendedPullRequest pullRequest = TestPullRequestFactory.pullRequest(RELEASE_BRANCH, TARGET_BRANCH);
        when(gitHubClient.getPullRequestExtended(REPO_FULL_NAME, RELEASE_PULL_REQUEST_NUMBER)).thenReturn(pullRequest);
        when(gitHubClient.mergePullRequest(eq(REPO_FULL_NAME), eq(pullRequest), any()))
            .thenReturn(MergeResult.MERGED);

        sut.mergeReleaseBranches(releaseInfo, Arrays.asList(releasePullRequest), EMPTY_CONSUMER);

        verify(gitHubClient).mergePullRequest(eq(REPO_FULL_NAME), eq(pullRequest), any());
    }

    @Test
    public void mergeBack_ShouldNotCommentTicket_OnSingleRepoWithAlreadyMergedPR() throws Exception {
        ReleasePullRequest releasePullRequest = new ReleasePullRequest(REPO_FULL_NAME, RELEASE_PULL_REQUEST_NUMBER);

        ExtendedPullRequest pullRequest = TestPullRequestFactory.pullRequest(RELEASE_BRANCH, TARGET_BRANCH);
        pullRequest.setMerged(true);

        when(gitHubClient.getPullRequestExtended(REPO_FULL_NAME, RELEASE_PULL_REQUEST_NUMBER)).thenReturn(pullRequest);

        AtomicBoolean wasCallbackCalled = new AtomicBoolean();
        sut.mergeReleaseBranches(
            releaseInfo,
            Arrays.asList(releasePullRequest),
            event -> wasCallbackCalled.set(true)
        );

        assertFalse(wasCallbackCalled.get());
    }

    @Test
    public void cleanUpFeatureBranches_ShouldDeleteFeatureBranches() throws Exception {
        int sourceRepoId = 2;
        int targetRepoId = 1;

        PullRequest forkPR = TestPullRequestFactory.pullRequest(
            sourceRepoId, TARGET_BRANCH,
            targetRepoId, TARGET_BRANCH
        );

        PullRequest branchPR = TestPullRequestFactory.pullRequest(FEATURE_BRANCH, TARGET_BRANCH);

        when(gitHubClient.getPullRequest(REPO_FULL_NAME, RELEASE_PULL_REQUEST_NUMBER))
            .thenReturn(TestPullRequestFactory.pullRequest(RELEASE_BRANCH, TARGET_BRANCH));
        when(gitHubClient.isBranchMergedTo(eq(REPO_FULL_NAME), any(), any())).thenReturn(true);

        when(pullRequestService.getPullRequests(eq(REPO_FULL_NAME), any(), any()))
            .thenReturn(Arrays.asList(forkPR, branchPR));

        ReleasePullRequest releasePullRequest = new ReleasePullRequest(REPO_FULL_NAME, RELEASE_PULL_REQUEST_NUMBER);
        sut.cleanUpFeatureBranches(Arrays.asList(releasePullRequest), null);

        verify(gitHubClient, times(1)).deleteBranch(REPO_FULL_NAME, FEATURE_BRANCH);
    }

    @Test
    public void cleanUpFeatureBranches_ShouldIgnoreReposWithoutReleasePRs() throws Exception {
        ReleasePullRequest releasePullRequest = new ReleasePullRequest(REPO_FULL_NAME, 0);
        sut.cleanUpFeatureBranches(Arrays.asList(releasePullRequest), null);

        verify(gitHubClient, never()).getPullRequest(any(), anyInt());
        verify(gitHubClient, never()).deleteBranch(any(), any());
    }

    @Test(expected = IllegalStateException.class)
    public void cleanUpFeatureBranches_ShouldFail_IfBranchNotMerged() throws IOException {
        PullRequest branchPR = TestPullRequestFactory.pullRequest(FEATURE_BRANCH, TARGET_BRANCH);
        when(pullRequestService.getPullRequests(eq(REPO_FULL_NAME), any(), any())).thenReturn(Arrays.asList(branchPR));

        when(gitHubClient.getPullRequest(REPO_FULL_NAME, RELEASE_PULL_REQUEST_NUMBER))
            .thenReturn(TestPullRequestFactory.pullRequest(RELEASE_BRANCH, TARGET_BRANCH));
        when(gitHubClient.isBranchMergedTo(eq(REPO_FULL_NAME), eq(FEATURE_BRANCH), eq(TARGET_BRANCH)))
            .thenReturn(false);

        ReleasePullRequest releasePullRequest = new ReleasePullRequest(REPO_FULL_NAME, RELEASE_PULL_REQUEST_NUMBER);
        sut.cleanUpFeatureBranches(Arrays.asList(releasePullRequest), null);
    }

    @Test
    public void testGetReleaseBranchName() throws Exception {
        Assert.assertEquals(
            "release/2017.1.2_MBI-43234", getReleaseBranchName("2017.1.2_MBI-43234")
        );

        Assert.assertEquals(
            "release/2017.1.2", getReleaseBranchName("2017.1.2 Привет")
        );

        Assert.assertEquals(
            "release/2017.1.2", getReleaseBranchName("2017.1.2\uD83E\uDD8A")
        );

        Assert.assertEquals(
            "release/2017.1.2-666-17", getReleaseBranchName("2017.1.2~666-17: Очень важная версия")
        );

        Assert.assertEquals(
            "release/2017.1.2", getReleaseBranchName("2017.1.2Привет")
        );

        Assert.assertEquals(
            "release/2017.1.2", getReleaseBranchName("2017.1.2привет")
        );

        Assert.assertEquals(
            "release/2017.1.2", getReleaseBranchName("2017.1.2")
        );

        Assert.assertEquals(
            "release/2017.1.2", getReleaseBranchName("2017.1.2 Что не поправлено в версии 2017.1.1")
        );
    }

    @Test(expected = IllegalStateException.class)
    public void illegalFixVersionName() {
        getReleaseBranchName("Привет");
    }

}
