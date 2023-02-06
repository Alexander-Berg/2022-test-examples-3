package ru.yandex.market.tsum.pipelines.idx.jobs;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.conductor.ConductorBranch;
import ru.yandex.market.tsum.clients.idx.DeployInfo;
import ru.yandex.market.tsum.clients.idx.IdxClient;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipelines.idx.IdxUtils;
import ru.yandex.market.tsum.pipelines.idx.resources.Role;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JobTesterConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PollIdxDeployJobTest {
    @Mock
    private IdxClient idxClient;
    private OngoingStubbing<DeployInfo> stubbing;

    private PollIdxDeployJob.Config config;

    @Autowired
    private JobTester jobTester;

    private PollIdxDeployJob job;
    private JobContext context;
    private Role role;

    @Before
    public void setUp() {
        DeployInfo firstInfo = new DeployInfo(false, 1000);
        idxClient = mock(IdxClient.class);
        stubbing = when(idxClient.getDeployInfo(any(), any(), any()))
            .thenReturn(firstInfo);

        config = PollIdxDeployJob.Config
            .builder(IdxUtils.ConfigurationDcName.RESERVED, IdxUtils.ROLE, null)
            .withPollingPeriodSeconds(1)
            .withPollingRetryOnExceptionCount(0)
            .withPollingTimeoutSeconds(10)
            .build();

        role = new Role(
            new ru.yandex.market.tsum.clients.idx.Role(
                new ru.yandex.market.tsum.clients.idx.Role.Configuration("gibson", ConductorBranch.PRESTABLE),
                new ru.yandex.market.tsum.clients.idx.Role.Configuration("stratocaster", ConductorBranch.STABLE)
            )
        );

        job = jobTester.jobInstanceBuilder(PollIdxDeployJob.class)
            .withBean(idxClient)
            .withResources(
                config,
                role
            )
            .create();

        context = getJobContext();
    }

    @Test
    public void pollingWorks() throws Exception {
        DeployInfo successfulInfo = new DeployInfo(true, 0);
        stubbing.thenReturn(successfulInfo);

        job.execute(context);
    }

    private TestJobContext getJobContext() {
        JobState jobState = mock(JobState.class);

        TestJobContext jobContext = new TestJobContext();
        jobContext.setJobStateMock(jobState);
        return jobContext;
    }
}
