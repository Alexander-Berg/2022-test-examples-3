package ru.yandex.market.tsum.pipelines.wms.jobs;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
import ru.yandex.market.tsum.pipe.engine.definition.context.JobActionsContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobProgressContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.JobProgressContextImpl;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.JobProgressContextImpl.ProgressBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.SupportType;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobProgress;
import ru.yandex.market.tsum.pipe.engine.runtime.exceptions.JobManualFailException;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.Notificator;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.TaskState;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.MarketTeamcityBuildJobTest;
import ru.yandex.market.tsum.pipelines.wms.resources.WmsJugglerWaitAllGreenParams;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MarketTeamcityBuildJobTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class WmsJugglerAllGreenJobTest {
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
    private WmsJugglerWaitAllGreenParams jugglerWatchJobParams = WmsJugglerWaitAllGreenParams.builder()
        .addHost("test_host1")
        .setMaxWait(1)
        .build();

    @InjectMocks
    private WmsJugglerWaitAllGreenJob jugglerJob;

    private static Date createDate(int year, int month, int day) {
        return new Date(year - 1900, month - 1, day);
    }

    private static JugglerEvent createJugglerEvent(String host, String service, EventStatus status) {
        return new JugglerEvent(
            host,
            service,
            new JugglerEvent.Status(status, new Date()),
            new Date(),
            new RawCompleteEvent.DescriptionWithDate(
                new RawCompleteEvent.Description(""),
                new Date()
            )
        );
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
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
    public void executeNotAllChecksGreenInTimeout() throws Exception {
        doReturn(new ArrayList<>(
            Arrays.asList(
                createJugglerEvent("test_host1", "test_service1", EventStatus.CRIT),
                createJugglerEvent("test_host1", "test_service2", EventStatus.OK)
            ))).when(jugglerApiClient).getEvents(any());

        jugglerJob.execute(jobContext);
    }

    @Test
    public void executeAllChecksGreen() throws Exception {
        doReturn(Collections.singletonList(
            createJugglerEvent("test_host1", "test_service1", EventStatus.OK)
        )).when(jugglerApiClient).getEvents(any());

        jugglerJob.execute(jobContext);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void writeTimePointUrlOnCriticalEventFound() {
        String expectedHost = "test_host1";
        String expectedService = "test_service1";

        JobProgress progress = new JobProgress();
        ProgressBuilder progressBuilder = JobProgressContextImpl.builder(progress);

        doReturn(Collections.singletonList(
            createJugglerEvent(expectedHost, expectedService, EventStatus.CRIT)
        )).when(jugglerApiClient).getEvents(any());

        doCallRealMethod().when(jugglerApiClient).getTimePointUrl(anyString(), anyString());

        doAnswer(invocation -> {
            Function<ProgressBuilder, ProgressBuilder> updater = invocation.getArgument(0);
            updater.apply(progressBuilder);
            return null;
        }).when(jobProgressContext).update(any(Function.class));

        Exception expectedException = null;
        try {
            jugglerJob.execute(jobContext);
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

        jugglerJob.execute(jobContext);
    }

    @Test
    public void calculateProgressRatio() throws Exception {
        long watchingTimeMillis = 1000;
        Instant startDate = new Date().toInstant();
        Instant halfDate = new Date(startDate.toEpochMilli() + (watchingTimeMillis / 2)).toInstant();
        Instant finishDate = new Date(startDate.toEpochMilli() + watchingTimeMillis).toInstant();
        assertEquals(1.0f, WmsJugglerWaitAllGreenJob.calculateProgressRatio(startDate, finishDate,
            watchingTimeMillis), 0);
        assertEquals(0.0f,
            WmsJugglerWaitAllGreenJob.calculateProgressRatio(startDate, startDate, watchingTimeMillis),
            0);
        assertEquals(0.5f, WmsJugglerWaitAllGreenJob.calculateProgressRatio(startDate, halfDate, watchingTimeMillis),
            0);
    }
}
