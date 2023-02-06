package ru.yandex.market.tsum.pipelines.common.jobs.health;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.market_health.MarketHealthClient;
import ru.yandex.market.tsum.clients.solomon.SolomonAccessChecker;
import ru.yandex.market.tsum.clients.solomon.models.SolomonAccessCheckResult;
import ru.yandex.market.tsum.clients.solomon.models.SolomonProjectAccessibility;
import ru.yandex.market.tsum.context.ReleaseJobContext;
import ru.yandex.market.tsum.context.TsumJobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobProgressContext;
import ru.yandex.market.tsum.pipelines.common.jobs.github.commit.GitHubCommitJob;
import ru.yandex.market.tsum.pipelines.common.jobs.solomon.SolomonProjectInfoResource;
import ru.yandex.market.tsum.pipelines.common.resources.AbcServiceResource;
import ru.yandex.market.tsum.pipelines.lcmp.resources.ComponentChangeRequest;
import ru.yandex.market.tsum.pipelines.lcmp.resources.ComponentSpecResource;
import ru.yandex.market.tsum.pipelines.sre.resources.ApplicationName;
import ru.yandex.market.tsum.release.dao.Release;

import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class CreateHttpMetricsHealthConfigsJobTest {
    private static final String TEST_APPLICATION = "TEST_APPLICATION";
    private static final String TEST_PROJECT = "TEST_PROJECT";
    private static final String HEALTH_URL = "https://health-testing.market.yandex-team.ru";
    private static final String LOGSHATTER_CONFIG = "logshatterConfigId";
    private static final String CLICKPHITE_CONFIG = "clickphiteConfigId";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    TsumJobContext jobContext;
    @Mock
    ApplicationName applicationName;
    @Mock
    SolomonProjectInfoResource solomonProjectInfoResource;
    @Mock
    AbcServiceResource abcService;
    @Mock
    SolomonAccessChecker solomonAccessChecker;
    @Mock
    private GitHubCommitJob.CommitContext commitContext;
    @Mock
    private MarketHealthClient marketHealthClient;
    @Mock
    private ComponentChangeRequest componentChangeRequest;
    @Mock
    private CreateHttpMetricsHealthConfigsJobConfig jobConfig;
    @InjectMocks
    private CreateHttpMetricsHealthConfigsJob createHttpMetricsHealthConfigsJob;

    @Before
    public void setupMocks() {
        Mockito.when(jobConfig.getApplicationName()).thenReturn(TEST_APPLICATION);
        Mockito.when(jobConfig.getSolomonProjectId()).thenReturn(TEST_PROJECT);
        Release release = Mockito.mock(Release.class);
        Mockito.when(release.getId()).thenReturn("test-release-id");

        ReleaseJobContext releaseJobContext = Mockito.mock(ReleaseJobContext.class);
        Mockito.when(releaseJobContext.getCurrent()).thenReturn(release);

        JobProgressContext progressContext = Mockito.mock(JobProgressContext.class);

        Mockito.when(jobContext.release()).thenReturn(releaseJobContext);
        Mockito.when(jobContext.getJobId()).thenReturn("test-job-id");
        Mockito.when(jobContext.progress()).thenReturn(progressContext);

        createHttpMetricsHealthConfigsJob.healthUrl = HEALTH_URL;
    }

    private void setupSolomonAccessCheckMock(SolomonProjectAccessibility accessibility, String projectId) {
        Mockito.when(solomonAccessChecker.checkProjectAccess(projectId))
            .thenReturn(new SolomonAccessCheckResult(projectId, accessibility, null));
    }

    @Test
    public void ensureSolomonApiRequestedWhenSettingUpCommitContext() throws Exception {
        HealthMonitoringConfigFactory.ApplicationInfo applicationInfo = getApplicationInfo();
        String solomonProjectId = applicationInfo.getSolomonProjectId();

        setupSolomonAccessCheckMock(SolomonProjectAccessibility.ACCESSIBLE, applicationInfo.getSolomonProjectId());

        createHttpMetricsHealthConfigsJob.execute(jobContext);

        Mockito.verify(solomonAccessChecker, Mockito.times(1))
            .checkProjectAccess(solomonProjectId);
    }

    @Test
    public void ensureJobFailsIfProjectNotFoundInSolomon() throws Exception {
        HealthMonitoringConfigFactory.ApplicationInfo applicationInfo = getApplicationInfo();
        String solomonProjectId = applicationInfo.getSolomonProjectId();

        setupSolomonAccessCheckMock(SolomonProjectAccessibility.NOT_FOUND, solomonProjectId);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(
            "Проекта TEST_PROJECT в solomon нет. Как его создать: " +
                "https://wiki.yandex-team.ru/market/development/health/solomonmonitorings/#1" +
                ".sozdatproektvsolomone");

        createHttpMetricsHealthConfigsJob.execute(jobContext);
    }

    HealthMonitoringConfigFactory.ApplicationInfo getApplicationInfo() {
        HealthMonitoringConfigFactory.ApplicationInfo applicationInfo =
            new HealthMonitoringConfigFactory.ApplicationInfo(TEST_APPLICATION, TEST_PROJECT);
        assertTrue(applicationInfo.getSolomonProjectId().equals(TEST_PROJECT));
        return applicationInfo;
    }

    private ComponentSpecResource getComponentSpecResource() {
        ComponentSpecResource componentSpecResource = new ComponentSpecResource();
        componentSpecResource.setMarketHealthInfo(new ComponentSpecResource.MarketHealthInfo(
            "project", LOGSHATTER_CONFIG, CLICKPHITE_CONFIG));
        return componentSpecResource;
    }
}
