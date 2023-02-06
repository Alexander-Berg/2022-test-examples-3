package ru.yandex.market.tsum.release.merge;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.PullRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.clients.github.exceptions.PullRequestAlreadyExistsException;
import ru.yandex.market.tsum.clients.github.model.Branch;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipelines.test_data.TestVersionBuilder;
import ru.yandex.market.tsum.release.ReleaseIssueService;
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
import static ru.yandex.market.tsum.release.merge.MergerTest.VERSION;
import static ru.yandex.market.tsum.release.merge.MergerTest.branch;

/**
 * Тестирование создания релизного пулл реквеста.
 *
 * @author s-ermakov
 */
public class ReleasePullRequestCreatorTest {
    private static final String RELEASE_PULL_REQUEST_LABEL = "release";

    private Version version;
    private GitHubClient gitHubClient;
    private ReleaseIssueService releaseIssueService;
    private ReleasePullRequestCreator releasePullRequestCreator;
    private JobContext jobContext;

    @Before
    public void setUp() throws Exception {
        gitHubClient = mock(GitHubClient.class);
        releaseIssueService = mock(ReleaseIssueService.class);

        version = TestVersionBuilder.aVersion().withId(48471L).withName(VERSION).build();

        when(gitHubClient.createPullRequest(any(), any(Branch.class), any(Branch.class), any(), any()))
            .thenReturn(pullRequest(RELEASE_BRANCH_NAME, TARGET_BRANCH_NAME));
        when(gitHubClient.getBranch(any(), any())).thenReturn(null);

        when(releaseIssueService.getReleaseIssue(version)).thenReturn(RELEASE_ISSUE);

        jobContext = mock(JobContext.class);

        releasePullRequestCreator = new ReleasePullRequestCreator(
            gitHubClient, releaseIssueService
        );
    }

    @Test
    public void createPullRequestToTargetBranch() throws Exception {
        Branch targetBranch = branch(TARGET_BRANCH_NAME);
        Branch releaseBranch = branch(RELEASE_BRANCH_NAME);

        PullRequest releasePullRequest = pullRequest(RELEASE_BRANCH_NAME, TARGET_BRANCH_NAME);

        when(gitHubClient.createPullRequest(any(), any(Branch.class), any(Branch.class), any(), any()))
            .thenReturn(releasePullRequest);
        when(gitHubClient.getPullRequestLabels(any(), any())).thenReturn(Collections.emptyList());

        PullRequest pullRequest = releasePullRequestCreator.createReleasePullRequest(
            jobContext, REPO_FULL_NAME, targetBranch, releaseBranch, version, event -> {
            }
        );

        Assert.assertEquals(releasePullRequest, pullRequest);

        verify(gitHubClient).addLabelsToPullRequest(
            eq(REPO_FULL_NAME),
            eq(releasePullRequest),
            eq(Collections.singletonList(RELEASE_PULL_REQUEST_LABEL))
        );
    }

    @Test
    public void getCreatedPullRequestToTargetBranch() throws Exception {
        Branch targetBranch = branch(TARGET_BRANCH_NAME);
        Branch releaseBranch = branch(RELEASE_BRANCH_NAME);

        PullRequest releasePullRequest = pullRequest(RELEASE_BRANCH_NAME, TARGET_BRANCH_NAME);

        when(gitHubClient.createPullRequest(any(), any(Branch.class), any(Branch.class), any(), any()))
            .thenThrow(new PullRequestAlreadyExistsException());

        when(gitHubClient.getPullRequestLabels(any(), any())).thenReturn(Collections.emptyList());
        when(gitHubClient.getOpenPullRequest(any(), any(), any())).thenReturn(releasePullRequest);

        PullRequest pullRequest = releasePullRequestCreator.createReleasePullRequest(
            jobContext, REPO_FULL_NAME, targetBranch, releaseBranch, version, event -> {
            }
        );

        Assert.assertEquals(releasePullRequest, pullRequest);

        verify(gitHubClient).createPullRequest(
            eq(REPO_FULL_NAME),
            eq(releaseBranch),
            eq(targetBranch),
            any(),
            any()
        );

        verify(gitHubClient).addLabelsToPullRequest(
            eq(REPO_FULL_NAME),
            eq(releasePullRequest),
            eq(Collections.singletonList(RELEASE_PULL_REQUEST_LABEL))
        );
    }

    @Test
    public void dontLabelAlreadyLabeledPullRequest() throws Exception {
        Branch targetBranch = branch(TARGET_BRANCH_NAME);
        Branch releaseBranch = branch(RELEASE_BRANCH_NAME);

        PullRequest releasePullRequest = pullRequest(RELEASE_BRANCH_NAME, TARGET_BRANCH_NAME);

        when(gitHubClient.createPullRequest(any(), any(Branch.class), any(Branch.class), any(), any()))
            .thenReturn(releasePullRequest);
        when(gitHubClient.getPullRequestLabels(any(), any())).thenReturn(getLabels(RELEASE_PULL_REQUEST_LABEL,
            "hotfix"));

        PullRequest pullRequest = releasePullRequestCreator.createReleasePullRequest(
            jobContext, REPO_FULL_NAME, targetBranch, releaseBranch, version, event -> {
            }
        );

        Assert.assertEquals(releasePullRequest, pullRequest);
        verify(gitHubClient, never()).addLabelsToPullRequest(any(), any(), any());
    }

    public static List<Label> getLabels(String... labels) {
        return Arrays.stream(labels).map(l -> {
            Label label = new Label();
            label.setName(l);
            return label;
        }).collect(Collectors.toList());
    }

}
