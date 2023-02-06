package ru.yandex.market.tsum.tms.tasks.servers_counting;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.tsum.clients.conductor.ConductorClient;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 28.08.18
 */
public class WindowsServerProviderTest {
    @Test
    public void WindowsServerProviderMockito() throws ExecutionException, InterruptedException {
        Collection<String> windowsStableConductorGroups = Collections.singletonList("market_windows-stable");
        Collection<String> windowsTestingConductorGroups = Collections.singletonList("market_windows-testing");
        Collection<String> windowsUnstableConductorGroups = Collections.singletonList("market_windows-dev");

        ListenableFuture<List<String>> serverGroup1 = Futures.immediateFuture(Arrays.asList("group1", "group2"));
        ListenableFuture<List<String>> unstableServers = Futures.immediateFuture(Collections.singletonList("server3"));
        ListenableFuture<List<String>> emptyList = Futures.immediateFuture(Collections.emptyList());

        ConductorClient conductorClient = Mockito.mock(ConductorClient.class);
        Mockito.when(conductorClient.getGroupsToHosts("market_windows-stable")).thenReturn(emptyList);
        Mockito.when(conductorClient.getGroupsToHosts("market_windows-testing")).thenReturn(emptyList);
        Mockito.when(conductorClient.getGroupsToHosts("market_windows-dev")).thenReturn(unstableServers);
        Mockito.when(conductorClient.getHostToGroups("server3")).thenReturn(serverGroup1);

        WindowsServerProvider windowsServerProvider = new WindowsServerProvider(conductorClient,
            windowsStableConductorGroups, windowsTestingConductorGroups, windowsUnstableConductorGroups);

        List<ServerDescription> serverDescriptions = windowsServerProvider.getServers();
        Assert.assertEquals("server3", serverDescriptions.get(0).getName());
        Assert.assertEquals("group1", serverDescriptions.get(0).getConductorGroups().get(0));
    }
}
