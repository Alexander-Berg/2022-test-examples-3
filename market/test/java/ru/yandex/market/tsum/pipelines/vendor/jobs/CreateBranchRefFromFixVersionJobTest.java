package ru.yandex.market.tsum.pipelines.vendor.jobs;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.clients.github.model.Branch;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.common.resources.BranchRef;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersionName;
import ru.yandex.market.tsum.pipelines.common.resources.GithubRepo;
import ru.yandex.market.tsum.pipelines.vendor.jobs.CreateBranchRefFromFixVersionJob.Config;
import ru.yandex.market.tsum.pipelines.vendor.jobs.CreateBranchRefFromFixVersionJob.ReleaseType;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.pipelines.vendor.jobs.VendorPipeConstants.GITHUB_REPO;

@RunWith(JUnit4.class)
public class CreateBranchRefFromFixVersionJobTest {

    private final TestJobContext jobContext = new TestJobContext();
    private final GitHubClient gitHubClient = createGitHubClientMock();
    private final CreateBranchRefFromFixVersionJob job = createJobInstance(gitHubClient);

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void jobProducesGithubRepo() throws Exception {
        job.execute(jobContext);
        GithubRepo repo = jobContext.getResource(GithubRepo.class);
        assertSame(GITHUB_REPO, repo);
    }

    @Test(expected = IllegalArgumentException.class)
    public void versionMustHaveAtLeastOneComment() throws Exception {
        executeWithVersion("2017.4.1");
    }

    @Test
    public void versionCommentMayContainDigitsAndUnderscoresAndDashes() throws Exception {
        executeWithVersion("2017.4.1[some_c0mm4n7-yall]");
    }

    @Test(expected = IllegalArgumentException.class)
    public void versionCommentMayNotContainSpace() throws Exception {
        executeWithVersion("2017.4.1[some comment]");
    }

    @Test
    public void versionMayHaveMultipleComments() throws Exception {
        executeWithVersion("2017.4.1[some][comment][yall]");
    }

    @Test(expected = IllegalArgumentException.class)
    public void versionMayNotSeparateCounterAndCommentByAnything() throws Exception {
        executeWithVersion("2017.4.1 [some]");
    }

    @Test(expected = IllegalArgumentException.class)
    public void versionMayNotSeparateCommentsByAnything() throws Exception {
        executeWithVersion("2017.4.1[some] [comment]");
    }

    @Test(expected = IllegalArgumentException.class)
    public void jobFailsIfVersionYearIsBelow2000() throws Exception {
        executeWithVersion("1999.4.1[test]");
    }

    @Test(expected = IllegalArgumentException.class)
    public void jobFailsIfVersionYearIsAbove2099() throws Exception {
        executeWithVersion("2100.4.1[test]");
    }

    @Test
    public void jobDoesNotFailOnSomeValidYears() throws Exception {
        for (int i : new int[]{2000, 2017, 2042, 2069, 2099}) {
            executeWithVersion(i + ".4.1[test]");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void jobFailsIfVersionQuoterIsBelow1() throws Exception {
        executeWithVersion("2017.0.1[test]");
    }

    @Test(expected = IllegalArgumentException.class)
    public void jobFailsIfVersionQuoterIsAbove4() throws Exception {
        executeWithVersion("2017.5.1[test]");
    }

    @Test
    public void jobDoesNotFailOnValidQuoter() throws Exception {
        for (int i = 1; i <= 4; i++) {
            executeWithVersion("2017." + i + ".1[test]");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void jobFailsIfVersionCounterBelow1() throws Exception {
        executeWithVersion("2017.4.0[test]");
    }

    @Test(expected = IllegalArgumentException.class)
    public void jobFailsIfVersionCounterHasLeadingZero() throws Exception {
        executeWithVersion("2017.4.07[test]");
    }

    @Test
    public void jobDoesNotFailOnSomeValidCounters() throws Exception {
        for (int i : new int[]{1, 7, 34, 42, 71, 99}) {
            executeWithVersion("2017.4." + i + "[test]");
        }
    }

    @Test
    public void jobProducesReleaseBranchRefByDefault() throws Exception {
        executeWithVersion("2017.1.1[test]");
        assertEquals("release/2017.1.1_vendors", jobContext.getResource(BranchRef.class).getName());
    }

    @Test
    public void injectedVersionNameBecomesPartOfTheBranchName() throws Exception {
        executeWithVersion("2099.4.99[test]");
        assertEquals("release/2099.4.99_vendors", jobContext.getResource(BranchRef.class).getName());
    }

    @Test
    public void whenBranchNameCreatedVersionCommentsIgnored() throws Exception {
        executeWithVersion("2099.4.99[s_o_m_e][1gn0r4d][c-o-m-m-e-n-t-s]");
        assertEquals("release/2099.4.99_vendors", jobContext.getResource(BranchRef.class).getName());
    }

    @Test
    public void jobProducesHotfixBranchRefIfConfigured() throws Exception {
        job.setConfig(Config.withReleaseType(ReleaseType.HOTFIX));
        executeWithVersion("2017.1.1[test]");
        assertEquals("hotfix/2017.1.1_vendors", jobContext.getResource(BranchRef.class).getName());
    }

    @Test
    public void ifBranchNameIsValidJobCallsGitHubToCheckBranchExistence() throws Exception {
        executeWithVersion("2017.4.1[test]");
        verify(gitHubClient, times(1))
            .getBranch(GITHUB_REPO.getFullName(), "release/2017.4.1_vendors");
    }

    @Test
    public void jobFailsIfBranchIsNotFound() throws Exception {
        expected.expect(NullPointerException.class);
        String expectedBranchName = "release/2017.4.1_vendors";
        expected.expectMessage(format("Branch '%s#%s' not found!", GITHUB_REPO.getFullName(), expectedBranchName));
        when(gitHubClient.getBranch(any(), any())).thenReturn(null);
        executeWithVersion("2017.4.1[test]");
    }

    private void executeWithVersion(String versionName) throws Exception {
        job.setFixVersionName(new FixVersionName(versionName));
        job.execute(jobContext);
    }

    private CreateBranchRefFromFixVersionJob createJobInstance(GitHubClient gitHubClient) {
        CreateBranchRefFromFixVersionJob createdJob = new CreateBranchRefFromFixVersionJob();
        createdJob.setGitHubClient(gitHubClient);
        createdJob.setFixVersionName(new FixVersionName("2017.4.1[test]"));
        return createdJob;
    }

    private GitHubClient createGitHubClientMock() {
        GitHubClient mock = mock(GitHubClient.class);
        when(mock.getBranch(any(), any())).thenAnswer(inv ->
            new Branch(format("%s#%s", inv.getArguments()[0], inv.getArguments()[1])));
        return mock;
    }
}
