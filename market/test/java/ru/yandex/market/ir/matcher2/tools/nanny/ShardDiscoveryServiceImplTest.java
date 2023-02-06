package ru.yandex.market.ir.matcher2.tools.nanny;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ShardDiscoveryServiceImplTest {

    private ShardDiscoveryServiceImpl shardDiscoveryService;
    private HttpClient client = Mockito.mock(HttpClient.class);

    @Before
    public void setUp() {
        shardDiscoveryService = new ShardDiscoveryServiceImpl(client, "shard_name");
    }

    @Test
    public void getMiniclusterInstances() throws IOException, InterruptedException {
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        final String responseNanny = Files.readString(Paths.get(getAbsolutePath("/nanny_response.json")));
        when(response.body()).thenReturn(responseNanny);
        when(response.statusCode()).thenReturn(200);
        when(client.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(response);

        final List<NannyInstance> miniclusterInstances = shardDiscoveryService.getMiniclusterInstances(2);

        verify(client).send(captor.capture(), eq(HttpResponse.BodyHandlers.ofString()));
        assertEquals("/v2/services/shard_name/current_state/instances/", captor.getValue().uri().getPath());

        assertEquals(4, miniclusterInstances.size());
        final List<String> nannyHost = miniclusterInstances.stream()
                .map(nannyInstance -> nannyInstance.getHost() + ":" + nannyInstance.getPort())
                .collect(Collectors.toList());
        assertTrue(nannyHost.contains("sas4-6780-675-sas-market-test--b0c-16769.gencfg-c.yandex.net:16769"));
        assertTrue(nannyHost.contains("sas1-9641-sas-market-test-skut-b0c-16769.gencfg-c.yandex.net:16769"));
        assertTrue(nannyHost.contains("sas4-6772-039-sas-market-test--b0c-16769.gencfg-c.yandex.net:16769"));
        assertTrue(nannyHost.contains("sas4-6775-cba-sas-market-test--b0c-16769.gencfg-c.yandex.net:16769"));
    }


    private String getAbsolutePath(String path) {
        return new File(ShardDiscoveryServiceImplTest.class.getResource(path).getPath()).getAbsolutePath();
    }
}
