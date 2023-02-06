package ru.yandex.direct.uaas;

import java.io.IOException;
import java.time.Duration;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.JUnitSoftAssertions;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static ru.yandex.direct.uaas.UaasClient.DEFAULT_IP;

public class UaasClientRequestAllParamsEmptyTest {

    @Rule
    public JUnitSoftAssertions softAssertions = new JUnitSoftAssertions();

    private UaasClient uaasClient;

    @Before
    public void before() throws IOException {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher());
        mockWebServer.start();
        uaasClient =
                new UaasClient(UaasClientConfig.builder()
                        .withTimeout(Duration.ofSeconds(10))
                        .withUrl("http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort())
                        .build(),
                        new DefaultAsyncHttpClient());
    }

    @Test
    public void testRequest() {
        UaasResponse responseCall = uaasClient.split(new UaasRequest("service1"));
        softAssertions.assertThat(responseCall.getConfigVersion()).isEmpty();
        softAssertions.assertThat(responseCall.getSplitParams()).isEmpty();
        softAssertions.assertThat(responseCall.getBoxedCrypted()).isEmpty();
        softAssertions.assertThat(responseCall.getBoxes()).isEmpty();
        softAssertions.assertThat(responseCall.getFlags()).isEmpty();
    }

    private Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getMethod()).isEqualTo("GET");
                softAssertions.assertThat(request.getPath()).isEqualTo("/service1?text=");
                softAssertions.assertThat(request.getHeader("User-Agent")).isEmpty();
                softAssertions.assertThat(request.getHeader("Cookie")).isNull();
                softAssertions.assertThat(request.getHeader("Host")).isEmpty();
                softAssertions.assertThat(request.getHeader("X-Forwarded-For-Y")).isEqualTo(DEFAULT_IP);
                return new MockResponse();
            }
        };
    }
}
