package ru.yandex.market.pricelabs.tms.processing;

import java.util.List;
import java.util.Map;

import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;
import ru.yandex.market.pricelabs.tms.ConfigurationForTests.MockWebServerControls;
import ru.yandex.market.pricelabs.tms.services.market_indexer.IndexerService;
import ru.yandex.market.yt.YtClusters;
import ru.yandex.market.yt.client.YtClientProxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexerClusterSourceTest extends AbstractTmsSpringConfiguration {

    @Autowired
    private IndexerService indexer;

    @Autowired
    @Qualifier("mockWebServerMarketIndexer")
    private MockWebServerControls mockWebServerMarketIndexer;

    private String indexer1;
    private String indexer2;

    @Mock
    private YtClientProxy proxy1;

    @Mock
    private YtClientProxy proxy2;

    private IndexerClusterSource source;


    @BeforeEach
    void init() {
        indexer1 = "indexer1";
        indexer2 = "indexer2";

        var cluster1 = "cluster1";
        var cluster2 = "cluster2";
        when(proxy1.getClusterName()).thenReturn(cluster1);
        when(proxy2.getClusterName()).thenReturn(cluster2);

        this.source = new IndexerClusterSource(indexer, new YtClusters(List.of(proxy1, proxy2)),
                Map.of(indexer1, cluster1, indexer2, cluster2), 60);
        this.source.updateCurrentCluster();
        mockWebServerMarketIndexer.cleanup();
    }

    @Test
    void testCurrentCluster() {

        this.source.updateCurrentCluster();
        assertEquals(new IndexerYtClientProxy(proxy1, indexer1), this.source.getCurrentCluster());

        this.source.updateCurrentCluster();
        assertEquals(new IndexerYtClientProxy(proxy1, indexer1), this.source.getCurrentCluster());

        // 2 запроса в целом
        checkRequest();
        checkRequest();
        mockWebServerMarketIndexer.checkNoMessages();
    }

    @Test
    void testDifferentCurrentCluster() {
        sendResponse(indexer2);

        this.source.updateCurrentCluster();
        assertEquals(new IndexerYtClientProxy(proxy2, indexer2), this.source.getCurrentCluster());

        this.source.updateCurrentCluster();
        assertEquals(new IndexerYtClientProxy(proxy2, indexer2), this.source.getCurrentCluster());

        // 2 запроса в целом (ответим только на первый)
        checkRequest();
        checkRequest();
        mockWebServerMarketIndexer.checkNoMessages();
    }

    @Test
    void testSwitchingCurrentCluster() {

        sendResponse(indexer2);
        this.source.updateCurrentCluster();
        assertEquals(new IndexerYtClientProxy(proxy2, indexer2), this.source.getCurrentCluster());

        sendResponse(indexer1);
        this.source.updateCurrentCluster();
        assertEquals(new IndexerYtClientProxy(proxy1, indexer1), this.source.getCurrentCluster());

        sendResponse(indexer2);
        this.source.updateCurrentCluster();
        assertEquals(new IndexerYtClientProxy(proxy2, indexer2), this.source.getCurrentCluster());

        // 2 запроса в целом (ответим только на первый)
        checkRequest();
        checkRequest();
        checkRequest();
        mockWebServerMarketIndexer.checkNoMessages();
    }

    @Test
    void testUnknownCluster() {
        sendResponse("indexer3");

        this.source.updateCurrentCluster();
        assertEquals(new IndexerYtClientProxy(proxy1, indexer1), this.source.getCurrentCluster());

        this.source.updateCurrentCluster();
        assertEquals(new IndexerYtClientProxy(proxy1, indexer1), this.source.getCurrentCluster());

        // 2 запроса в целом (ответим только на первый)
        checkRequest();
        checkRequest();
        mockWebServerMarketIndexer.checkNoMessages();
    }

    private void sendResponse(String master) {
        mockWebServerMarketIndexer.enqueue(new MockResponse()
                .setBody("{\"current_master\":\"" + master + "\",\"self_url\":\"http://active.idxapi.vs.market.yandex" +
                        ".net:29334/v1/master\"}"));
    }

    private void checkRequest() {
        var request = mockWebServerMarketIndexer.getMessage();
        assertEquals("/v1/master", request.getRequestUrl().encodedPath());
    }
}
