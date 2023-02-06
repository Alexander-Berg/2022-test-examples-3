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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatacampClientTest {
    private static final String HOST = "localhost";
    private static final int PORT = 443;

    private static final String RESPONSE_BODY = "Start full mine for 589 shops";

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
        DatacampClient client = new DatacampClient(new NettyHttpClientContext(config));
        client.setHttpClient(httpClient);
        boolean result = client.remineAll(HOST, PORT);

        Assert.assertTrue(result);
    }

}
