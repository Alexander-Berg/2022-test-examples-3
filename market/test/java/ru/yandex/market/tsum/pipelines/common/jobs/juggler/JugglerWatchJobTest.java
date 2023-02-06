package ru.yandex.market.tsum.pipelines.common.jobs.juggler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.tsum.clients.juggler.EventStatus;
import ru.yandex.market.tsum.clients.juggler.JugglerApiClient;
import ru.yandex.market.tsum.clients.juggler.JugglerEvent;
import ru.yandex.market.tsum.clients.juggler.RawCompleteEvent;
import ru.yandex.market.tsum.clients.startrek.StartrekCommentNotification;
import ru.yandex.market.tsum.core.utils.InstantUtils;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobActionsContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobProgressContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.JobProgressContextImpl;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.JobProgressContextImpl.ProgressBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.SupportType;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobProgress;
import ru.yandex.market.tsum.pipe.engine.runtime.exceptions.JobManualFailException;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.Notificator;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.TaskState;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.MarketTeamcityBuildJobTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Andrey Trubachev <a href="mailto:d3rp@yandex-team.ru"></a>
 * @date 09/07/2017
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MarketTeamcityBuildJobTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class JugglerWatchJobTest {

    private static final String TEST_JUGGLER_URL = "http://testJugglerUrl";
    @Mock
    private JobContext jobContext;
    @Spy
    private JugglerApiClient jugglerApiClient = Mockito.spy(new JugglerApiClient(
        "http://testApiUrl",
        TEST_JUGGLER_URL,
        "testToken",
        Mockito.mock(NettyHttpClientContext.class)));

    @Mock
    private Notificator notificator;

    @Mock
    private JobProgressContext jobProgressContext;

    @Mock
    private JobActionsContext jobActionsContext;

    @Spy
    private JugglerWatchJobConfig jugglerWatchJobConfig = JugglerWatchJobConfig.newBuilder()
        .addHost("test_host1")
        .setStatuses(EventStatus.CRIT)
        .setWatchPeriodMillis(1, TimeUnit.SECONDS)
        .build();

    @InjectMocks
    private JugglerWatchJob jugglerWatchJob;

    private static Date createDate(int year, int month, int day) {
        return new Date(year - 1900, month - 1, day);
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(jobContext.notifications()).thenReturn(notificator);
        when(jobContext.progress()).thenReturn(jobProgressContext);
        when(jobContext.actions()).thenReturn(jobActionsContext);
        doAnswer(mock -> {
            throw new JobManualFailException("", Collections.singletonList(SupportType.NONE));
        }).when(jobActionsContext).failJob(Mockito.anyString(), eq(SupportType.NONE));
        doAnswer(mock -> {
            throw new JobManualFailException("", Collections.singletonList(SupportType.NONE));
        }).when(jobActionsContext).failJob(Mockito.anyString(), anyList());
    }

    @Test(expected = JobManualFailException.class)
    public void executeFoundCheckWithDesirableStatus() throws Exception {
        doReturn(new ArrayList<>(
            Arrays.asList(
                new JugglerEvent(
                    "test_host1",
                    "test_service1",
                    new JugglerEvent.Status(EventStatus.CRIT, new Date()),
                    new Date(),
                    new RawCompleteEvent.DescriptionWithDate(
                        new RawCompleteEvent.Description(""),
                        new Date()
                    )
                ),
                new JugglerEvent(
                    "test_host1",
                    "test_service2",
                    new JugglerEvent.Status(EventStatus.OK, new Date()),
                    new Date(),
                    new RawCompleteEvent.DescriptionWithDate(
                        new RawCompleteEvent.Description(""),
                        new Date()
                    )
                )
            ))).when(jugglerApiClient).getEvents(any());

        jugglerWatchJob.execute(jobContext);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void writeTimePointUrlOnCriticalEventFound() {
        String expectedHost = "test_host1";
        String expectedService = "test_service1";

        JobProgress progress = new JobProgress();
        ProgressBuilder progressBuilder = JobProgressContextImpl.builder(progress);

        doReturn(Collections.singletonList(new JugglerEvent(
            expectedHost,
            expectedService,
            new JugglerEvent.Status(EventStatus.CRIT, new Date()),
            new Date(),
            new RawCompleteEvent.DescriptionWithDate(
                new RawCompleteEvent.Description(""),
                new Date()
            )
        ))).when(jugglerApiClient).getEvents(any());

        doCallRealMethod().when(jugglerApiClient).getTimePointUrl(anyString(), anyString());

        doAnswer(invocation -> {
            Function<ProgressBuilder, ProgressBuilder> updater
                = invocation.getArgument(0);
            updater.apply(progressBuilder);
            return null;
        })
            .when(jobProgressContext).update(any(Function.class));

        Exception expectedException = null;
        try {
            jugglerWatchJob.execute(jobContext);
        } catch (Exception e) {
            expectedException = e;
        }
        Assert.assertNotNull(expectedException);
        Assert.assertThat(expectedException, Matchers.instanceOf(JobManualFailException.class));
        assertEquals(1, progress.getTaskStates().size());

        TaskState criticalState = progress.getTaskStates().get(Module.JUGGLER.name() + 0);

        assertNotNull(criticalState);
        assertEquals(criticalState.getStatus(), TaskState.TaskStatus.FAILED);

        UriComponents criticalStateUri = UriComponentsBuilder.fromUriString(criticalState.getUrl()).build();
        assertEquals(criticalStateUri.getScheme() + "://" + criticalStateUri.getHost(), TEST_JUGGLER_URL);
        assertEquals(1, criticalStateUri.getPathSegments().size());
        assertEquals("check_details", criticalStateUri.getPathSegments().get(0));

        MultiValueMap<String, String> queryParams = criticalStateUri.getQueryParams();

        assertEquals(expectedHost, queryParams.getFirst("host"));
        assertEquals(expectedService, queryParams.getFirst("service"));

        String lastParameterValue = queryParams.getFirst("last");

        assertEquals("1DAY", lastParameterValue);
        assertNotNull(queryParams.getFirst("before"));
    }

    @Test(expected = JobManualFailException.class)
    public void failsWhenChecksNotFound() throws Exception {
        doReturn(Collections.emptyList())
            .when(jugglerApiClient).getEvents(any());

        jugglerWatchJob.execute(jobContext);
    }

    @Test
    public void calculateProgressRatio() throws Exception {
        long watchingTimeMillis = 1000;
        Instant startDate = new Date().toInstant();
        Instant halfDate = new Date(startDate.toEpochMilli() + (watchingTimeMillis / 2)).toInstant();
        Instant finishDate = new Date(startDate.toEpochMilli() + watchingTimeMillis).toInstant();
        assertEquals(1.0f, JugglerWatchJob.calculateProgressRatio(startDate, finishDate, watchingTimeMillis), 0);
        assertEquals(0.0f, JugglerWatchJob.calculateProgressRatio(startDate, startDate, watchingTimeMillis), 0);
        assertEquals(0.5f, JugglerWatchJob.calculateProgressRatio(startDate, halfDate, watchingTimeMillis), 0);
    }

    @Test
    public void calculateRemainingTimeMillis() throws Exception {
        long watchingTimeMillis = 60000;
        Instant startDate = new Date().toInstant();
        Instant halfDate = new Date(startDate.toEpochMilli() + (watchingTimeMillis / 2)).toInstant();
        Instant finishDate = new Date(startDate.toEpochMilli() + watchingTimeMillis).toInstant();
        assertEquals(
            60000,
            InstantUtils.calculateRemainingTimeMillis(startDate, startDate, watchingTimeMillis)
        );
        assertEquals(
            0,
            InstantUtils.calculateRemainingTimeMillis(startDate, finishDate, watchingTimeMillis)
        );
        assertEquals(
            30000,
            InstantUtils.calculateRemainingTimeMillis(startDate, halfDate, watchingTimeMillis)
        );
        assertEquals(
            0,
            InstantUtils.calculateRemainingTimeMillis(startDate, finishDate, 0)
        );
        assertEquals(
            0,
            InstantUtils.calculateRemainingTimeMillis(startDate, finishDate, 1)
        );
    }

    @Test
    public void startrekNotification() throws Exception {
        JobState jobState = mock(JobState.class);
        when(jobState.getTitle()).thenReturn("Juggler Watch Job Title");
        when(jobContext.getJobState()).thenReturn(jobState);
        when(jobContext.getPipeLaunchUrl()).thenReturn("https://example.yandex.net/pipe");
        when(jobContext.getJobLaunchDetailsUrl()).thenReturn("https://example.yandex.net/pipe/job");

        List<JugglerEvent> events = Arrays.asList(
            new JugglerEvent(
                "foo.yandex.net",
                "robot",
                new RawCompleteEvent.Status(EventStatus.OK, createDate(2018, 8, 12)),
                new Date(),
                null
            ),
            new JugglerEvent(
                "foo.yandex.net",
                "indexer",
                new RawCompleteEvent.Status(EventStatus.WARN, createDate(2018, 8, 12)),
                new Date(),
                null
            ),
            new JugglerEvent(
                "foo.yandex.net",
                "publisher",
                new RawCompleteEvent.Status(EventStatus.CRIT, createDate(2018, 8, 12)),
                new Date(),
                null
            )
        );
        StartrekCommentNotification notification =
            JugglerWatchJobNotifications.createStartrekNotification(jobContext, events);

        String expectedComment = "" +
            "**Juggler Watch Job Title:** завершены проверки в Juggler:\n" +
            "\n" +
            "- !!(green)**robot** (foo.yandex.net @ 2018-08-12 00:00:00)!!\n" +
            "- !!(yellow)**indexer** (foo.yandex.net @ 2018-08-12 00:00:00)!!\n" +
            "- !!(red)**publisher** (foo.yandex.net @ 2018-08-12 00:00:00)!!\n" +
            "\n" +
            "((https://example.yandex.net/pipe Перейти к пайплайну))\n" +
            "((https://example.yandex.net/pipe/job Перейти к пайплайн задаче))";

        assertEquals(
            expectedComment,
            notification.getStartrekComment()
        );
    }
}
