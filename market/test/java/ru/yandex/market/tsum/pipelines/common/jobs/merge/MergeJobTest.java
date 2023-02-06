package ru.yandex.market.tsum.pipelines.common.jobs.merge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.egit.github.core.PullRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.tsum.clients.github.GitHubClient;
import ru.yandex.market.tsum.clients.github.model.Branch;
import ru.yandex.market.tsum.clients.startrek.StartrekClient;
import ru.yandex.market.tsum.clients.startrek.StartrekCommentNotification;
import ru.yandex.market.tsum.clients.startrek.StartrekNotificationTarget;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.Notificator;
import ru.yandex.market.tsum.pipelines.common.jobs.merge.resources.MergeJobConfig;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;
import ru.yandex.market.tsum.pipelines.test_data.TestRepositoryFactory;
import ru.yandex.market.tsum.release.merge.MergeFinishedInfo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.pipelines.test_data.TestPullRequestFactory.pullRequest;
import static ru.yandex.market.tsum.release.merge.MergerTest.branch;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 02.06.17
 */
public class MergeJobTest {
    private static final String REPO_FULL_NAME = "market/market";

    private MergeJob sut;
    private List<PullRequest> pullRequests;
    private StartrekClientSpy startrekClient;
    private JobContext jobContext;
    private PullRequest pr1;
    private PullRequest pr2;
    private final Notificator notificator = Mockito.mock(Notificator.class);

    @Before
    public void setUp() throws Exception {
        initPullRequests();
        GitHubClient gitHubClient = mock(GitHubClient.class);

        when(gitHubClient.getRepository(REPO_FULL_NAME))
            .thenReturn(TestRepositoryFactory.repository("market", "market"));

        jobContext = mock(JobContext.class);
        when(jobContext.getPipeLaunchUrl()).thenReturn("http://pipe/launch");
        when(jobContext.getJobLaunchDetailsUrl()).thenReturn("http://pipe/launch/job/details");
        when(jobContext.notifications()).thenReturn(notificator);

        startrekClient = new StartrekClientSpy();
        sut = new MergeJob();
        sut.setReleaseInfo(new ReleaseInfo(new FixVersion(123, "fix_v"), "TEST-1", "tsum"));
        sut.setMergeJobConfig(new MergeJobConfig(REPO_FULL_NAME, "master"));
        sut.setGitHubClient(gitHubClient);
    }

    @Test
    public void successfulPullRequests() throws Exception {
        runNotifications(
            pullRequests,
            Collections.emptyList(),
            pullRequests,
            Collections.emptyList()
        );

        Mockito.verify(notificator, times(1)).notifyAboutEvent(any(), anyMap());
    }

    @Test
    public void failedPullRequests() throws Exception {
        runNotifications(
            Collections.emptyList(),
            pullRequests,
            pullRequests,
            Collections.emptyList()
        );

        Mockito.verify(notificator, times(1)).notifyAboutEvent(any(), anyMap());
    }

    @Test
    public void filteredOutPullRequests() throws Exception {
        runNotifications(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            pullRequests
        );

        Mockito.verify(notificator, times(2)).notifyAboutEvent(any(), anyMap());
    }

    @Test
    public void oneFilteredOutAndOneSuccessful() throws Exception {
        runNotifications(
            Arrays.asList(pr2),
            Collections.emptyList(),
            Arrays.asList(pr2),
            Arrays.asList(pr1)
        );

        Mockito.verify(notificator, times(2)).notifyAboutEvent(any(), anyMap());
    }

    private void runNotifications(List<PullRequest> successfulPullRequests, List<PullRequest> failedPullRequests,
                                  List<PullRequest> acceptedPullRequests, List<PullRequest> filteredOutPullRequests) {
        MergeFinishedInfo mergeFinishedEvent = createPullRequestStatus(
            successfulPullRequests,
            failedPullRequests,
            acceptedPullRequests,
            filteredOutPullRequests
        );

        sut.notifyMergeFinishedToReleaseIssue(mergeFinishedEvent, jobContext);
    }

    private MergeFinishedInfo createPullRequestStatus(List<PullRequest> successfulPullRequests,
                                                      List<PullRequest> failedPullRequests,
                                                      List<PullRequest> acceptedPullRequests,
                                                      List<PullRequest> filteredOutPullRequests) {
        Branch releaseBranch = branch("release-1.1");
        Branch targetBranch = branch("master");

        return new MergeFinishedInfo(
            targetBranch,
            releaseBranch,
            successfulPullRequests,
            failedPullRequests,
            acceptedPullRequests,
            filteredOutPullRequests
        );
    }

    private void initPullRequests() {
        String pullRequestBaseUrl = "https://github.yandex-team.ru/market/market/pull";

        pr1 = pullRequest(1, "feature/feature-1", "master");
        pr1.setTitle("TEST-1: Очень важная фича");
        pr1.setHtmlUrl(pullRequestBaseUrl + "/1");

        pr2 = pullRequest(2, "feature/feature-2", "master");
        pr2.setTitle("TEST-2: Ещё более важная фича");
        pr2.setHtmlUrl(pullRequestBaseUrl + "/2");

        this.pullRequests = Arrays.asList(pr1, pr2);
    }

    static class StartrekClientSpy extends StartrekClient {
        private final List<StartrekCommentNotification> sentNotifications = new ArrayList<>();

        StartrekClientSpy() {
            super(null, null, null);
        }

        @Override
        public void send(StartrekCommentNotification notification, StartrekNotificationTarget target) {
            this.sentNotifications.add(notification);
        }

        List<StartrekCommentNotification> getSentNotifications() {
            return sentNotifications;
        }
    }

}
