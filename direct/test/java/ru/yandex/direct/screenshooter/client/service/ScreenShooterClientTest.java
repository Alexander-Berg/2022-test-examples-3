package ru.yandex.direct.screenshooter.client.service;

import java.time.Duration;

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
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.avatars.client.AvatarsClient;
import ru.yandex.direct.avatars.config.AvatarsConfig;
import ru.yandex.direct.avatars.config.ServerConfig;
import ru.yandex.direct.rotor.client.RotorClient;
import ru.yandex.direct.screenshooter.client.configuration.ScreenShooterTest;
import ru.yandex.direct.screenshooter.client.model.ScreenShooterScreenshot;
import ru.yandex.direct.screenshooter.client.model.ScreenShooterSizeInfo;
import ru.yandex.direct.screenshooter.client.model.ScreenShooterSizesInfo;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ScreenShooterTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ScreenShooterClientTest {
    private final static Logger logger = LoggerFactory.getLogger(ScreenShooterClientTest.class);
    @Rule
    public final JUnitSoftAssertions softAssertions = new JUnitSoftAssertions();

    AsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient();

    ParallelFetcherFactory fetcherFactory = new ParallelFetcherFactory(asyncHttpClient, new FetcherSettings());

    MockWebServer mockWebServer = new MockWebServer();
    String mockWebServerUrl;

    String source = "performance-banner-online";

    RotorClient rotorClient;

    AvatarsClient avatarsClient;

    ScreenShooterClient screenShooterClient;

    @Before
    public void setUp() throws Exception {
        mockWebServer.start();
        mockWebServerUrl = "http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort();

        TvmIntegration tvmIntegration = Mockito.mock(TvmIntegration.class);
        when(tvmIntegration.getTicket(any())).thenReturn("tvm_ticket_mock");

        rotorClient = new RotorClient(source, mockWebServerUrl, fetcherFactory, tvmIntegration, TvmService.ZORA_GO,
                null);

        avatarsClient = new AvatarsClient(
                new AvatarsConfig(
                        "test_config",
                        new ServerConfig(mockWebServer.getHostName(), mockWebServer.getPort(), "http"),
                        new ServerConfig(mockWebServer.getHostName(), mockWebServer.getPort(), "http"),
                        Duration.ofSeconds(5),
                        "test_namespace",
                        false),
                fetcherFactory,
                null,
                null);

        screenShooterClient = new ScreenShooterClient(rotorClient, avatarsClient);
    }

    @After
    public void tearDown() {
        try {
            mockWebServer.shutdown();
        } catch (Exception e) {
            logger.warn("cannot shut down mockWebServer", e);
        }
    }

    @Test
    public void testGetScreenshotFromUrl() {
        String imageUrl = "https://image.url";
        long imageWidth = 1024;
        long imageHeight = 768;
        byte[] imageData = new byte[]{1, 2, 3};

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

        String avatarsResponse = "{\n" +
                "    \"extra\": {\n" +
                "        \"svg\": {\n" +
                "            \"path\": \"/get-media-adv-screenshooter/3585873/ba006488-7588-49f8-b49f-cba7dd94bad8" +
                "/svg\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"group-id\": 3585873,\n" +
                "    \"imagename\": \"ba006488-7588-49f8-b49f-cba7dd94bad8\",\n" +
                "    \"meta\": {\n" +
                "        \"crc64\": \"8071A3A08761D583\",\n" +
                "        \"md5\": \"134d37692849f9631945883fa9fa61af\",\n" +
                "        \"modification-time\": 1620759913,\n" +
                "        \"orig-animated\": false,\n" +
                "        \"orig-format\": \"JPEG\",\n" +
                "        \"orig-orientation\": \"0\",\n" +
                "        \"orig-size\": {\n" +
                "            \"x\": 1080,\n" +
                "            \"y\": 607\n" +
                "        },\n" +
                "        \"orig-size-bytes\": 66289,\n" +
                "        \"processed_by_computer_vision\": false,\n" +
                "        \"processed_by_computer_vision_description\": \"computer vision is disabled\",\n" +
                "        \"processing\": \"finished\"\n" +
                "    },\n" +
                "    \"sizes\": {\n" +
                "        \"optimize\": {\n" +
                "            \"height\": 607,\n" +
                "            \"path\": \"/get-media-adv-screenshooter/3585873/ba006488-7588-49f8-b49f-cba7dd94bad8" +
                "/optimize\",\n" +
                "            \"width\": 1080\n" +
                "        },\n" +
                "        \"orig\": {\n" +
                "            \"height\": 607,\n" +
                "            \"path\": \"/get-media-adv-screenshooter/3585873/ba006488-7588-49f8-b49f-cba7dd94bad8" +
                "/orig\",\n" +
                "            \"width\": 1080\n" +
                "        },\n" +
                "        \"screenshot\": {\n" +
                "            \"height\": 205,\n" +
                "            \"path\": \"/get-media-adv-screenshooter/3585873/ba006488-7588-49f8-b49f-cba7dd94bad8" +
                "/screenshot\",\n" +
                "            \"width\": 364\n" +
                "        }\n" +
                "    }\n" +
                "}";

        mockWebServer.setDispatcher(
                new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                        if (request.getMethod().equals("POST") &&
                                request.getPath().equals("/v1/rotor/execute/png/") &&
                                request.getBody().readUtf8().equals(
                                        expectedRotorPayload.replace(" ", ""))) {
                            return new MockResponse()
                                    .setBody(new Buffer().write(imageData))
                                    .setHeader("X-Rotor-HttpCode", "200")
                                    .setHeader("X-Rotor-Status", "TExecuteResponse_EStatus_OK");
                        } else if (request.getMethod().equals("POST") &&
                                request.getPath().startsWith("/put-test_namespace/")) {
                            return new MockResponse().setBody(avatarsResponse);
                        }
                        return new MockResponse().setResponseCode(404);
                    }
                });

        ScreenShooterScreenshot screenshot = screenShooterClient.getScreenshotFromUrl(imageUrl, imageWidth,
                imageHeight);

        ScreenShooterScreenshot expectedScreenshot = new ScreenShooterScreenshot()
                .withUrl(mockWebServerUrl +
                        "/get-media-adv-screenshooter/3585873/ba006488-7588-49f8-b49f-cba7dd94bad8/orig")
                .withSizes(new ScreenShooterSizesInfo()
                        .withOrig(new ScreenShooterSizeInfo()
                                .withHeight(607)
                                .withWidth(1080)
                                .withUrl(mockWebServerUrl +
                                        "/get-media-adv-screenshooter/3585873/ba006488-7588-49f8-b49f-cba7dd94bad8/" +
                                        "orig"))
                        .withOptimize(new ScreenShooterSizeInfo()
                                .withHeight(607)
                                .withWidth(1080)
                                .withUrl(mockWebServerUrl +
                                        "/get-media-adv-screenshooter/3585873/ba006488-7588-49f8-b49f-cba7dd94bad8/" +
                                        "optimize"))
                        .withScreenshot(new ScreenShooterSizeInfo()
                                .withHeight(205)
                                .withWidth(364)
                                .withUrl(mockWebServerUrl +
                                        "/get-media-adv-screenshooter/3585873/ba006488-7588-49f8-b49f-cba7dd94bad8/" +
                                        "screenshot")))
                .withIsDone(true)
                .withImageData(imageData);

        softAssertions.assertThat(screenshot).isEqualToComparingFieldByFieldRecursively(expectedScreenshot);
    }

    @Test
    public void testGetScreenshotFromUrlRotorFailed() {
        String imageUrl = "https://image.url";
        long imageWidth = 1024;
        long imageHeight = 768;

        mockWebServer.setDispatcher(
                new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                        return new MockResponse().setResponseCode(404);
                    }
                });

        ScreenShooterScreenshot screenshot = screenShooterClient.getScreenshotFromUrl(imageUrl, imageWidth,
                imageHeight);

        ScreenShooterScreenshot expectedScreenshot = new ScreenShooterScreenshot().withIsDone(false);

        softAssertions.assertThat(screenshot).isEqualToComparingFieldByFieldRecursively(expectedScreenshot);
    }
}
