package ru.yandex.travel.api.services.orders.train;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.api.services.train.TrainApiProperties;
import ru.yandex.travel.api.services.train.TrainApiProxy;
import ru.yandex.travel.api.services.train.TrainProxyOrderListDTO;
import ru.yandex.travel.api.services.train.TrainProxyOrderStatus;
import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.testing.TestUtils;
import ru.yandex.travel.testing.misc.TestResources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TrainApiProxyTest {

    private TrainApiProxy trainApiProxy;
    private AsyncHttpClientWrapper mockHttpClient;

    @Before
    public void setUp() {
        mockHttpClient = mock(AsyncHttpClientWrapper.class);
        TrainApiProperties props = new TrainApiProperties();
        props.setHttpReadTimeout(Duration.ofSeconds(1));
        props.setHttpRequestTimeout(Duration.ofSeconds(2));
        props.setBaseUrl("https://localhost");
        trainApiProxy = new TrainApiProxy(mockHttpClient, props, null);
    }

    @Test
    public void testParseWithoutErrors() throws ExecutionException, InterruptedException {
        mockResponse();
        CompletableFuture<TrainProxyOrderListDTO> future = trainApiProxy.listTrainOrders(0, 10,
                TrainProxyOrderStatus.DONE, "");
        TrainProxyOrderListDTO orderListDTO = future.get();
        assertThat(orderListDTO.getResults()).hasSize(2);
    }

    private void mockResponse() {
        String responseBody = TestResources.readResource("train_proxy/get_orders.json");
        Response mockedResponse = mock(Response.class);
        when(mockedResponse.getStatusCode()).thenReturn(200);
        when(mockedResponse.getResponseBody()).thenReturn(responseBody);
        CompletableFuture<Response> mockedFuture = TestUtils.genericsFriendlyMock(CompletableFuture.class);
        when(mockedFuture.toCompletableFuture()).thenReturn(CompletableFuture.completedFuture(mockedResponse));
        when(mockHttpClient.executeRequest(any(RequestBuilder.class))).thenReturn(mockedFuture);
    }
}
