package ru.yandex.market.tsum.clients.idx;

import com.google.common.util.concurrent.Futures;
import org.apache.http.HttpStatus;
import org.asynchttpclient.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClient;
import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.tsum.clients.idx.bean.ReportVersionInfoResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportClientTest {
    private static final String INDEXER_VERSION = "2019.3.55.0";
    private static final String REPORT_HOST = "localhost";
    private static final int REPORT_PORT = 8181;

    private static final String RESPONSE_BODY =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<admin-action>\n" +
            "<market-indexer-version>" + INDEXER_VERSION + "</market-indexer-version>\n" +
            "</admin-action>";

    private NettyHttpClient httpClient;

    @Before
    public void initMock() {
        httpClient = mock(NettyHttpClient.class);
        Response response = mock(Response.class);

        when(response.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(response.getResponseBody()).thenReturn(RESPONSE_BODY);

        when(httpClient.executeRequest(any())).thenReturn(
            Futures.immediateFuture(response)
        );
    }

    @Test
    public void testGetVersionInfo() {
        HttpClientConfig config = new HttpClientConfig();
        ReportClient client = new ReportClient(new NettyHttpClientContext(config));
        client.setHttpclient(httpClient);
        ReportVersionInfoResponse versionInfo = client.getVersionInfo(REPORT_HOST, REPORT_PORT);

        Assert.assertNotNull(versionInfo);
        Assert.assertEquals(versionInfo.getMarketIndexerVersion(), INDEXER_VERSION);
    }

}
