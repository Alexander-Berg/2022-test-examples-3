package ru.yandex.market.tsum.pipelines.common.jobs.nanny;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import nanny.tickets.Releases;
import nanny.tickets.Tickets;
import nanny.tickets_api.TicketsApi;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.tsum.clients.nanny.NannyClient;
import ru.yandex.market.tsum.clients.nanny.NannyTicketApiClient;
import ru.yandex.market.tsum.clients.pollers.Poller;
import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.sandbox.SandboxReleaseType;
import ru.yandex.market.tsum.clients.sandbox.SandboxTask;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.LaunchJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.common.resources.SandboxTaskId;

import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.tsum.pipelines.common.jobs.nanny.NannyReleaseJobNotifications.DEPLOY_SUCCEEDED;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 23.08.17
 */
public class NannyReleaseJobTest {
    @Mock
    private NannyClient nannyClient;

    @Mock
    private SandboxClient sandboxClient;

    @Mock
    private NannyTicketApiClient nannyTicketApiClient;

    @InjectMocks
    NannyReleaseTestJob sut;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void finishImmediatelyIfNoMatchingResourceTypePassed() throws Exception {
        sut.setConfig(
            NannyReleaseJobConfig.builder(SandboxReleaseType.TESTING)
                .withSandboxResourceType("requiredResourceType")
                .build()
        );

        sut.setSandboxTaskIds(
            Collections.singletonList(new SandboxTaskId("type1", 1L, "someResourceType"))
        );

        sut.execute(new TestJobContext());
        Mockito.verifyZeroInteractions(sandboxClient);
    }

    @Test
    public void pollSandboxResourceRelease() throws Exception {
        sut.setConfig(
            NannyReleaseJobConfig.builder(SandboxReleaseType.TESTING).build()
        );
        sut.setTsumSandboxUrl("https://sandbox.yandex-team.ru");

        long id = 1;
        SandboxTaskId taskId = new SandboxTaskId("testType", id, "testResourceType");

        Mockito.when(sandboxClient.getTask(taskId.getId()))
            .thenReturn(new SandboxTask("testType", id, SandboxTask.SANDBOX_SUCCESS_STATUS))
            .thenReturn(new SandboxTask("testType", id, SandboxTask.SANDBOX_RELEASING_STATUS))
            .thenReturn(new SandboxTask("testType", id, SandboxTask.SANDBOX_RELEASED_STATUS));

        sut.pollSandboxResourceRelease(new TestJobContext(), taskId);

        Mockito.verify(sandboxClient, Mockito.times(3)).getTask(id);
    }

    private static class NannyReleaseTestJob extends NannyReleaseJob {
        @Override
        protected Poller.PollerBuilder<SandboxTask> createPoller() {
            return super.createPoller().allowIntervalLessThenOneSecond(true).interval(0, TimeUnit.MILLISECONDS);
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("c4c557d2-1742-4684-ac9f-98b7ed6f8254");
        }
    }

    TicketsApi.FindReleasesResponse prepareAnswer() {
        return TicketsApi.FindReleasesResponse.newBuilder()
            .addValue(Releases.Release.newBuilder()
                .setStatus(Releases.ReleaseStatus.newBuilder()
                    .setPostProcessing(Releases.PostProcessingStatus.newBuilder()
                        .setIsFinished(true)
                        .build())
                    .build())
                .build()
            )
            .build();
    }

