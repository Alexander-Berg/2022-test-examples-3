package ru.yandex.market.tsum.pipelines.apps.jobs.percommit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.apps.resources.AppConfigResource;
import ru.yandex.market.tsum.pipelines.apps.resources.MobileIssueResource;
import ru.yandex.market.tsum.pipelines.common.resources.BranchRef;
import ru.yandex.market.tsum.pipelines.common.resources.PerCommitLaunchParams;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.model.Issue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SortingCenterPerCommitInitJobTest {
    @InjectMocks
    private SortingCenterPerCommitInitJob job = new SortingCenterPerCommitInitJob();
    @Mock
    private PerCommitLaunchParams perCommitLaunchParams;
    @Mock
    private AppConfigResource appConfigResource;
    @Mock
    private Issues issues;

    private final TestJobContext context = new TestJobContext();

    @Test
    public void shouldProduceResourcesForIssueBranch() throws Exception {
        when(perCommitLaunchParams.getBranch()).thenReturn("feature/QUEUE-123-add-something");
        when(appConfigResource.getStartrekQueueId()).thenReturn("QUEUE");
        var mockIssue = mock(Issue.class);
        when(mockIssue.getKey()).thenReturn("QUEUE-123");
        when(issues.get(eq("QUEUE-123"))).thenReturn(mockIssue);

        job.execute(context);

        var branchResource = context.getResource(BranchRef.class);
        assertEquals("feature/QUEUE-123-add-something", branchResource.getName());
        var mobileIssueResource = context.getResource(MobileIssueResource.class);
        assertEquals("QUEUE-123", mobileIssueResource.getIssueKey());
        AppConfigResource gotAppConfigResource = context.getResource(AppConfigResource.class);
        assertEquals("QUEUE", gotAppConfigResource.getStartrekQueueId());
    }

    @Test
    public void shouldProduceResourcesForSomeBranch() throws Exception {
        when(perCommitLaunchParams.getBranch()).thenReturn("master");
        when(appConfigResource.getStartrekQueueId()).thenReturn("QUEUE");

        job.execute(context);

        BranchRef branchResource = context.getResource(BranchRef.class);
        assertEquals("master", branchResource.getName());

        MobileIssueResource mobileIssueResource = context.getResource(MobileIssueResource.class);
        assertNull(mobileIssueResource.getIssueKey());

        AppConfigResource gotAppConfigResource = context.getResource(AppConfigResource.class);
        assertEquals("QUEUE", gotAppConfigResource.getStartrekQueueId());
    }
}
