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

public class UaasClientRequestTest {

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
                        .withUrl("http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort())
                        .withTimeout(Duration.ofSeconds(10))
                        .build(),
                        new DefaultAsyncHttpClient());
    }

    @Test
    public void testRequest() {
        UaasResponse responseCall = uaasClient.split(new UaasRequest("service").withUserAgent("ru.yandex.sample/4.56" +
                ".1234 (Apple iPad1,1; iOS 5.1.1)").withCookies("yandexuid=423425342130").withHost("host").withIp(
                "2a02:6b8:b010:d003" +
                        "::1:c8").withUuid("32140").withText("text"));
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
                softAssertions.assertThat(request.getPath()).isEqualTo("/service?text=text&uuid=32140");
                softAssertions.assertThat(request.getHeader("User-Agent")).isEqualTo("ru.yandex.sample/4.56.1234 " +
                        "(Apple iPad1,1; iOS 5.1.1)");
                softAssertions.assertThat(request.getHeader("Cookie")).isEqualTo("yandexuid=423425342130");
                softAssertions.assertThat(request.getHeader("Host")).isEqualTo("host");
                softAssertions.assertThat(request.getHeader("X-Forwarded-For-Y")).isEqualTo("2a02:6b8:b010:d003::1:c8");
                return new MockResponse();
            }
        };
    }
}
