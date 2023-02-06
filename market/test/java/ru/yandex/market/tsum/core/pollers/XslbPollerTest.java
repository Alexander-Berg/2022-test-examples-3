package ru.yandex.market.tsum.core.pollers;

import com.google.common.util.concurrent.Futures;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.tsum.clients.pollers.PollerOptions;
import ru.yandex.market.tsum.clients.pollers.XslbPoller;
import ru.yandex.market.tsum.clients.conductor.ConductorClient;
import ru.yandex.market.tsum.clients.xlsb.VirtualService;
import ru.yandex.market.tsum.clients.xlsb.XslbClient;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class XslbPollerTest {

    private static final Gson GSON = new Gson();
    private static Set<String> ipAddresses = new HashSet<>(Arrays.asList("2a02:6b8:0:3400::452"));
    private PollerOptions.PollerOptionsBuilder pollerOptions = new PollerOptions.PollerOptionsBuilder()
        .allowIntervalLessThenOneSecond(true)
        .interval(1, TimeUnit.MILLISECONDS);

    @Mock
    private ConductorClient conductorClient;

    @Mock
    private XslbClient xslbClient = new XslbClient();

    @Before
    public void setup() {
        String vService1 = "  {\n" +
            "    \"vip\": \"2a02:6b8:0:3400::452\",\n" +
            "    \"vport\": \"29327\",\n" +
            "    \"rsport\": \"29327\",\n" +
            "    \"method\": \"TUN\",\n" +
            "    \"proto\": \"TCP\",\n" +
            "    \"vsname\": \"guruass.tst.vs.market.yandex.net\"\n" +
            "  }\n";

        String vService2 = "  {\n" +
            "    \"vip\": \"2a02:6b8:0:3400::45d\",\n" +
            "    \"vport\": \"39001\",\n" +
            "    \"rsport\": \"39001\",\n" +
            "    \"method\": \"TUN\",\n" +
            "    \"proto\": \"TCP\",\n" +
            "    \"vsname\": \"checkouter.sand.tst.vs.market.yandex.net\"\n" +
            "  }\n";

        String vService3 = "  {\n" +
            "    \"vip\": \"2a02:6b8:0:3400::45d\",\n" +
            "    \"vport\": \"39011\",\n" +
            "    \"rsport\": \"39011\",\n" +
            "    \"method\": \"TUN\",\n" +
            "    \"proto\": \"TCP\",\n" +
            "    \"vsname\": \"checkouter.sand.tst.vs.market.yandex.net\"\n" +
            "  }\n";

        List<VirtualService> virtualServices = Arrays.asList(
            GSON.fromJson(vService1, VirtualService.class),
            GSON.fromJson(vService2, VirtualService.class),
            GSON.fromJson(vService3, VirtualService.class)
        );

        when(xslbClient.getBalancers(anyString(), anyInt())).thenReturn(virtualServices);
    }

    @Test
    public void pollReadyBalancerTest() throws Exception {
        when(conductorClient.getGroupsToHosts(anyString()))
            .thenReturn(Futures.immediateFuture(
                Arrays.asList("mslb01ht.market.yandex.net", "mslb01vt.market.yandex.net")
            ));

        List<String> servers = conductorClient.getGroupsToHosts("market_slb_searhc-testing").get();

        Set<String> actualyIpAddresses = new XslbPoller(pollerOptions, anyInt(), xslbClient)
            .pollReadyBalancer(servers, "guruass.tst.vs.market.yandex.net");
        assertEquals(ipAddresses, actualyIpAddresses);
    }
}