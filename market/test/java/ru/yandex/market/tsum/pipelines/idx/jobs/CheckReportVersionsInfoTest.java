package ru.yandex.market.tsum.pipelines.idx.jobs;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.request.netty.NettyHttpClient;
import ru.yandex.market.tsum.clients.idx.ReportClient;
import ru.yandex.market.tsum.clients.idx.bean.ReportVersionInfoResponse;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.MarketTeamcityBuildJobTest;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;
import ru.yandex.market.tsum.pipelines.idx.resources.VersionsInfoConfig;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MarketTeamcityBuildJobTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CheckReportVersionsInfoTest {
    private static final String INDEXER_RELEASE_VERSION = "3.2.1";
    private static final String INDEXER_RELEASE_VERSION_UNDER_REPORT = "3.2.1.0";
    private static final String INDEXER_RELEASE_TICKET = "MARKETINDEXER-0000";

    @Mock
    private ReportClient reportClient;

    @Mock
    private NettyHttpClient httpClient;

    @Autowired
    private JobTester jobTester;

    private CheckReportVersionsInfoJob job;
    private VersionsInfoConfig config;
    private ReleaseInfo releaseInfo;
    private TestJobContext context;

    private static TestJobContext getJobContext() {
        JobState jobState = mock(JobState.class);
        when(jobState.getLastLaunch()).thenReturn(new JobLaunch(1, null, null, null));

        TestJobContext jobContext = new TestJobContext();
        jobContext.setJobStateMock(jobState);
        jobContext.setPipeLaunchUrl("http://example.yandex.net/pipe");
        jobContext.setJobLaunchDetailsUrl("http://example.yandex.net/pipe/job");
        return jobContext;
    }

    private static VersionsInfoConfig getJobConfig() {
        return VersionsInfoConfig.builder()
            .withBalancer("localhost")
            .withBalancerPort(8181)
            .withPollingPeriodSeconds(1)
            .withPollingRetryOnExceptionCount(1)
            .withPollingTimeoutSeconds(1)
            .withSleepTimeoutSeconds(0)
            .build();
    }

    private static ReleaseInfo getReleaseInfo() {
        return new ReleaseInfo(new FixVersion(0, INDEXER_RELEASE_VERSION), INDEXER_RELEASE_TICKET);
    }

    @Before
    public void setUp() {
        config = getJobConfig();
        releaseInfo = getReleaseInfo();
        context = getJobContext();

        reportClient = mock(ReportClient.class);
        httpClient = mock(NettyHttpClient.class);
        reportClient.setHttpclient(httpClient);

        ReportVersionInfoResponse response = new ReportVersionInfoResponse();
        response.setMarketIndexerVersion(INDEXER_RELEASE_VERSION_UNDER_REPORT);

        when(reportClient.getVersionInfo(config.getBalancer(), config.getBalancerPort())).thenReturn(response);

        job = jobTester.jobInstanceBuilder(CheckReportVersionsInfoJob.class)
            .withBean(reportClient)
            .withResources(
                config,
                releaseInfo
            )
            .create();
    }

    @Test
    public void checkReportVersionsInfoTest() throws Exception {
        job.execute(context);
    }

}
