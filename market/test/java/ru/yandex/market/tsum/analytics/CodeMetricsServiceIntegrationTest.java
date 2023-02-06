package ru.yandex.market.tsum.analytics;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.analytics.model.ChangeMetrics;
import ru.yandex.market.tsum.analytics.model.CommitMetrics;
import ru.yandex.market.tsum.analytics.model.IssueMetrics;
import ru.yandex.market.tsum.analytics.model.ReviewMetrics;
import ru.yandex.market.tsum.clients.arcadia.ArcArcadiaClient;
import ru.yandex.market.tsum.clients.arcadia.RootArcadiaClient;
import ru.yandex.market.tsum.clients.bitbucket.BitbucketClient;
import ru.yandex.market.tsum.clients.calendar.CalendarClient;
import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.core.TsumDebugRuntimeConfig;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogEntry;
import ru.yandex.market.tsum.pipelines.common.resources.ChangelogInfo;
import ru.yandex.market.tsum.release.RepositoryType;
import ru.yandex.startrek.client.Issues;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 17.01.18
 */

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TsumDebugRuntimeConfig.class})
public class CodeMetricsServiceIntegrationTest {
    private static final String REPOSITORY_ID = "market-infra/market-health";
    private static final Instant RELEASE_DATE = Instant.parse("2018-02-11T12:00:00Z");

    @Autowired
    private Issues startrekIssues;

    @Autowired
    private GitHubClient gitHubClient;

    @Autowired
    private BitbucketClient bitbucketClient;

    @Mock
    private ArcArcadiaClient arcArcadiaClient;

    @Autowired
    private RootArcadiaClient arcadiaClient;

    @Autowired
    private CalendarClient calendarClient;

    @Test
    public void calculatesIssueSecondsToResolve() {
        CodeMetricsService service = new CodeMetricsService(
            startrekIssues, gitHubClient, bitbucketClient, arcadiaClient, arcArcadiaClient, calendarClient
        );

        ChangelogInfo logInfo = new ChangelogInfo(
            Collections.singletonList(
                new ChangelogEntry("d7d55544c9e2377c7fcac9a335ef90c72fb61f5a", "MARKETINFRATEST-3882 test")
            )
        );

        List<ChangeMetrics> result = service.analyse(REPOSITORY_ID, logInfo, RELEASE_DATE, RepositoryType.GITHUB);
        IssueMetrics issueMetrics = result.get(0).getIssueMetrics();

        Assert.assertEquals(706, issueMetrics.getSecondsToResolve());

        assertThat(issueMetrics.getSecondsInStatus(), hasEntry("inProgress", 695L));
        assertThat(issueMetrics.getSecondsInStatus(), hasEntry("open", 11L));
        assertThat(issueMetrics.getSecondsInStatus(), hasEntry("closed", 2154513L));
    }

