package ru.yandex.market.tsum.pipelines.common.jobs.deploy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import ru.yandex.market.tsum.clients.yp.YandexDeployClient;
import ru.yandex.market.tsum.clients.yp.model.Condition;
import ru.yandex.market.tsum.clients.yp.model.DeployPatchAction;
import ru.yandex.market.tsum.clients.yp.model.DeployPatchActionType;
import ru.yandex.market.tsum.clients.yp.model.DeployTicketProgress;
import ru.yandex.market.tsum.clients.yp.model.DeployTicketStatus;
import ru.yandex.market.tsum.clients.yp.transport.TransportType;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;

import static org.mockito.ArgumentMatchers.any;

public class YandexDeployReleaseIntegrationJobTest {
    private final TestJobContext context = new TestJobContext();
    private final String ticketid = "ticket";
    private final Map<String, List<String>> mapReleaseTickets = Map.of(
        "release-1", List.of("ticket-1", "ticket-2"),
        "release-2", List.of("ticket-3", "ticket-4", "ticket-5")
    );

    @Mock
    private YandexDeployClient yandexDeployClient;

    @InjectMocks
    YandexDeployReleaseIntegrationJob job = new YandexDeployReleaseIntegrationJob();

    private DeployTicketStatus getDeployTicketStatusMock(DeployPatchActionType type) {
        TransportType transport = TransportType.YSON;
        return DeployTicketStatus.newBuilder(transport)
            .setAction(DeployPatchAction.newBuilder(transport)
                .setType(type)
                .build())
            .setProgress(DeployTicketProgress.newBuilder(transport)
                .setClosed(Condition.newBuilder(transport).build())
                .build())
            .build();
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testIsTicketNeedToCommitNew() throws ExecutionException, InterruptedException {
        Mockito.when(yandexDeployClient.getDeployTicketStatus(any(), any()))
            .thenReturn(getDeployTicketStatusMock(DeployPatchActionType.UNRECOGNIZED));

        Assert.assertTrue("Fail when skip new ticket", job.isTicketNeedToCommit(ticketid));
    }

    @Test
    public void testIsTicketNeedToCommitCommited() throws ExecutionException, InterruptedException {
        Mockito.when(yandexDeployClient.getDeployTicketStatus(any(), any()))
            .thenReturn(getDeployTicketStatusMock(DeployPatchActionType.DPAT_COMMIT));

        Assert.assertFalse("Fail when commit committed ticket", job.isTicketNeedToCommit(ticketid));
    }

    @Test
    public void testIsTicketNeedToCommitSkipped() throws ExecutionException, InterruptedException {
        Mockito.when(yandexDeployClient.getDeployTicketStatus(any(), any()))
            .thenReturn(getDeployTicketStatusMock(DeployPatchActionType.DPAT_SKIP));

        Assert.assertFalse("Fail when commit skipped ticket", job.isTicketNeedToCommit(ticketid));
    }

    @Test
    public void testPollDeployTickets() throws ExecutionException, InterruptedException, TimeoutException {
        Mockito.when(yandexDeployClient.getDeployTicketStatus(any(), any()))
            .thenReturn(getDeployTicketStatusMock(DeployPatchActionType.DPAT_COMMIT));
        job.setJobConfig(YandexDeployReleaseIntegrationJobConfig.builder()
            .withAutoCommitAllTickets(false)
            .withClosedTicketsPercent(50)
            .withPollerIntervalMinutes(1)
            .withPollerMaxExceptionRetryCount(1)
            .withWaitingTimeoutMinutes(1)
            .build()
        );

        job.pollDeployTickets(
            context,
            mapReleaseTickets,
            job.getInitialReleaseStates(new ArrayList<>(mapReleaseTickets.keySet()))
        );
    }
}
