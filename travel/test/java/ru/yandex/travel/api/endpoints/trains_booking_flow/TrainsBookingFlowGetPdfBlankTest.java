package ru.yandex.travel.api.endpoints.trains_booking_flow;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.api.services.orders.TrainOrdersService;
import ru.yandex.travel.commons.http.CommonHttpHeaders;
import ru.yandex.travel.train.partners.im.ImClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "credentials.enabled=true",
        "management.server.port=0"
    })
@ActiveProfiles("test")
public class TrainsBookingFlowGetPdfBlankTest {

    @LocalServerPort
    private int localPort;

    private AsyncHttpClient asyncHttpClient;

    @MockBean
    private TrainOrdersService trainOrdersService;

    @Before
    public void setUp() throws IOException {
        asyncHttpClient = Dsl.asyncHttpClient(new DefaultAsyncHttpClientConfig.Builder()
            .setThreadFactory(new ThreadFactoryBuilder().setDaemon(true).build())
        );
    }

    @Test
    public void testPdfDownloadPermissionDenied() throws ExecutionException, InterruptedException, IOException {
        String sessionKey = "session_key";
        String yandexUid = "yandex_uid";
        String userIp = "127.0.0.1";

        when(trainOrdersService.downloadBlank(any()))
            .thenReturn(CompletableFuture.failedFuture(new StatusRuntimeException(Status.PERMISSION_DENIED)));

        RequestBuilder builder = new RequestBuilder()
            .setHeader(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), sessionKey)
            .setHeader(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), yandexUid)
            .setHeader(CommonHttpHeaders.HeaderType.USER_IP.getHeader(), userIp)
            .setHeader("Accept", "application/json, text/plain, */*")
            .setUrl("http://localhost:" + localPort + "/api/trains_booking_flow/v1/download_blank")
            .setMethod("GET");
        Response response = asyncHttpClient.executeRequest(builder.build()).toCompletableFuture().get();

        assertThat(response.getStatusCode()).isEqualTo(403);
        assertThat(response.getHeaders().get("Content-Type")).isEqualTo("application/json");
    }

    @Test
    public void testPdfImClientException() throws ExecutionException, InterruptedException, IOException {
        String sessionKey = "session_key";
        String yandexUid = "yandex_uid";
        String userIp = "127.0.0.1";

        when(trainOrdersService.downloadBlank(any()))
                .thenReturn(CompletableFuture.failedFuture(new ImClientException(61, "Не подходит")));

        RequestBuilder builder = new RequestBuilder()
                .setHeader(CommonHttpHeaders.HeaderType.SESSION_KEY.getHeader(), sessionKey)
                .setHeader(CommonHttpHeaders.HeaderType.YANDEX_UID.getHeader(), yandexUid)
                .setHeader(CommonHttpHeaders.HeaderType.USER_IP.getHeader(), userIp)
                .setHeader("Accept", "application/json, text/plain, */*")
                .setUrl("http://localhost:" + localPort + "/api/trains_booking_flow/v1/download_blank")
                .setMethod("GET");
        Response response = asyncHttpClient.executeRequest(builder.build()).toCompletableFuture().get();

        assertThat(response.getStatusCode()).isEqualTo(500);
        assertThat(response.getHeaders().get("Content-Type")).isEqualTo("application/json");
    }

}