    @Test
    public void calculatesGithubMetrics() {
        CodeMetricsService service = new CodeMetricsService(
            startrekIssues, gitHubClient, bitbucketClient, arcadiaClient, arcArcadiaClient, calendarClient
        );

        ChangelogInfo logInfo = new ChangelogInfo(
            Collections.singletonList(
                new ChangelogEntry("d7d55544c9e2377c7fcac9a335ef90c72fb61f5a", "merged #1415")
            )
        );

        List<ChangeMetrics> result = service.analyse(REPOSITORY_ID, logInfo, RELEASE_DATE, RepositoryType.GITHUB);
        ReviewMetrics metrics = result.get(0).getReviewMetrics();

        Assert.assertEquals(Instant.parse("2017-12-13T12:56:44Z"), metrics.getCreateDate());
        Assert.assertEquals(Instant.parse("2017-12-18T15:48:48Z"), metrics.getMergeDate());
        Assert.assertEquals(Instant.parse("2017-12-15T12:37:17Z"), metrics.getFirstApprovalDate());
        Assert.assertEquals(Instant.parse("2017-12-18T15:48:41Z"), metrics.getLastApprovalDate());
        Assert.assertEquals(1415, metrics.getId());
        Assert.assertEquals(5, metrics.getNumberOfCommits());
        Assert.assertEquals(136, metrics.getNumberOfAdditions());
        Assert.assertEquals(101, metrics.getNumberOfDeletions());
        Assert.assertEquals(5, metrics.getNumberOfChangedFiles());
        Assert.assertEquals(451, metrics.getSecondsToFirstReview());
        Assert.assertEquals(442317, metrics.getSecondsToApproval());
        Assert.assertEquals(442324, metrics.getSecondsToMerge());
        Assert.assertEquals(5180596, metrics.getSecondsToRelease());
        Assert.assertEquals(283282, metrics.getSecondsToMergeFromLastCommit());
        Assert.assertEquals(270691, metrics.getSecondsToMergeFromFirstApproval());
        Assert.assertEquals(5008963, metrics.getSecondsToReleaseFromFirstApproval());
        Assert.assertEquals(4738279, metrics.getSecondsToReleaseFromLastApproval());
        Assert.assertEquals(7, metrics.getSecondsToMergeFromLastApproval());

        assertThat(metrics.getSecondsToCommitFromReview(),
            equalTo(Arrays.asList(76082L, 60794L)));

        assertThat(metrics.getSecondsToReviewFromCommit(), equalTo(Arrays.asList(20489L, 1330L)));

        CommitMetrics commitMetrics = result.get(0).getCommitMetrics();
        Assert.assertEquals(136, commitMetrics.getNumberOfAdditions());
        Assert.assertEquals(101, commitMetrics.getNumberOfDeletions());
        Assert.assertEquals(5, commitMetrics.getNumberOfChangedFiles());
        Assert.assertEquals("alkedr", commitMetrics.getAuthor());
        Assert.assertEquals(Instant.parse("2017-12-18T15:48:47Z"), commitMetrics.getDate());
    }

    @Test
    public void calculatesGithubMetricsWithCommands() {
        CodeMetricsService service = new CodeMetricsService(
            startrekIssues, gitHubClient, bitbucketClient, arcadiaClient, arcArcadiaClient, calendarClient
        );

        ChangelogInfo logInfo = new ChangelogInfo(
            Collections.singletonList(
                new ChangelogEntry("13392c808e3e3c99fc3ba411d893027decc5f4b4", "merged #6249")
            )
        );

        List<ChangeMetrics> result = service.analyse(
            "market/MarketNode", logInfo, Instant.parse("2018-04-01T12:00:00Z"), RepositoryType.GITHUB
        );

        ReviewMetrics metrics = result.get(0).getReviewMetrics();

        Assert.assertEquals(Instant.parse("2018-03-16T10:02:39Z"), metrics.getCreateDate());
        Assert.assertEquals(Instant.parse("2018-04-01T12:00:00Z"), metrics.getMergeDate());
        Assert.assertEquals(Instant.parse("2018-03-19T11:18:58Z"), metrics.getFirstApprovalDate());
        Assert.assertEquals(Instant.parse("2018-03-19T11:29:45Z"), metrics.getLastApprovalDate());
        Assert.assertEquals(6249, metrics.getId());
        Assert.assertEquals(4, metrics.getNumberOfCommits());
        Assert.assertEquals(83, metrics.getNumberOfAdditions());
        Assert.assertEquals(4, metrics.getNumberOfDeletions());
        Assert.assertEquals(3, metrics.getNumberOfChangedFiles());
        Assert.assertEquals(264025, metrics.getSecondsToFirstReview());
        Assert.assertEquals(264426, metrics.getSecondsToApproval());
        Assert.assertEquals(1389441, metrics.getSecondsToMerge());
        Assert.assertEquals(1389441, metrics.getSecondsToRelease());
        Assert.assertEquals(410259, metrics.getSecondsToMergeFromLastCommit());
        Assert.assertEquals(1125662, metrics.getSecondsToMergeFromFirstApproval());
        Assert.assertEquals(1125015, metrics.getSecondsToMergeFromLastApproval());

        Assert.assertEquals(1, metrics.getAddedLabelsDates().size());
        Assert.assertEquals(1, metrics.getAddedLabelsValues().size());
        Assert.assertEquals(3, metrics.getReviewCommandsDates().size());
        Assert.assertEquals(3, metrics.getReviewCommandsValues().size());
    }

