package ru.yandex.direct.rotor.client;

import java.nio.charset.Charset;
import java.util.List;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.assertj.core.api.JUnitSoftAssertions;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.rotor.client.model.RotorImageResponse;
import ru.yandex.direct.rotor.client.model.RotorResponse;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class RotorClientTest {
    private final static Logger logger = LoggerFactory.getLogger(RotorClientTest.class);
    @Rule
    public final JUnitSoftAssertions softAssertions = new JUnitSoftAssertions();

    AsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient();

    ParallelFetcherFactory fetcherFactory = new ParallelFetcherFactory(asyncHttpClient, new FetcherSettings());

    MockWebServer mockRotorWebServer = new MockWebServer();

    String source = "performance-banner-online";

    RotorClient rotorClient;

    @Before
    public void setUp() throws Exception {
        mockRotorWebServer.start();

        TvmIntegration tvmIntegration = Mockito.mock(TvmIntegration.class);
        when(tvmIntegration.getTicket(any())).thenReturn("tvm_ticket_mock");

        rotorClient = new RotorClient(
                source,
                "http://" + mockRotorWebServer.getHostName() + ":" + mockRotorWebServer.getPort(),
                fetcherFactory, tvmIntegration, TvmService.ZORA_GO, null);
    }

    @After
    public void tearDown() {
        try {
            mockRotorWebServer.shutdown();
        } catch (Exception e) {
            logger.warn("cannot shut down mockWebServer", e);
        }
    }

    @Test
    public void testGetScreenshotFromUrl() throws InterruptedException {
        String imageUrl = "https://image.url";
        long imageWidth = 1024;
        long imageHeight = 768;

        String expectedRotorPayload = "{" +
                "    \"TvmServiceTicket\": \"tvm_ticket_mock\"," +
                "    \"Url\": \"" + imageUrl + "\"," +
                "    \"Source\": \"" + source + "\"," +
                "    \"Options\": {" +
                "         \"ScreenshotMode\": 0," +
                "         \"OutputFormat\": {" +
                "              \"Html\": false," +
                "              \"Png\": true" +
                "          }," +
                "         \"ViewPortSize\": {" +
                "              \"Width\": " + imageWidth + "," +
                "              \"Height\": " + imageHeight + "" +
                "          }," +
                "         \"Plugins\": {" +
                "             \"Flash\": false" +
                "          }," +
                "         \"EnableImages\": true," +
                "         \"EnableAdblock\":false," +
                "         \"Trace\":false" +
                "    }," +
                "    \"HttpResponse\": null" +
                "}";

        mockRotorWebServer.setDispatcher(
                new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                        if (request.getMethod().equals("POST") &&
                                request.getPath().equals("/v1/rotor/execute/png/") &&
                                request.getBody().readUtf8().equals(
                                        expectedRotorPayload.replace(" ", ""))) {
                            return new MockResponse()
                                    .setBody(new Buffer().write(new byte[]{1, 2, 3}))
                                    .setHeader("X-Rotor-HttpCode", "200")
                                    .setHeader("X-Rotor-Status", "TExecuteResponse_EStatus_OK");
                        }
                        return new MockResponse().setResponseCode(404);
                    }
                });

        RotorImageResponse response = rotorClient.getScreenshotFromUrl(imageUrl, imageWidth, imageHeight);

        softAssertions.assertThat(response).isNotNull();
        softAssertions.assertThat(response.getImage()).isEqualTo(new byte[]{1, 2, 3});
        softAssertions.assertAll();
    }

    @Test
    public void testGet() throws InterruptedException {
        String url = "https://some.url";
        String resultUrl = "https://result.url";
        List<String> urlTrace = List.of(url, "https://intermediate.url", resultUrl);

        String expectedRotorPayload = "{" +
                "    \"TvmServiceTicket\": \"tvm_ticket_mock\"," +
                "    \"Url\": \"" + url + "\"," +
                "    \"Source\": \"" + source + "\"," +
                "    \"Options\": {" +
                "         \"ScreenshotMode\": 0," +
                "         \"OutputFormat\": {" +
                "              \"Html\": true," +
                "              \"Png\": false" +
                "          }," +
                "         \"ViewPortSize\": {" +
                "              \"Width\": 0," +
                "              \"Height\": 0" +
                "          }," +
                "         \"Plugins\": {" +
                "             \"Flash\": false" +
                "          }," +
                "         \"EnableImages\": true," +
                "         \"EnableAdblock\":false," +
                "         \"Trace\":true" +
                "    }," +
                "    \"HttpResponse\": null" +
                "}";

        mockRotorWebServer.setDispatcher(
                new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                        if (request.getMethod().equals("POST") &&
                                request.getPath().equals("/v1/rotor/execute/") &&
                                request.getBody().readUtf8().equals(
                                        expectedRotorPayload.replace(" ", ""))) {
                            String response = "{" +
                                    "    \"Url\": \"" + url + "\"," +
                                    "    \"ResultUrl\": \"" + resultUrl + "\"," +
                                    "    \"UrlTrace\": [" +
                                    "        \"" + url + "\"," +
                                    "        \"https://intermediate.url\"," +
                                    "        \"" + resultUrl + "\"" +
                                    "    ]," +
                                    "    \"Trace\": {\n" +
                                    "        \"LogEvents\": [\n" +
                                    "            {\n" +
                                    "                \"JobProxyStateChange\": {\n" +
                                    "                    \"NodeName\": \"localhost\",\n" +
                                    "                    \"ProxyStateId\": \"PJS_WaitExec\"\n" +
                                    "                },\n" +
                                    "                \"Offset\": 304\n" +
                                    "            },\n" +
                                    "            {\n" +
                                    "                \"JobResource\": {\n" +
                                    "                    \"Duration\": 404,\n" +
                                    "                    \"Status\": \"RS_FROM_ZORA\",\n" +
                                    "                    \"Url\": \"https://itunes.apple.com\"\n" +
                                    "                },\n" +
                                    "                \"JobResourceDetails\": {\n" +
                                    "                    \"Request\": {\n" +
                                    "                        \"Headers\": [\n" +
                                    "                            {\n" +
                                    "                                \"Name\": \"User-Agent\",\n" +
                                    "                                \"Value\": \"Mozilla/5.0 (compatible; " +
                                    "YandexBot/3.0; +http://yandex.com/bots) AppleWebKit/537.36 (KHTML, like Gecko) " +
                                    "Chrome/81.0.4044.268\"\n" +
                                    "                            }\n" +
                                    "                        ]\n" +
                                    "                    },\n" +
                                    "                    \"Response\": {\n" +
                                    "                        \"Headers\": [\n" +
                                    "                            {\n" +
                                    "                                \"Name\": \"X-Daiquiri-Instance\",\n" +
                                    "                                \"Value\": " +
                                    "\"daiquiri:11896007:mr47p00it-qujn04123002:7987:22RELEASE72:daiquiri-amp-store" +
                                    "-l7shared-int-001-mr\"\n" +
                                    "                            }\n" +
                                    "                        ]\n" +
                                    "                    }\n" +
                                    "                }\n" +
                                    "            },\n" +
                                    "            {\n" +
                                    "                \"JobResource\": {\n" +
                                    "                    \"Url\": \"https://example.com\"\n" +
                                    "                }\n" +
                                    "            }\n" +
                                    "        ]\n" +
                                    "    }" +
                                    "}";
                            return new MockResponse()
                                    .setBody(new Buffer().writeString(response, Charset.defaultCharset()))
                                    .setHeader("X-Rotor-HttpCode", "200")
                                    .setHeader("X-Rotor-Status", "TExecuteResponse_EStatus_OK");
                        }
                        return new MockResponse().setResponseCode(404);
                    }
                });

        RotorResponse response = rotorClient.get(url);

        softAssertions.assertThat(response).isNotNull();
        softAssertions.assertThat(response.getUrl()).isEqualTo(url);
        softAssertions.assertThat(response.getResultUrl()).isEqualTo(resultUrl);
        softAssertions.assertThat(response.getUrlTrace()).isEqualTo(urlTrace);
        softAssertions.assertThat(response.getTrace()).isNotNull();
        softAssertions.assertThat(response.getTrace().getLogEvents()).hasSize(3);
        softAssertions.assertThat(response.getTrace().getLogEvents().get(0).getJobResource()).isNull();
        softAssertions.assertThat(response.getTrace().getLogEvents().get(1).getJobResource()).isNotNull();
        softAssertions.assertThat(response.getTrace().getLogEvents().get(1).getJobResource().getUrl()).isEqualTo("https://itunes.apple.com");
        softAssertions.assertThat(response.getTrace().getLogEvents().get(2).getJobResource()).isNotNull();
        softAssertions.assertThat(response.getTrace().getLogEvents().get(2).getJobResource().getUrl()).isEqualTo("https://example.com");
        softAssertions.assertAll();
    }
}
