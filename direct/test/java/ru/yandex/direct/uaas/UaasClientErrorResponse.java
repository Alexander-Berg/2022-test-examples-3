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

public class UaasClientErrorResponse {

    @Rule
    public JUnitSoftAssertions softAssertions = new JUnitSoftAssertions();

    private UaasClient uaasClient;

    @Before
    public void before() throws IOException {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher());
        mockWebServer.start();
        uaasClient =
                new UaasClient(UaasClientConfig.builder().withTimeout(Duration.ofSeconds(10)).withUrl("http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort()).build(),
                        new DefaultAsyncHttpClient());
    }

    @Test
    public void testRequest() {
        softAssertions.assertThatThrownBy(() -> uaasClient.split(new UaasRequest("service1"))).isInstanceOf(UaasException.class);

    }

    private Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setStatus("HTTP/1.1 " + 503);
            }
        };
    }
}
