package ru.yandex.market.tsum.pipelines.idx.jobs;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
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
import ru.yandex.market.tsum.clients.startrek.StartrekCommentNotification;
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
public class PollGenerationJobTest {
    @Mock
    private IdxClient idxClient;
    private OngoingStubbing<RecentGeneration> stubbing;

    @Mock
    private Notificator notificator;

    @Autowired
    private JobTester jobTester;

    private PollGenerationJob.Config config;
    private PollGenerationJob job;
    private TestJobContext context;

    private static final String MI_TYPE = "mitype";
    private static final String NEW_RELEASE = "new_release";
    private static final String OLD_RELEASE = "old_release";

    @Before
    public void setUp() {
        config = PollGenerationJob.Config.builder()
            .withMitype(MI_TYPE)
            .withReleaseVersion(NEW_RELEASE)
            .withPollingTimeoutSeconds(3)
            .withPollingPeriodSeconds(1)
            .build();

        RecentGeneration firstGeneration = RecentGeneration.builder()
            .withName("foo")
            .withReleaseVersion(OLD_RELEASE + NEW_RELEASE)
            .withSuccessful(false)
            .withCancelled(false)
            .build();

        idxClient = mock(IdxClient.class);
        stubbing = when(idxClient.getRecentGeneration(any(), any(), any(), any())).thenReturn(firstGeneration);

        notificator = mock(Notificator.class);

        job = jobTester
            .jobInstanceBuilder(PollGenerationJob.class)
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
            .withName("bar")
            .withReleaseVersion(NEW_RELEASE + ".0")
            .withSuccessful(true)
            .build();
        stubbing.thenReturn(successfulGeneration);

        job.execute(context);
    }

    @Test(expected = PollGenerationJob.FailedGenerationException.class)
    public void rejectsFailedGeneration() throws Exception {
        RecentGeneration failedGeneration = RecentGeneration.builder()
            .withName("bar")
            .withReleaseVersion(NEW_RELEASE + ".1")
            .withSuccessful(false)
            .build();
        stubbing.thenReturn(failedGeneration);
        try {
            job.execute(context);
        } catch (RuntimeException exc) {
            throw (Exception) exc.getCause();
        }
    }

    @Test
    public void acceptsSuccessfulGenerationAfterCancelled() throws Exception {
        RecentGeneration cancelledGeneration = RecentGeneration.builder()
            .withName("cancelled")
            .withReleaseVersion(NEW_RELEASE + ".0")
            .withSuccessful(true)
            .withCancelled(true)
            .build();

        RecentGeneration successfulGeneration = RecentGeneration.builder()
            .withName("bar")
            .withReleaseVersion(NEW_RELEASE + ".0")
            .withSuccessful(true)
            .withCancelled(false)
            .build();

        stubbing.thenReturn(cancelledGeneration);
        stubbing.thenReturn(successfulGeneration);

        job.execute(context);
    }

    @Test
    public void startrekNotificationWorks() throws Exception {
        RecentGeneration generation = RecentGeneration.builder()
            .withName("generation_name")
            .withReleaseVersion("release_version")
            .build();

        StartrekCommentNotification notification = job.createStartrekNotification(
            context,
            generation
        );

        String expectedComment = "" +
            "**Poll job title:** собралось поколение generation_name с версией индексатора release_version.\n" +
            "\n" +
            "((http://example.yandex.net/pipe Перейти к пайплайну))\n" +
            "((http://example.yandex.net/pipe/job Перейти к пайплайн задаче))";

        MatcherAssert.assertThat(
            notification.getStartrekComment(),
            Matchers.equalTo(expectedComment)
        );
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