    @Test
    public void marksPRMetrics() {
        CodeMetricsService service = new CodeMetricsService(
            startrekIssues, gitHubClient, bitbucketClient, arcadiaClient, arcArcadiaClient, calendarClient
        );

        ChangelogInfo logInfo = new ChangelogInfo(
            Arrays.asList(
                new ChangelogEntry("2964a5462abfa9a93730e0f62946195c2aed1a7f", "merged #38"),
                new ChangelogEntry("b95329dc9f943beb6003a237b6d9b53941486f09", "second"),
                new ChangelogEntry("bac1e2b4480df97407de888746f365fc1fbd4b46", "first")
            )
        );

        String fullRepositoryName = "market-infra/test-pipeline";
        List<ChangeMetrics> result = service.analyse(
            fullRepositoryName, logInfo, Instant.parse("2018-04-01T12:00:00Z"), RepositoryType.GITHUB
        );

        Assert.assertEquals(3, result.size());
        Assert.assertEquals(-1, result.get(0).getCommitMetrics().getBelongsToReviewId());
        Assert.assertEquals(38, result.get(1).getCommitMetrics().getBelongsToReviewId());
        Assert.assertEquals(38, result.get(2).getCommitMetrics().getBelongsToReviewId());

    }

    @Ignore
    @Test
    public void exportsToTskv() {
        CodeMetricsService service = new CodeMetricsService(
            startrekIssues, gitHubClient, bitbucketClient, arcadiaClient, arcArcadiaClient, calendarClient
        );

        ChangelogInfo logInfo = new ChangelogInfo(
            Collections.singletonList(
                new ChangelogEntry("d7d55544c9e2377c7fcac9a335ef90c72fb61f5a", "MARKETINFRATEST-3882  merged #1415")
            )
        );

        List<ChangeMetrics> result = service.analyse(REPOSITORY_ID, logInfo, RELEASE_DATE, RepositoryType.GITHUB);
        System.out.println(result.get(0).toTskv());
    }

    @Test
    public void calculatesArcadiaMetrics() {
        CodeMetricsService service = new CodeMetricsService(
            startrekIssues, gitHubClient, bitbucketClient, arcadiaClient, arcArcadiaClient, calendarClient
        );

        ChangelogInfo logInfo = new ChangelogInfo(
            Collections.singletonList(
                new ChangelogEntry("2908464", "REVIEW: 270915")
            )
        );

        List<ChangeMetrics> result = service.analyse(null, logInfo, RELEASE_DATE, RepositoryType.GITHUB);

        ReviewMetrics metrics = result.get(0).getReviewMetrics();

        Assert.assertEquals(Instant.parse("2017-05-03T09:34:21Z"), metrics.getCreateDate());
        Assert.assertEquals(Instant.parse("2017-05-15T09:36:47Z"), metrics.getMergeDate());
        Assert.assertEquals(Instant.parse("2017-05-15T09:36:30Z"), metrics.getFirstApprovalDate());
        Assert.assertEquals(270915, metrics.getId());
        Assert.assertEquals(2, metrics.getNumberOfCommits());
        Assert.assertEquals(81, metrics.getNumberOfAdditions());
        Assert.assertEquals(1, metrics.getNumberOfDeletions());
        Assert.assertEquals(5, metrics.getNumberOfChangedFiles());
        Assert.assertEquals(421, metrics.getSecondsToFirstReview());
        Assert.assertEquals(24546339, metrics.getSecondsToRelease());
        Assert.assertEquals(1036929, metrics.getSecondsToApproval());
        Assert.assertEquals(1036946, metrics.getSecondsToMerge());
        Assert.assertEquals(1033562, metrics.getSecondsToMergeFromLastCommit());
        Assert.assertEquals(17, metrics.getSecondsToMergeFromFirstApproval());
        Assert.assertEquals(17, metrics.getSecondsToMergeFromLastApproval());

        assertThat(metrics.getSecondsToCommitFromReview(),
            equalTo(Collections.singletonList(2963L)));

        assertThat(metrics.getSecondsToReviewFromCommit(), equalTo(Arrays.asList(421L, 95L)));

        CommitMetrics commitMetrics = result.get(0).getCommitMetrics();
        Assert.assertEquals(81, commitMetrics.getNumberOfAdditions());
        Assert.assertEquals(1, commitMetrics.getNumberOfDeletions());
        Assert.assertEquals(5, commitMetrics.getNumberOfChangedFiles());
        Assert.assertEquals("vladvin", commitMetrics.getAuthor());
        Assert.assertEquals(Instant.parse("2017-05-15T09:36:45.942048Z"), commitMetrics.getDate());
    }
}
