package ru.yandex.market.tsum.pipelines.idx.jobs;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.idx.CheckGenerationResult;
import ru.yandex.market.tsum.clients.idx.IdxClient;
import ru.yandex.market.tsum.clients.startrek.StartrekCommentNotification;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.Notificator;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.MarketTeamcityBuildConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.MarketTeamcityBuildJobTest;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MarketTeamcityBuildJobTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CheckGenerationJobTest {
    private static final String INDEXER_RELAESE_VERSION = "3.2.1.0";
    private static final String INDEXER_RELAESE_TICKET = "MARKETINDEXER-0000";

    @Mock
    private IdxClient idxClient;

    @Mock
    private Notificator notificator;

    @Autowired
    private JobTester jobTester;

    private CheckGenerationJob.Config config;
    private CheckGenerationJob job;
    private TestJobContext context;

    private void mockChecking(CheckGenerationResult additionalResult) {
        CheckGenerationResult incompleteResult = new CheckGenerationResult(CheckGenerationResult.Status.INCOMPLETE);
        CheckGenerationResult publishingResult = new CheckGenerationResult(CheckGenerationResult.Status.PUBLISHING);
        when(idxClient.checkGenerationByVersion(any(), any(), any(), anyBoolean()))
            .thenReturn(incompleteResult)
            .thenReturn(publishingResult)
            .thenReturn(additionalResult);
    }

    @Before
    public void setUp() {
        config = CheckGenerationJob.Config.builder()
            .withServer(IdxClient.IdxApiServer.TESTING)
            .withBalancer(IdxClient.Balancer.TESTING)
            .withPollingPeriodSeconds(1)
            .build();

        idxClient = mock(IdxClient.class);
        notificator = mock(Notificator.class);

        job = jobTester.jobInstanceBuilder(CheckGenerationJob.class)
            .withBeans(idxClient, notificator)
            .withResources(
                MarketTeamcityBuildConfig.builder().withJobName("").build(),
                new ReleaseInfo(new FixVersion(0, INDEXER_RELAESE_VERSION), INDEXER_RELAESE_TICKET),
                config
            )
            .create();

        context = getJobContext();
    }

    @Test
    public void acceptsSuccessfulChecks() throws Exception {
        CheckGenerationResult successfulResult = new CheckGenerationResult(
            CheckGenerationResult.Status.PUBLISHED,
            Maps.toMap(
                Lists.newArrayList("foo", "bar"),
                name -> true
            )
        );
        mockChecking(successfulResult);

        job.execute(context);
    }

    @Test(expected = CheckGenerationJob.StaleGenerationException.class)
    public void rejectsStaleGeneration() throws Exception {
        CheckGenerationResult staleResult = new CheckGenerationResult(CheckGenerationResult.Status.STALE);
        mockChecking(staleResult);

        try {
            job.execute(context);
        } catch (RuntimeException exc) {
            throw (Exception) exc.getCause();
        }
    }

    @Test(expected = CheckGenerationJob.CorruptedGenerationException.class)
    public void rejectsCorruptedGeneration() throws Exception {
        CheckGenerationResult corruptedResult = new CheckGenerationResult(
            CheckGenerationResult.Status.PUBLISHED,
            Maps.toMap(
                Lists.newArrayList("foo", "bar"),
                name -> false
            )
        );

        mockChecking(corruptedResult);

        try {
            job.execute(context);
        } catch (RuntimeException exc) {
            throw (Exception) exc.getCause();
        }
    }

    @Test
    public void startrekNotificationWorks() {
        CheckGenerationResult result = new CheckGenerationResult(
            CheckGenerationResult.Status.PUBLISHED,
            Maps.toMap(
                Lists.newArrayList("check_bill", "check_ted"),
                name -> name.equals("check_ted")
            )
        );

        StartrekCommentNotification notification = job.createStartrekNotification(
            context,
            result
        );

        String expectedComment = "" +
            "**Check job title:** завершена раскладка поколения с версией индексатора " + INDEXER_RELAESE_VERSION +
            ".\n" +
            "\n" +
            "Статус поколения — **PUBLISHED**.\n" +
            "\n" +
            "Результаты проверок под репортом:\n" +
            "- !!check_bill — не прошла!!\n" +
            "- check_ted — прошла\n" +
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
        when(jobState.getTitle()).thenReturn("Check job title");

        TestJobContext jobContext = new TestJobContext();
        jobContext.setJobStateMock(jobState);
        jobContext.setPipeLaunchUrl("http://example.yandex.net/pipe");
        jobContext.setJobLaunchDetailsUrl("http://example.yandex.net/pipe/job");
        return jobContext;
    }
}