    @Test
    public void redeploySimple() throws Exception {
        sut.setConfig(
            NannyReleaseJobConfig.builder(SandboxReleaseType.TESTING)
                .withSandboxResourceType("requiredResourceType")
                .withRedeploy(true)
                .build()
        );
        sut.setTsumSandboxUrl("https://sandbox.yandex-team.ru");
        sut.setTsumNannyUrl("https://nanny-dev.yandex-team.ru");

        sut.setSandboxTaskIds(
            Collections.singletonList(new SandboxTaskId("type1", 1L, "requiredResourceType"))
        );

        long id = 1;
        SandboxTaskId taskId = new SandboxTaskId("testType", id, "testResourceType");

        Mockito.when(sandboxClient.getTask(taskId.getId()))
            .thenReturn(new SandboxTask("testType", id, SandboxTask.SANDBOX_SUCCESS_STATUS))
            .thenReturn(new SandboxTask("testType", id, SandboxTask.SANDBOX_RELEASING_STATUS))
            .thenReturn(new SandboxTask("testType", id, SandboxTask.SANDBOX_RELEASED_STATUS));

        Mockito.when(nannyTicketApiClient.findReleases(any())).thenReturn(prepareAnswer());
        Mockito.when(nannyTicketApiClient.findTickets(any())).thenReturn(prepareTicketsAnswer());
        Mockito.when(nannyTicketApiClient.commitTicket(any(), any())).thenReturn(null);
        Mockito.when(nannyTicketApiClient.getTicket((String) any()))
            .thenReturn(TicketsApi.GetTicketResponse.newBuilder().build());
        Mockito.when(nannyTicketApiClient.getRelease((String) any())).thenReturn(releaseAnswer());

        LaunchJobContext context = new TestJobContext();
        sut.execute(context);

        Mockito.verify(context.notifications(), Mockito.times(1))
            .notifyAboutEvent(ArgumentMatchers.argThat(event -> event.getEventMeta() == DEPLOY_SUCCEEDED), any());
    }

    @Test
    public void redeployRecipe() throws Exception {
        sut.setConfig(
            NannyReleaseJobConfig.builder(SandboxReleaseType.TESTING)
                .withSandboxResourceType("requiredResourceType")
                .withRedeploy(true)
                .withRecipeId("1")
                .build()
        );
        sut.setTsumSandboxUrl("https://sandbox.yandex-team.ru");
        sut.setTsumNannyUrl("https://nanny-dev.yandex-team.ru");

        sut.setSandboxTaskIds(
            Collections.singletonList(new SandboxTaskId("type1", 1L, "requiredResourceType"))
        );

        long id = 1;
        SandboxTaskId taskId = new SandboxTaskId("testType", id, "testResourceType");

        Mockito.when(sandboxClient.getTask(taskId.getId()))
            .thenReturn(new SandboxTask("testType", id, SandboxTask.SANDBOX_SUCCESS_STATUS))
            .thenReturn(new SandboxTask("testType", id, SandboxTask.SANDBOX_RELEASING_STATUS))
            .thenReturn(new SandboxTask("testType", id, SandboxTask.SANDBOX_RELEASED_STATUS));

        Mockito.when(nannyTicketApiClient.findReleases(any())).thenReturn(prepareAnswer());
        Mockito.when(nannyTicketApiClient.findTickets(any())).thenReturn(prepareTicketsAnswer());
        Mockito.when(nannyTicketApiClient.commitTicket(any(), any())).thenReturn(null);
        Mockito.when(nannyTicketApiClient.getTicket((String) any()))
            .thenReturn(TicketsApi.GetTicketResponse.newBuilder().build());
        Mockito.when(nannyTicketApiClient.getRelease((String) any())).thenReturn(releaseAnswer());

        LaunchJobContext context = new TestJobContext();
        sut.execute(context);

        Mockito.verify(context.notifications(), Mockito.times(1))
            .notifyAboutEvent(ArgumentMatchers.argThat(event -> event.getEventMeta() == DEPLOY_SUCCEEDED), any());
    }

    private TicketsApi.GetReleaseResponse releaseAnswer() {
        return TicketsApi.GetReleaseResponse.newBuilder().setValue(
            Releases.Release.newBuilder().setStatus(Releases.ReleaseStatus.newBuilder()
                .setStatus(Releases.ReleaseStatus.Status.CLOSED)
                .build()
            ).build()
        ).build();
    }

    private TicketsApi.FindTicketsResponse prepareTicketsAnswer() {
        return TicketsApi.FindTicketsResponse.newBuilder()
            .addValue(Tickets.Ticket.newBuilder().build())
            .setTotal(1)
            .build();
    }

}
