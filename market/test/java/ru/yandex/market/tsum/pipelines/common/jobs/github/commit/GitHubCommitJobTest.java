package ru.yandex.market.tsum.pipelines.common.jobs.github.commit;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.eclipse.egit.github.core.CommitStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Answers;

import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.clients.pollers.Poller;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobActionsContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.SupportType;
import ru.yandex.market.tsum.pipe.engine.runtime.exceptions.JobManualFailException;

import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.BASE_BRANCH_NAME;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.FILE_CONTENTS;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.FILE_PATH;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.FULL_JOB_ID;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.JOB_LAUNCH_DETAILS_URL;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.PULL_REQUEST_TITLE;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.REPOSITORY_NAME;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.REPOSITORY_ORGANIZATION;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.approvedReview;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.changesRequestedReview;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.closedPullRequest;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.createHeadRefToReturnOk;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.createPullRequestToReturnOk;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.deleteHeadRefToReturn;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.getBaseBranchToReturnEmptyList;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.getBaseBranchToReturnRequiredChecks;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.getBaseRefToReturnBaseBranch;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.getHeadBranchStatusesToReturn;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.getHeadBranchToReturnOk;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.getMatchingPullRequestsToReturn;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.getReviewsToReturn;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.mergedPullRequest;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.nonRequiredCheck;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.openPullRequest;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.putFileToReturnOk;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.putMergeToReturn;
import static ru.yandex.market.tsum.pipelines.common.jobs.github.commit.WireMockRequests.requiredCheck;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 22.05.2019
 */
