package ru.yandex.market.tsum.tms.tasks.servers_counting;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.tsum.clients.bot.BotItemInfo;
import ru.yandex.market.tsum.clients.conductor.ConductorClient;
import ru.yandex.market.tsum.clients.conductor.ConductorPackageOnHost;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 08.09.17
 */
public class ConductorServerProviderTest {
    @Test
    public void ConductorServerProviderMockito() throws ExecutionException, InterruptedException {

        ListenableFuture<List<String>> allServers = Futures.immediateFuture(
            Arrays.asList("server1", "server2", "server3", "server4")
        );
        ListenableFuture<List<String>> serverGroup1 = Futures.immediateFuture(Arrays.asList("group1", "group2"));
        ListenableFuture<List<String>> serverGroup2 = Futures.immediateFuture(Arrays.asList("group2", "group3"));
        ListenableFuture<List<String>> stableServers = Futures.immediateFuture(Collections.singletonList("server1"));
        ListenableFuture<List<String>> testingServers = Futures.immediateFuture(Collections.singletonList("server2"));
        ListenableFuture<List<String>> ignoreServers = Futures.immediateFuture(Collections.singletonList("server3"));
        ListenableFuture<List<String>> emptyList = Futures.immediateFuture(Collections.emptyList());
        ListenableFuture<List<ConductorPackageOnHost>> packages = Futures.immediateFuture(
            Collections.singletonList(new ConductorPackageOnHost("yandex-market-app", "42.21"))
        );

        Map<String, BotItemInfo> baremetalServers = Stream.of("server1", "server2", "server6")
            .collect(Collectors.toMap(serverName -> serverName, ConductorServerProviderTest::getBotItemInfo));
        String conductorProject = "cs";
        List<String> ignoreConductorGroups = Collections.singletonList("cs_deb-ignore");

        ConductorClient conductorClient = Mockito.mock(ConductorClient.class);
        Mockito.when(conductorClient.getProjectHosts(conductorProject)).thenReturn(allServers);
        Mockito.when(conductorClient.getGroupsToHosts("cs_deb-stable")).thenReturn(stableServers);
        Mockito.when(conductorClient.getGroupsToHosts("cs_deb-testing")).thenReturn(testingServers);
        Mockito.when(conductorClient.getGroupsToHosts("cs_deb-ignore")).thenReturn(ignoreServers);
        Mockito.when(conductorClient.getGroupsToHosts("cs_deb-prestable")).thenReturn(emptyList);
        Mockito.when(conductorClient.getGroupsToHosts("cs_deb-unstable")).thenReturn(emptyList);
        Mockito.when(conductorClient.getGroupsToHosts("cs_deb-unknown")).thenReturn(emptyList);
        Mockito.when(conductorClient.getHostToGroups("server1")).thenReturn(serverGroup1);
        Mockito.when(conductorClient.getHostToGroups("server2")).thenReturn(serverGroup2);
        Mockito.when(conductorClient.getPackagesOnHost(Mockito.anyString())).thenReturn(packages);

        ConductorServerProvider conductorServerProvider = new ConductorServerProvider(
            conductorClient, baremetalServers, conductorProject, ignoreConductorGroups
        );

        List<ServerDescription> serverDescriptions = conductorServerProvider.getServers();

        Assert.assertEquals("server2", serverDescriptions.get(0).getName());
        Assert.assertEquals(Environment.TESTING, serverDescriptions.get(0).getEnvironment());
        Assert.assertEquals(Arrays.asList("group2", "group3"), serverDescriptions.get(0).getConductorGroups());
        Assert.assertEquals("server1", serverDescriptions.get(1).getName());
        Assert.assertEquals(Environment.STABLE, serverDescriptions.get(1).getEnvironment());
        Assert.assertEquals(Arrays.asList("group1", "group2"), serverDescriptions.get(1).getConductorGroups());
        Assert.assertEquals(Collections.singletonList("yandex-market-app"), serverDescriptions.get(0).getPackages());
    }

    private static BotItemInfo getBotItemInfo(String name) {
        BotItemInfo botItemInfo = new BotItemInfo();
        botItemInfo.setName(name);
        return botItemInfo;
    }
}
