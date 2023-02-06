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

public class UaasClientResponseTest {

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
    public void testResponse() {
        UaasResponse responseCall = uaasClient.split(new UaasRequest("service1"));
        softAssertions.assertThat(responseCall.getConfigVersion()).isEqualTo("15253");
        softAssertions.assertThat(responseCall.getSplitParams()).isEqualTo(
                "{\"r\":0,\"s\":\"direct\",\"d\":\"tablet\",\"m\":\"ipad\",\"b\":\"Unknown\",\"i\":true,\"n\":\"\"," +
                        "\"f\":\"\"}");
        softAssertions.assertThat(responseCall.getBoxes()).isEqualTo("189744,0,19;192348,0,86;194132,0,31");
        softAssertions.assertThat(responseCall.getBoxedCrypted()).isEqualTo("NX2LOq7WS_GmsG2DQBY" +
                "-FfLRCa4J1k3CmzxTNpp3EeMzQtz7lI5g7w,,");
        var flags = responseCall.getFlags();
        softAssertions.assertThat(flags).hasSize(3);
        softAssertions.assertThat(flags.get(0)).isEqualTo("[{\"HANDLER\":\"DRIVE_MOBILE\"," +
                "\"CONTEXT\":{\"DRIVE_MOBILE\":{}}}]");
        softAssertions.assertThat(flags.get(1)).isEqualTo("[{\"HANDLER\":\"APPSEARCH\"," +
                "\"CONTEXT\":{\"APPSEARCH\":{\"flags\":[\"appsearch_config=1\",\"passthrough=1\"]," +
                "\"testid\":[\"192348\"]}},\"CONDITION\":\"cgi.app_id eq 'ru.yandex.searchplugin'\"}," +
                "{\"HANDLER\":\"REPORT\",\"CONTEXT\":{\"MAIN\":{}},\"TESTID\":[\"192348\"],\"CONDITION\":\"cgi.app_id" +
                " eq 'ru.yandex.searchplugin'\"}]");
        softAssertions.assertThat(flags.get(2)).isEqualTo("[]");
    }

    private Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse()
                        .setHeader("X-Yandex-ExpConfigVersion", "15253")
                        .setHeader("X-Yandex-ExpSplitParams",
                                "eyJyIjowLCJzIjoiZGlyZWN0IiwiZCI6InRhYmxldCIsIm0iOiJpcGFkIiwiYiI6IlVua25vd24iLCJpIjp0cnVlLCJuIjoiIiwiZiI6IiJ9")
                        .setHeader("X-Yandex-ExpBoxes", "189744,0,19;192348,0,86;194132,0,31")
                        .setHeader("X-Yandex-ExpBoxes-Crypted", "NX2LOq7WS_GmsG2DQBY" +
                                "-FfLRCa4J1k3CmzxTNpp3EeMzQtz7lI5g7w,,")
                        .setHeader("X-Yandex-ExpFlags",
                                "W3siSEFORExFUiI6IkRSSVZFX01PQklMRSIsIkNPTlRFWFQiOnsiRFJJVkVfTU9CSUxFIjp7fX19XQ==," +
                                        "W3siSEFORExFUiI6IkFQUFNFQVJDSCIsIkNPTlRFWFQiOnsiQVBQU0VBUkNIIjp7ImZsYWdzIjpbImFwcHNlYXJjaF9jb25maWc9MSIsInBhc3N0aHJvdWdoPTEiXSwidGVzdGlkIjpbIjE5MjM0OCJdfX0sIkNPTkRJVElPTiI6ImNnaS5hcHBfaWQgZXEgJ3J1LnlhbmRleC5zZWFyY2hwbHVnaW4nIn0seyJIQU5ETEVSIjoiUkVQT1JUIiwiQ09OVEVYVCI6eyJNQUlOIjp7fX0sIlRFU1RJRCI6WyIxOTIzNDgiXSwiQ09ORElUSU9OIjoiY2dpLmFwcF9pZCBlcSAncnUueWFuZGV4LnNlYXJjaHBsdWdpbicifV0=,W10=");
            }
        };
    }
}
