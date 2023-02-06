package ru.yandex.market.tsum.release.merge.filters;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Sets;
import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.PullRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.tsum.clients.github.GitHubClient;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.tsum.pipelines.test_data.TestPullRequestFactory.pullRequest;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 08.06.17
 */
public class PullRequestStatusValidatorTest {
    private static final String REPO_FULL_NAME = "market/market";
    private static final String TEAMCITY_CONTEXT = "teamcity";
    private static final String CODE_REVIEW_CONTEXT = "Ревью кода";
    private GitHubClient gitHubClient;

    @Before
    public void setUp() throws Exception {
        gitHubClient = mock(GitHubClient.class);
    }

    @Test
    public void emptyStatusChecks() throws Exception {
        testOnePullRequest(Collections.emptyList(), 0, 1);
    }

    @Test
    public void oneSuccessfulStatusCheck() throws Exception {
        testOnePullRequest(
            Arrays.asList(commitStatus(CommitStatus.STATE_SUCCESS, TEAMCITY_CONTEXT)),
            1, 0
        );
    }

    @Test
    public void onePendingStatusCheck() throws Exception {
        testOnePullRequest(
            Arrays.asList(commitStatus(CommitStatus.STATE_PENDING, TEAMCITY_CONTEXT)),
            0, 1
        );
    }

    @Test
    public void onePendingStatusCheckWithTimeout() throws Exception {
        testOnePullRequest(
            Arrays.asList(commitStatus(CommitStatus.STATE_PENDING, TEAMCITY_CONTEXT)),
            0, 1,
            new PullRequestStatusValidator(gitHubClient, Sets.newHashSet(TEAMCITY_CONTEXT),
                new PullRequestStatusValidator.PollerSettings(600, TimeUnit.MILLISECONDS, 1, TimeUnit.SECONDS))
        );
    }

    @Test
    public void oneFailingStatusCheck() throws Exception {
        testOnePullRequest(
            Arrays.asList(commitStatus(CommitStatus.STATE_FAILURE, TEAMCITY_CONTEXT)),
            0, 1
        );
    }

    @Test
    public void oldStatusChecksDontCount() throws Exception {
        testOnePullRequest(
            Arrays.asList(
                commitStatus(CommitStatus.STATE_SUCCESS, TEAMCITY_CONTEXT, nowMinusMinutes(1)),
                commitStatus(CommitStatus.STATE_PENDING, TEAMCITY_CONTEXT, nowMinusMinutes(2)),
                commitStatus(CommitStatus.STATE_FAILURE, TEAMCITY_CONTEXT, nowMinusMinutes(3))
            ),
            1, 0
        );
    }

    @Test
    public void nonRequiredStatusChecksDontCount() throws Exception {
        testOnePullRequest(
            Arrays.asList(
                commitStatus(CommitStatus.STATE_SUCCESS, TEAMCITY_CONTEXT),
                commitStatus(CommitStatus.STATE_FAILURE, CODE_REVIEW_CONTEXT)
            ),
            1, 0
        );
    }

    @Test
    public void requiredStatusCheckIsMissing() throws Exception {
        testOnePullRequest(
            Arrays.asList(
                commitStatus(CommitStatus.STATE_FAILURE, CODE_REVIEW_CONTEXT)
            ),
            0, 1
        );
    }

    private Date nowMinusMinutes(int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -minutes);
        return calendar.getTime();
    }

    private void testOnePullRequest(List<CommitStatus> statuses, int expectedAccepted, int expectedFilteredOut,
                                    PullRequestStatusValidator sut) throws InterruptedException {
        PullRequest pullRequest = pullRequest();
        Mockito.when(gitHubClient.getStatusChecks(eq(REPO_FULL_NAME), eq(pullRequest)))
            .thenReturn(statuses);

        PullRequestValidatorResult result = sut.validate(REPO_FULL_NAME, Collections.singletonList(pullRequest));

        Assert.assertEquals(expectedAccepted, result.getAcceptedPullRequests().size());
        Assert.assertEquals(expectedFilteredOut, result.getRejectedPullRequests().size());
    }

    private void testOnePullRequest(List<CommitStatus> statuses, int expectedAccepted, int expectedFilteredOut)
        throws InterruptedException {
        testOnePullRequest(statuses, expectedAccepted, expectedFilteredOut,
            new PullRequestStatusValidator(gitHubClient, Sets.newHashSet(TEAMCITY_CONTEXT),
                new PullRequestStatusValidator.PollerSettings(0, TimeUnit.MILLISECONDS, 1, TimeUnit.SECONDS)));
    }

    private CommitStatus commitStatus(String state, String context) {
        return commitStatus(state, context, new Date());
    }

    private CommitStatus commitStatus(String state, String context, Date date) {
        CommitStatus status = new CommitStatus();
        status.setState(state);
        status.setContext(context);
        status.setCreatedAt(date);
        status.setUpdatedAt(date);
        return status;
    }

}
