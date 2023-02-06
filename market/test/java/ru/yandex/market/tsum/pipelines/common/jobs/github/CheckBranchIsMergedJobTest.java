package ru.yandex.market.tsum.pipelines.common.jobs.github;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.verification.VerificationMode;

import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.pipe.engine.runtime.exceptions.JobManualFailException;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.common.jobs.github.resources.CheckBranchIsMergedConfig;
import ru.yandex.market.tsum.pipelines.common.resources.BranchRef;
import ru.yandex.market.tsum.pipelines.common.resources.GithubRepo;

import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.CheckBranchIsMergedJob.Mode.TARGET_INTO_SOURCE;
import static ru.yandex.market.tsum.pipelines.common.resources.BranchRef.MASTER;

@RunWith(JUnit4.class)
public class CheckBranchIsMergedJobTest {

    private static final String TEST_REPO = "githubrepo";
    private static final String TEST_BRANCH = "githubbranch";
    private static final String TEST_TARGET_BRANCH = "githubtargetbranch";
    private static final VerificationMode SINGLE_TIME = times(1);

    private final GitHubClient gitHubClientMock = createGitHubClientMock();
    private final CheckBranchIsMergedJob job = createJobWithInjectedValues(gitHubClientMock);

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void jobCallsGitHubExactlyOnce() throws Exception {
        job.execute(null);
        verify(gitHubClientMock, SINGLE_TIME)
            .isBranchMergedTo(any(), any(), any());
    }

    @Test
    public void jobPassesInjectedValuesToGitHub() throws Exception {
        job.execute(null);
        verifyGitHubCall(TEST_REPO, TEST_BRANCH, MASTER.getName());
    }

    @Test
    public void targetBranchMayBeSpecifiedInConfig() throws Exception {
        job.setConfig(CheckBranchIsMergedConfig.builder().withTargetBranch(new BranchRef(TEST_TARGET_BRANCH)).build());
        job.execute(null);
        verifyGitHubCall(TEST_REPO, TEST_BRANCH, TEST_TARGET_BRANCH);
    }

    @Test
    public void targetIntoSourceModeReversesCheck() throws Exception {
        job.setConfig(CheckBranchIsMergedConfig.builder().withMode(TARGET_INTO_SOURCE).build());
        job.execute(null);
        verifyGitHubCall(TEST_REPO, MASTER.getName(), TEST_BRANCH);
    }

    private void verifyGitHubCall(String repo, String sourceBranch, String targetBranch) {
        verify(gitHubClientMock, SINGLE_TIME)
            .isBranchMergedTo(repo, sourceBranch, targetBranch);
    }

    @Test
    public void jobFailsIfGitHubReturnsFalse() throws Exception {
        expected.expect(JobManualFailException.class);
        expected.expectMessage(is(format("Branch '%s#%s' is NOT merged into '%s#%s'!",
            TEST_REPO, TEST_BRANCH, TEST_REPO, MASTER.getName())));
        when(gitHubClientMock.isBranchMergedTo(any(), any(), any())).thenReturn(false);
        job.execute(new TestJobContext());
    }

    private CheckBranchIsMergedJob createJobWithInjectedValues(GitHubClient client) {
        CheckBranchIsMergedJob createdJob = new CheckBranchIsMergedJob();
        createdJob.setGitHubClient(client);
        createdJob.setRepo(new GithubRepo(TEST_REPO));
        createdJob.setSourceBranch(new BranchRef(TEST_BRANCH));
        return createdJob;
    }

    private GitHubClient createGitHubClientMock() {
        GitHubClient mock = mock(GitHubClient.class);
        when(mock.isBranchMergedTo(anyString(), anyString(), anyString())).thenReturn(true);
        return mock;
    }
}
