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
import ru.yandex.market.tsum.clients.idx.DatacampClient;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.MarketTeamcityBuildJobTest;
import ru.yandex.market.tsum.pipelines.idx.resources.OffersMiningConfig;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MarketTeamcityBuildJobTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class OffersMiningJobTest {
    @Mock
    private DatacampClient datacampClient;

    @Mock
    private NettyHttpClient httpClient;

    @Autowired
    private JobTester jobTester;

    private OffersMiningJob job;
    private OffersMiningConfig config;
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

    private static OffersMiningConfig getJobConfig() {
        return OffersMiningConfig.builder()
            .withBalancer("localhost")
            .withBalancerPort(8181)
            .withSleepTimeoutSeconds(0)
            .withMiningWaitingTimeoutMinutes(0)
            .build();
    }

    @Before
    public void setUp() {
        config = getJobConfig();
        context = getJobContext();

        datacampClient = mock(DatacampClient.class);
        httpClient = mock(NettyHttpClient.class);
        datacampClient.setHttpClient(httpClient);

        when(datacampClient.remineAll(config.getBalancer(), config.getBalancerPort())).thenReturn(true);

        job = jobTester.jobInstanceBuilder(OffersMiningJob.class)
            .withBean(datacampClient)
            .withResources(config)
            .create();
    }

    @Test
    public void testOffersMining() throws Exception {
        job.execute(context);
    }
}
