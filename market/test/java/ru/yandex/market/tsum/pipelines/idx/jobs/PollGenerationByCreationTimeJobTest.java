package ru.yandex.market.tsum.pipelines.idx.jobs;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.idx.IdxClient;
import ru.yandex.market.tsum.clients.idx.RecentGeneration;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.Notificator;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.MarketTeamcityBuildConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.MarketTeamcityBuildJobTest;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MarketTeamcityBuildJobTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PollGenerationByCreationTimeJobTest {
    @Mock
    private IdxClient idxClient;
    private OngoingStubbing<RecentGeneration> stubbing;

    @Mock
    private Notificator notificator;

    @Autowired
    private JobTester jobTester;

    private PollGenerationByCreationTimeJob.Config config;
    private PollGenerationByCreationTimeJob job;
    private TestJobContext context;

    private static final String MI_TYPE = "mitype";
    private static final String NEW_RELEASE = "new_release";

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final String BEFORE = DTF.format(NOW.minusMinutes(60));
    private static final String AFTER = DTF.format(NOW.plusMinutes(60));

    @Before
    public void setUp() {
        config = PollGenerationByCreationTimeJob.Config.builder()
            .withMitype(MI_TYPE)
            .withReleaseVersion(NEW_RELEASE)
            .withPollingTimeoutSeconds(3)
            .withPollingPeriodSeconds(1)
            .build();

        RecentGeneration firstGeneration = RecentGeneration.builder()
            .withName(BEFORE)
            .withReleaseVersion(NEW_RELEASE)
            .withSuccessful(false)
            .withCancelled(false)
            .build();

        idxClient = mock(IdxClient.class);
        stubbing = when(idxClient.getRecentGeneration(any(), any(), any(), any())).thenReturn(firstGeneration);

        notificator = mock(Notificator.class);

        job = jobTester
            .jobInstanceBuilder(PollGenerationByCreationTimeJob.class)
            .withBeans(idxClient, notificator)
            .withResources(
                MarketTeamcityBuildConfig.builder().withJobName("").build(),
                config
            )
            .create();

        context = getJobContext();
    }

    @Test
    public void acceptsSuccessfulGeneration() throws Exception {
        RecentGeneration successfulGeneration = RecentGeneration.builder()
            .withName(AFTER)
            .withReleaseVersion(NEW_RELEASE)
            .withSuccessful(true)
            .build();
        stubbing.thenReturn(successfulGeneration);

        job.execute(context);
    }

    @Test(expected = PollGenerationByCreationTimeJob.FailedGenerationException.class)
    public void rejectsFailedGeneration() throws Exception {
        RecentGeneration failedGeneration = RecentGeneration.builder()
            .withName(AFTER)
            .withReleaseVersion(NEW_RELEASE)
            .withSuccessful(false)
            .build();
        stubbing.thenReturn(failedGeneration);
        try {
            job.execute(context);
        } catch (RuntimeException exc) {
            throw (Exception) exc.getCause();
        }
    }

    private static TestJobContext getJobContext() {
        JobState jobState = mock(JobState.class);
        when(jobState.getTitle()).thenReturn("Poll job title");

        TestJobContext jobContext = new TestJobContext();
        jobContext.setJobStateMock(jobState);
        jobContext.setPipeLaunchUrl("http://example.yandex.net/pipe");
        jobContext.setJobLaunchDetailsUrl("http://example.yandex.net/pipe/job");
        return jobContext;
    }
}
