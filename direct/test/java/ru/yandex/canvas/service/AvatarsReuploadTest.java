package ru.yandex.canvas.service;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.model.avatars.AvatarsPutCanvasResult;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmIntegrationStub;
import ru.yandex.direct.tvm.TvmService;

import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
public class AvatarsReuploadTest {
    private final static Logger logger = LoggerFactory.getLogger(AvatarsReuploadTest.class);
    AvatarsService avatarsService;
    TvmIntegration tvmIntegration = new TvmIntegrationStub();
    AsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient();
    MockWebServer mockAvatarsWebServer = new MockWebServer();

    @Before
    public void setUp() throws Exception {
        mockAvatarsWebServer.start();
        avatarsService = new AvatarsService(
                "http://" + mockAvatarsWebServer.getHostName() + ":" + mockAvatarsWebServer.getPort(),
                "https://avatars.mdst.yandex.net", "canvas", tvmIntegration,
                TvmService.AVATARS_PROD, asyncHttpClient);
    }

    @After
    public void tearDown() {
        try {
            mockAvatarsWebServer.shutdown();
        } catch (Exception e) {
            logger.warn("cannot shut down mockWebServer", e);
        }
    }

    @Test
    public void testUpload() {
        mockAvatarsWebServer.setDispatcher(
                new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                        if (request.getMethod().equals("GET") &&
                                request.getPath().equals("/getimageinfo-canvas/3703/2a00000169bac39e3467bdd01fefbeff088d")) {
                            return new MockResponse().setBody("{                                                                    "
                                    + "    \"extra\": {\n"
                                    + "        \"svg\": {\n"
                                    + "            \"path\": \"/get-canvas/3703/2a00000169bac39e3467bdd01fefbeff088d/svg\"\n"
                                    + "        }\n"
                                    + "    },\n"
                                    + "    \"group-id\": 3703,\n"
                                    + "    \"imagename\": \"2a00000169bac39e3467bdd01fefbeff088d\",\n"
                                    + "    \"meta\": {\n"
                                    + "        \"AppTimestamp\": \"Feb  5 2019 12:44:53\",\n"
                                    + "        \"NeuralNetClasses\": {\n"
                                    + "            \"direct_v3_alco\": 0,\n"
                                    + "            \"direct_v3_alcoaccessories\": 0,\n"
                                    + "            \"direct_v3_intim\": 0,\n"
                                    + "            \"direct_v3_menunderwear\": 0,\n"
                                    + "            \"direct_v3_porno_probability\": 0,\n"
                                    + "            \"direct_v3_shock\": 138,\n"
                                    + "            \"direct_v3_tobacco\": 40,\n"
                                    + "            \"direct_v3_weapon\": 5,\n"
                                    + "            \"direct_v3_womenunderwear\": 0,\n"
                                    + "            \"direct_v4_intim\": 0,\n"
                                    + "            \"direct_v4_tobacco\": 188\n"
                                    + "        },\n"
                                    + "        \"crc64\": \"493524DA8758F683\",\n"
                                    + "        \"md5\": \"de31150447c33fed6be328b042ac21cc\",\n"
                                    + "        \"modification-time\": 1553616576,\n"
                                    + "        \"orig-animated\": false,\n"
                                    + "        \"orig-format\": \"JPEG\",\n"
                                    + "        \"orig-orientation\": \"\",\n"
                                    + "        \"orig-size\": {\n"
                                    + "            \"x\": 1440,\n"
                                    + "            \"y\": 720\n"
                                    + "        },\n"
                                    + "        \"orig-size-bytes\": 53902,\n"
                                    + "        \"processed_by_computer_vision\": true,\n"
                                    + "        \"processing\": \"finished\",\n"
                                    + "        \"r-orig-size\": {\n"
                                    + "            \"x\": 1400,\n"
                                    + "            \"y\": 700\n"
                                    + "        }\n"
                                    + "    },\n"
                                    + "    \"sizes\": {\n"
                                    + "        \"cropSource\": {\n"
                                    + "            \"height\": 720,\n"
                                    + "            \"path\": \"/get-canvas/3703/2a00000169bac39e3467bdd01fefbeff088d/cropSource"
                                    + "\",\n"
                                    + "            \"width\": 1440\n"
                                    + "        },\n"
                                    + "        \"largePreview\": {\n"
                                    + "            \"height\": 720,\n"
                                    + "            \"path\": \"/get-canvas/3703/2a00000169bac39e3467bdd01fefbeff088d/largePreview"
                                    + "\",\n"
                                    + "            \"width\": 1440\n"
                                    + "        },\n"
                                    + "        \"optimize\": {            \"height\": 720,\n"
                                    + "            \"path\": \"/get-canvas/3703/2a00000169bac39e3467bdd01fefbeff088d/optimize\",\n"
                                    + "            \"width\": 1440\n"
                                    + "        },\n"
                                    + "        \"orig\": {\n"
                                    + "            \"height\": 720,\n"
                                    + "            \"path\": \"/get-canvas/3703/2a00000169bac39e3467bdd01fefbeff088d/orig\",\n"
                                    + "            \"width\": 1440\n"
                                    + "        },\n"
                                    + "        \"preview\": {\n"
                                    + "            \"height\": 600,\n"
                                    + "            \"path\": \"/get-canvas/3703/2a00000169bac39e3467bdd01fefbeff088d/preview\",\n"
                                    + "            \"width\": 1200\n"
                                    + "        },\n"
                                    + "        \"preview480p\": {\n"
                                    + "            \"height\": 426,\n"
                                    + "            \"path\": \"/get-canvas/3703/2a00000169bac39e3467bdd01fefbeff088d/preview480p"
                                    + "\",\n"
                                    + "            \"width\": 852\n"
                                    + "        },\n"
                                    + "        \"thumbnail\": {\n"
                                    + "            \"height\": 300,\n"
                                    + "            \"path\": \"/get-canvas/3703/2a00000169bac39e3467bdd01fefbeff088d/thumbnail\",\n"
                                    + "            \"width\": 600\n"
                                    + "        }\n"
                                    + "    } }");
                        }
                        return new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value());
                    }
                });

        AvatarsPutCanvasResult result = avatarsService.upload(
                "https://avatars.mdst.yandex.net/get-canvas/3703/2a00000169bac39e3467bdd01fefbeff088d/orig");

        AvatarsPutCanvasResult.SizesInfo sizeInfo = result.getSizes();

        assertThat(sizeInfo, Matchers.notNullValue());
        assertThat(sizeInfo.getCropSource().getUrl(), Matchers.equalTo(
                "https://avatars.mdst.yandex.net/get-canvas/3703/2a00000169bac39e3467bdd01fefbeff088d/cropSource"));
        assertThat(sizeInfo.getLargePreview().getUrl(), Matchers.equalTo(
                "https://avatars.mdst.yandex.net/get-canvas/3703/2a00000169bac39e3467bdd01fefbeff088d/largePreview"));
        assertThat(sizeInfo.getOptimize().getUrl(), Matchers.equalTo(
                "https://avatars.mdst.yandex.net/get-canvas/3703/2a00000169bac39e3467bdd01fefbeff088d/optimize"));
        assertThat(sizeInfo.getOrig().getUrl(), Matchers.equalTo(
                "https://avatars.mdst.yandex.net/get-canvas/3703/2a00000169bac39e3467bdd01fefbeff088d/orig"));
        assertThat(sizeInfo.getPreview().getUrl(), Matchers.equalTo(
                "https://avatars.mdst.yandex.net/get-canvas/3703/2a00000169bac39e3467bdd01fefbeff088d/preview"));
        assertThat(sizeInfo.getPreview480p().getUrl(), Matchers.equalTo(
                "https://avatars.mdst.yandex.net/get-canvas/3703/2a00000169bac39e3467bdd01fefbeff088d/preview480p"));
        assertThat(sizeInfo.getThumbnail().getUrl(), Matchers.equalTo(
                "https://avatars.mdst.yandex.net/get-canvas/3703/2a00000169bac39e3467bdd01fefbeff088d/thumbnail"));
    }
}