public class GitHubCommitJobTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());
    private GitHubClient gitHubClient;

    @Before
    public void setUp() {
        gitHubClient = new GitHubClient("localhost", wireMockRule.port(), "http", "");
    }

    @Test
    public void shouldCreateBranchAndPullRequestAndPoll_whenPullRequestDoesNotExist() {
        givenNoBranch();
        givenNoPullRequest();
        givenPullRequestCreationWillSucceed();
        givenCreatedPullRequestIsMergeable();
        givenCreatedPullRequestMergeWillSucceed();
        assertThatJobMergesPullRequest();
    }

    @Test
    public void shouldRecreateBranchAndCreatePullRequest_whenBranchExistsAndPullRequestDoesNot() {
        givenBranch();
        givenNoPullRequest();
        givenPullRequestCreationWillSucceed();
        givenCreatedPullRequestIsMergeable();
        givenCreatedPullRequestMergeWillSucceed();
        assertThatJobMergesPullRequest();
    }

    @Test
    public void shouldCompleteSuccessfully_whenPullRequestExists_andIsMerged() throws Exception {
        givenMergedPullRequest();
        // Проверяем что не падает. Ручка мёржа не замокана, упадём если джоба попытается смёржить ещё раз.
        runJob();
    }


    @Test
    public void shouldPoll_whenPullRequestExists_andIsAboutToBecomeMergeable() {
        givenPullRequestAboutToBecomeMergeable();
        givenMergeablePullRequestMergeWillSucceed();
        assertThatJobMergesPullRequest();
    }


    @Test
    public void shouldMerge_noReview_successfulRequiredCheck() {
        givenOpenPullRequest();
        givenNoReview();
        givenSuccessfulRequiredCheck();
        givenPullRequestMergeWillSucceed();
        assertThatJobMergesPullRequest();
    }

    @Test
    public void shouldMerge_approvedReview_successfulRequiredCheck() {
        givenOpenPullRequest();
        givenApprovedReview();
        givenSuccessfulRequiredCheck();
        givenPullRequestMergeWillSucceed();
        assertThatJobMergesPullRequest();
    }

    @Test
    public void shouldFail_changesRequestedReview_successfulRequiredCheck() {
        givenOpenPullRequest();
        givenChangesRequestedReview();
        givenSuccessfulRequiredCheck();
        givenPullRequestMergeWillSucceed();
        assertThatJobFails();
    }


    @Test
    public void shouldMerge_noReview_successfulRequiredCheckAndFailedNonRequiredCheck() {
        givenOpenPullRequest();
        givenNoReview();
        givenSuccessfulRequiredCheckAndFailedNonRequiredCheck();
        givenPullRequestMergeWillSucceed();
        assertThatJobMergesPullRequest();
    }

    @Test
    public void shouldFail_noReview_failedRequiredCheck() {
        givenOpenPullRequest();
        givenNoReview();
        givenFailedRequiredCheck();
        givenPullRequestMergeWillSucceed();
        assertThatJobFails();
    }

    @Test
    public void shouldTimeout_noReview_pendingRequiredCheck() {
        givenOpenPullRequest();
        givenNoReview();
        givenPendingRequiredCheck();
        givenPullRequestMergeWillSucceed();
        assertThatJobTimeouts();
    }

    @Test
    public void shouldFail_noReview_noConfiguredRequiredChecksForBaseBranch() {
        givenOpenPullRequest();
        givenNoReview();
        givenNoConfiguredRequiredChecksForBaseBranch();
        givenPullRequestMergeWillSucceed();
        assertThatJobFails();
    }

    @Test
    public void shouldFail_noReview_noRequiredChecks() {
        givenOpenPullRequest();
        givenNoReview();
        givenNoRequiredChecks();
        givenPullRequestMergeWillSucceed();
        assertThatJobTimeouts();
    }


    @Test
    public void shouldFail_whenThereAreConflicts() {
        givenOpenPullRequest();
        givenNoReview();
        givenSuccessfulRequiredCheck();
        givenPullRequestMergeWillFailBecauseOfConflicts();
        assertThatJobFails();
    }

    @Test
    public void shouldFail_whenPullRequestIsClosedWithoutMerging() {
        givenClosedPullRequest();
        givenNoReview();
        givenSuccessfulRequiredCheck();
        assertThatJobFails();
    }

    @Test
    public void shouldFail_whenThereAreTwoPullRequests() {
        givenTwoPullRequests();
        assertThatJobFails();
    }


    private void givenNoBranch() {
        mockBranchRequests(422);  // 422 Unprocessable Entity
    }

    private void givenBranch() {
        mockBranchRequests(204);
    }

    private void mockBranchRequests(int deleteHeadBranchStatus) {
        wireMockRule.stubFor(getBaseRefToReturnBaseBranch());
        wireMockRule.stubFor(deleteHeadRefToReturn(deleteHeadBranchStatus)
            .whenScenarioStateIs(STARTED).willSetStateTo("HEAD_BRANCH_DELETED"));
        wireMockRule.stubFor(createHeadRefToReturnOk().whenScenarioStateIs("HEAD_BRANCH_DELETED")
            .willSetStateTo("HEAD_BRANCH_CREATED"));
        wireMockRule.stubFor(getHeadBranchToReturnOk().whenScenarioStateIs("HEAD_BRANCH_CREATED"));
        wireMockRule.stubFor(putFileToReturnOk().whenScenarioStateIs("HEAD_BRANCH_CREATED")
            .willSetStateTo("FILE_COMMITTED"));
    }


    private void givenNoPullRequest() {
        wireMockRule.stubFor(getMatchingPullRequestsToReturn().whenScenarioStateIs(STARTED));
    }

    private void givenOpenPullRequest() {
        wireMockRule.stubFor(getMatchingPullRequestsToReturn(openPullRequest()).whenScenarioStateIs(STARTED));
    }

    private void givenMergedPullRequest() {
        wireMockRule.stubFor(getMatchingPullRequestsToReturn(mergedPullRequest()).whenScenarioStateIs(STARTED));
    }

    private void givenClosedPullRequest() {
        wireMockRule.stubFor(getMatchingPullRequestsToReturn(closedPullRequest()).whenScenarioStateIs(STARTED));
    }

    private void givenPullRequestCreationWillSucceed() {
        wireMockRule.stubFor(createPullRequestToReturnOk().whenScenarioStateIs("FILE_COMMITTED").willSetStateTo(
            "PULL_REQUEST_CREATED"));
    }

    private void givenCreatedPullRequestIsMergeable() {
        wireMockRule.stubFor(getMatchingPullRequestsToReturn(openPullRequest()).whenScenarioStateIs(
            "PULL_REQUEST_CREATED"));
        wireMockRule.stubFor(getReviewsToReturn().whenScenarioStateIs("PULL_REQUEST_CREATED"));
        wireMockRule.stubFor(getBaseBranchToReturnRequiredChecks().whenScenarioStateIs("PULL_REQUEST_CREATED"));
        wireMockRule.stubFor(getHeadBranchStatusesToReturn(requiredCheck(CommitStatus.STATE_SUCCESS))
            .whenScenarioStateIs("PULL_REQUEST_CREATED"));
    }


    private void givenPullRequestAboutToBecomeMergeable() {
        wireMockRule.stubFor(getMatchingPullRequestsToReturn(openPullRequest()));
        wireMockRule.stubFor(getReviewsToReturn());
        wireMockRule.stubFor(getBaseBranchToReturnRequiredChecks());

        wireMockRule.stubFor(getHeadBranchStatusesToReturn().whenScenarioStateIs(STARTED).willSetStateTo("POLL1"));
        wireMockRule.stubFor(getHeadBranchStatusesToReturn().whenScenarioStateIs("POLL1").willSetStateTo("POLL2"));
        wireMockRule.stubFor(getHeadBranchStatusesToReturn().whenScenarioStateIs("POLL2").willSetStateTo("POLL3"));
        wireMockRule.stubFor(getHeadBranchStatusesToReturn(requiredCheck(CommitStatus.STATE_SUCCESS))
            .whenScenarioStateIs("POLL3").willSetStateTo("PULL_REQUEST_MERGEABLE"));
    }


    private void givenNoReview() {
        wireMockRule.stubFor(getReviewsToReturn().whenScenarioStateIs(STARTED));
    }

    private void givenApprovedReview() {
        wireMockRule.stubFor(getReviewsToReturn(approvedReview()).whenScenarioStateIs(STARTED));
    }

    private void givenChangesRequestedReview() {
        wireMockRule.stubFor(getReviewsToReturn(changesRequestedReview()).whenScenarioStateIs(STARTED));
    }


    private void givenNoConfiguredRequiredChecksForBaseBranch() {
        wireMockRule.stubFor(getBaseBranchToReturnEmptyList().whenScenarioStateIs(STARTED));
    }

    private void givenNoRequiredChecks() {
        wireMockRule.stubFor(getBaseBranchToReturnRequiredChecks().whenScenarioStateIs(STARTED));
        wireMockRule.stubFor(getHeadBranchStatusesToReturn().whenScenarioStateIs(STARTED));
    }

    private void givenSuccessfulRequiredCheck() {
        wireMockRule.stubFor(getBaseBranchToReturnRequiredChecks().whenScenarioStateIs(STARTED));
        wireMockRule.stubFor(
            getHeadBranchStatusesToReturn(requiredCheck(CommitStatus.STATE_SUCCESS)).whenScenarioStateIs(STARTED)
        );
    }

    private void givenFailedRequiredCheck() {
        wireMockRule.stubFor(getBaseBranchToReturnRequiredChecks().whenScenarioStateIs(STARTED));
        wireMockRule.stubFor(
            getHeadBranchStatusesToReturn(requiredCheck(CommitStatus.STATE_FAILURE)).whenScenarioStateIs(STARTED)
        );
    }

    private void givenPendingRequiredCheck() {
        wireMockRule.stubFor(getBaseBranchToReturnRequiredChecks().whenScenarioStateIs(STARTED));
        wireMockRule.stubFor(
            getHeadBranchStatusesToReturn(requiredCheck(CommitStatus.STATE_PENDING)).whenScenarioStateIs(STARTED)
        );
    }

    private void givenSuccessfulRequiredCheckAndFailedNonRequiredCheck() {
        wireMockRule.stubFor(getBaseBranchToReturnRequiredChecks().whenScenarioStateIs(STARTED));
        wireMockRule.stubFor(
            getHeadBranchStatusesToReturn(requiredCheck(CommitStatus.STATE_SUCCESS),
                nonRequiredCheck(CommitStatus.STATE_FAILURE))
                .whenScenarioStateIs(STARTED)
        );
    }


    private void givenCreatedPullRequestMergeWillSucceed() {
        wireMockRule.stubFor(putMergeToReturn(200).whenScenarioStateIs("PULL_REQUEST_CREATED").willSetStateTo(
            "PULL_REQUEST_MERGED"));
    }

    private void givenMergeablePullRequestMergeWillSucceed() {
        wireMockRule.stubFor(putMergeToReturn(200).whenScenarioStateIs("PULL_REQUEST_MERGEABLE").willSetStateTo(
            "PULL_REQUEST_MERGED"));
    }

    private void givenPullRequestMergeWillSucceed() {
        wireMockRule.stubFor(putMergeToReturn(200).whenScenarioStateIs(STARTED).willSetStateTo("PULL_REQUEST_MERGED"));
    }

    private void givenPullRequestMergeWillFailBecauseOfConflicts() {
        wireMockRule.stubFor(putMergeToReturn(405).whenScenarioStateIs(STARTED));
    }


    private void givenTwoPullRequests() {
        wireMockRule.stubFor(getMatchingPullRequestsToReturn(openPullRequest(), openPullRequest()));
    }


    private void assertThatJobMergesPullRequest() {
        try {
            runJob();
        } catch (TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        verifyThatPullRequestWasMerged();
    }

    private void assertThatJobFails() {
        assertThatThrownBy(this::runJob).isInstanceOf(JobManualFailException.class);
    }

    private void assertThatJobTimeouts() {
        assertThatThrownBy(this::runJob).isInstanceOf(InterruptedException.class);
    }

    private void runJob() throws TimeoutException, InterruptedException {
        Poller.Sleeper sleeper = new Poller.Sleeper() {
            private int count;

            @Override
            public void sleep(Duration duration) throws InterruptedException {
                count++;
                if (count > 100) {
                    throw new InterruptedException("Poller is polling for too long");
                }
            }
        };
        JobActionsContext jobActionsContext = mock(JobActionsContext.class);
        doThrow(new JobManualFailException("test", Collections.singletonList(SupportType.NONE)))
            .when(jobActionsContext).failJob(anyString(), any(SupportType.class));

        JobContext jobContext = mock(JobContext.class);
        when(jobContext.getFullJobId()).thenReturn(FULL_JOB_ID);
        when(jobContext.getJobLaunchDetailsUrl()).thenReturn(JOB_LAUNCH_DETAILS_URL);
        when(jobContext.progress()).then(Answers.RETURNS_MOCKS);
        when(jobContext.actions()).thenReturn(jobActionsContext);
        new TestJob(gitHubClient, sleeper).execute(jobContext);
    }


    private void verifyThatPullRequestWasMerged() {
        assertEquals("PULL_REQUEST_MERGED", wireMockRule.getAllScenarios().getScenarios().get(0).getState());
    }


    private static class TestJob extends GitHubCommitJob {
        TestJob(GitHubClient gitHubClient, Poller.Sleeper sleeper) {
            super(gitHubClient, sleeper);
        }

        @Override
        protected GitHubCommitJobConfig getConfig(JobContext context) {
            return new GitHubCommitJobConfig.Builder()
                .withRepositoryUserOrOrganization(REPOSITORY_ORGANIZATION)
                .withRepositoryName(REPOSITORY_NAME)
                .withBaseBranchName(BASE_BRANCH_NAME)
                .withPullRequestTitle(PULL_REQUEST_TITLE)
                .build();
        }

        @Override
        protected void commitChanges(JobContext jobContext, CommitContext commitContext) {
            commitContext.addFile(FILE_PATH, FILE_CONTENTS);
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("21977304-8dff-42bf-b4fc-477381442dfc");
        }
    }
}
