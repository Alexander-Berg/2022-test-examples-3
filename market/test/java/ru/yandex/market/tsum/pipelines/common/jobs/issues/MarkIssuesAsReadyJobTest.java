package ru.yandex.market.tsum.pipelines.common.jobs.issues;

import java.util.Collections;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.startrek.IssueBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipelines.common.jobs.release.IssueStatus;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;
import ru.yandex.market.tsum.pipelines.test_data.TestVersionBuilder;
import ru.yandex.market.tsum.release.FixVersionService;
import ru.yandex.market.tsum.release.ReleaseIssueService;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.Version;

@RunWith(MockitoJUnitRunner.class)
public class MarkIssuesAsReadyJobTest {
    private static final String RELEASE_ISSUE_KEY = "MARKETCHECKOUT-1";
    private static final String FEATURE_ISSUE_KEY = "MARKETCHECKOUT-2";
    private static final String ENVIRONMENT = "prestable";
    @Mock
    private ReleaseIssueService releaseIssueService;

    @Mock
    private FixVersionService fixVersionService;

    @Mock
    private JobContext jobContext;

    @InjectMocks
    private MarkIssuesAsReadyJob markIssuesAsReadyJob;

    @Test
    public void shouldNotifyAboutDeploy() throws Exception {
        int versionId = 123;

        markIssuesAsReadyJob.setReleaseInfo(new ReleaseInfo(new FixVersion(versionId, "asdasd"), "MARKETCHECKOUT" +
            "-135135"));
        markIssuesAsReadyJob.setMarkIssuesAsReadyJobConfig(MarkIssuesAsReadyJobConfig.builder()
            .setEnvironment(ENVIRONMENT)
            .build());

        Mockito.when(fixVersionService.getVersion(versionId)).thenReturn(TestVersionBuilder.aVersion().build());
        Mockito.when(releaseIssueService.getReleaseIssue(Mockito.any(Version.class)))
            .thenReturn(IssueBuilder.newBuilder(RELEASE_ISSUE_KEY).setStatus("Open").build());
        Mockito.when(releaseIssueService.getFeatureIssues(Mockito.any(Version.class)))
            .thenReturn(Collections.singletonList(IssueBuilder.newBuilder(FEATURE_ISSUE_KEY).setStatus(
                "ready_for_test").build()));

        markIssuesAsReadyJob.execute(jobContext);

        verifyUpdateEnvironmentCalls();
        verifyUpdateStatusCalls();
    }

    private void verifyUpdateEnvironmentCalls() {
        ArgumentCaptor<Issue> issueArgumentCaptor = ArgumentCaptor.forClass(Issue.class);
        ArgumentCaptor<String> environmentArgumentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(releaseIssueService, Mockito.times(2))
            .updateEnvironment(issueArgumentCaptor.capture(), environmentArgumentCaptor.capture());

        List<Issue> issues = issueArgumentCaptor.getAllValues();
        List<String> environments = environmentArgumentCaptor.getAllValues();

        Assert.assertThat(issues.get(0).getKey(), CoreMatchers.is(RELEASE_ISSUE_KEY));
        Assert.assertThat(environments.get(0), CoreMatchers.is(ENVIRONMENT));

        Assert.assertThat(issues.get(1).getKey(), CoreMatchers.is(FEATURE_ISSUE_KEY));
        Assert.assertThat(environments.get(1), CoreMatchers.is(ENVIRONMENT));
    }

    private void verifyUpdateStatusCalls() {
        ArgumentCaptor<Issue> issueArgumentCaptor = ArgumentCaptor.forClass(Issue.class);
        ArgumentCaptor<IssueStatus> environmentArgumentCaptor = ArgumentCaptor.forClass(IssueStatus.class);
        Mockito.verify(releaseIssueService, Mockito.times(2))
            .changeIssueStatusTo(issueArgumentCaptor.capture(), environmentArgumentCaptor.capture());

        List<Issue> issues = issueArgumentCaptor.getAllValues();
        List<IssueStatus> statuses = environmentArgumentCaptor.getAllValues();

        Assert.assertThat(issues.get(0).getKey(), CoreMatchers.is(RELEASE_ISSUE_KEY));
        Assert.assertThat(statuses.get(0), CoreMatchers.is(IssueStatus.READY_FOR_TEST));

        Assert.assertThat(issues.get(1).getKey(), CoreMatchers.is(FEATURE_ISSUE_KEY));
        Assert.assertThat(statuses.get(1), CoreMatchers.is(IssueStatus.READY_FOR_TEST));
    }
}
