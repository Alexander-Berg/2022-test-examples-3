package ru.yandex.chemodan.app.orchestrator;

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.orchestrator.cloud.ControlAgentClient;
import ru.yandex.chemodan.app.orchestrator.dao.Container;
import ru.yandex.chemodan.app.orchestrator.manager.OrchestratorControl;
import ru.yandex.misc.ip.HostPort;

/**
 * @author shirankov
 */
public class ControlAgentClientTests extends AbstractOrchestratorCoreTest {

    @Test
    public void waitContainerAliveRetries() throws IOException {
        ListF<Integer> delays = Cf.list(0,0,0);

        HttpClient httpClientMock = Mockito.mock(HttpClient.class);
        Mockito.when(httpClientMock.execute(Mockito.<HttpUriRequest>any(), Mockito.any(), Mockito.any()))
                .thenThrow(new SocketTimeoutException());

        OrchestratorControl orchestratorControl = Mockito.mock(OrchestratorControl.class);
        Mockito.when(orchestratorControl.getContainerWaitIsAliveChecksRetryIntervals())
                .thenReturn(delays);

        Container container = Mockito.mock(Container.class);
        Mockito.when(container.getPod()).thenReturn(new HostPort("host", 80));
        Mockito.when(container.getId()).thenReturn("id");

        ControlAgentClient client = new ControlAgentClient(httpClientMock, orchestratorControl, "office");

        boolean containerAlive = client.waitContainerAlive(container);
        assert !containerAlive;
        Mockito.verify(httpClientMock, Mockito.times(delays.length()+1))
                .execute(Mockito.<HttpUriRequest>any(), Mockito.any(), Mockito.any());
    }
}
