package ru.yandex.market.tsum.pipelines.apps.jobs.percommit;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.teamcity.TeamcityBuilder;
import ru.yandex.market.tsum.clients.teamcity.TeamcityParams;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipelines.apps.MobilePlatform;
import ru.yandex.market.tsum.pipelines.apps.resources.AppConfigResource;
import ru.yandex.market.tsum.pipelines.apps.resources.MobileIssueResource;
import ru.yandex.market.tsum.pipelines.apps.resources.SortingCenterTargetResource;
import ru.yandex.market.tsum.pipelines.apps.resources.YandexBetaResource;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.TeamcityBuildConfig;
import ru.yandex.market.tsum.pipelines.common.resources.BranchRef;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SortingCenterPerCommitTeamcityBuildJobTest {
    @InjectMocks
    private SortingCenterPerCommitTeamcityBuildJob job = new SortingCenterPerCommitTeamcityBuildJob();
    @Mock
    private BranchRef branchRef;
    @Mock
    private MobileIssueResource issue;
    @Mock
    private SortingCenterTargetResource target;
    @Mock
    private TeamcityBuildConfig config;
    @Mock
    private TeamcityBuilder builder;
    @Mock
    private AppConfigResource appConfig;

    private final TestJobContext context = new TestJobContext();

    @Before
    public void setUp() {
        var mockJobLaunch = mock(JobLaunch.class);
        when(mockJobLaunch.getTriggeredBy()).thenReturn(null);
        when(context.getJobState().getLastLaunch()).thenReturn(mockJobLaunch);
        when(appConfig.getYandexBetaAppName()).thenReturn("app");
        when(appConfig.getPlatform()).thenReturn(mock(MobilePlatform.class));
        when(branchRef.getName()).thenReturn("feature/QUEUE-123");
        when(issue.getIssueKey()).thenReturn("QUEUE-123");
        when(target.toString()).thenReturn("ZEBRA");
    }

    @Test
    public void setsTeamcityParameters() {
        TeamcityBuildConfig teamcityBuildConfig = job.getTeamcityConfig(context);
        Map<String, Object> parameters = teamcityBuildConfig.toParametersMap();

        assertEquals("feature/QUEUE-123", teamcityBuildConfig.getBranchName());
        assertEquals("feature/QUEUE-123", parameters.get(TeamcityParams.BUILD_BRANCH.getParamName()));
        assertEquals("QUEUE-123-QA-ZEBRA", parameters.get("system.deploy.branch"));
    }

    @Test
    public void shouldProduceResources() throws Exception {
        job.execute(context);

        var yandexBetaResource = context.getResource(YandexBetaResource.class);
        assertEquals("QUEUE-123-QA-ZEBRA", yandexBetaResource.getBranch());
    }

    @Test
    public void shouldProduceResourcesForEmptyIssue() throws Exception {
        when(issue.getIssueKey()).thenReturn(null);
        when(branchRef.getName()).thenReturn("master");

        job.execute(context);

        var yandexBetaResource = context.getResource(YandexBetaResource.class);
        assertEquals("master-QA-ZEBRA", yandexBetaResource.getBranch());
    }
}
