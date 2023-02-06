package ru.yandex.market.tsum.pipelines.checkout.jobs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.tsum.clients.juggler.EventStatus;
import ru.yandex.market.tsum.clients.juggler.JugglerApiClient;
import ru.yandex.market.tsum.clients.juggler.JugglerEvent;
import ru.yandex.market.tsum.clients.juggler.RawCompleteEvent;
import ru.yandex.market.tsum.clients.tankapi.StatusCode;
import ru.yandex.market.tsum.clients.tankapi.StatusJobResponse;
import ru.yandex.market.tsum.clients.tankapi.TankApiClient;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobActionsContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobProgressContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.SupportType;
import ru.yandex.market.tsum.pipe.engine.runtime.exceptions.JobManualFailException;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.Notificator;
import ru.yandex.market.tsum.pipelines.common.jobs.juggler.JugglerWatchJobConfig;
import ru.yandex.market.tsum.pipelines.common.jobs.tank.TankApiJobId;
import ru.yandex.market.tsum.pipelines.common.jobs.teamcity.MarketTeamcityBuildJobTest;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by aproskriakov on 12/7/21
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MarketTeamcityBuildJobTest.Config.class})
public class JugglerWatchTankAutostopJobTest {
    private static final String TEST_JUGGLER_URL = "http://testJugglerUrl";

    @Mock
    private JobContext jobContext;

    @Mock
    private Notificator notificator;

    @Mock
    private JobProgressContext jobProgressContext;

    @Mock
    private JobActionsContext jobActionsContext;

    @Spy
    private final JugglerWatchJobConfig jugglerWatchJobConfig = JugglerWatchJobConfig.newBuilder()
        .addHost("test_host1")
        .setStatuses(EventStatus.CRIT)
        .setWatchPeriodMillis(1, TimeUnit.SECONDS)
        .build();

    @Spy
    private final JugglerApiClient jugglerApiClient = Mockito.spy(new JugglerApiClient(
        "http://testApiUrl",
        TEST_JUGGLER_URL,
        "testToken",
        Mockito.mock(NettyHttpClientContext.class)));

    @Mock
    private final TankApiClient tankApiClient =
        Mockito.spy(new TankApiClient(Mockito.mock(NettyHttpClientContext.class)));

    @Spy
    private final List<TankApiJobId> tankApiJobIdList =
        Collections.singletonList(Mockito.mock(TankApiJobId.class));

    @InjectMocks
    private JugglerWatchTankAutostopJob jugglerWatchTankAutostopJob;

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

    @Test
    public void executeFoundCheckWithDesirableStatus() throws Exception {
        StatusJobResponse statusJobResponse = new StatusJobResponse();
        statusJobResponse.setStatusCode(StatusCode.RUNNING);
        when(tankApiClient.status(any(), any())).thenReturn(statusJobResponse);
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
        boolean thrownException = false;

        try {
            jugglerWatchTankAutostopJob.execute(jobContext);
        } catch (JobManualFailException e) {
            thrownException = true;
        }

        assertTrue(thrownException);
        verify(tankApiClient, times(1)).stop(anyList());
    }
}
