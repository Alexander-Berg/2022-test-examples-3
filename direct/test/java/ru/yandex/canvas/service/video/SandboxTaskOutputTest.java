package ru.yandex.canvas.service.video;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.service.SandBoxService;

import static org.junit.Assert.assertNotNull;
import static ru.yandex.canvas.steps.ResourceHelpers.getResource;
import static ru.yandex.canvas.utils.SandboxTestUtils.makeSandboxDispatcher;

@RunWith(SpringJUnit4ClassRunner.class)
public class SandboxTaskOutputTest {

    private static final String RESPONSE_JSON = "/ru/yandex/canvas/service/video/sandboxVideoConverterResponse.json";
    private static String sandboxResponse;

    private SandBoxService sandBoxService;

    private AsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient();

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        sandBoxService = new SandBoxService("sandboxTestToken",
                "http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort(),
                asyncHttpClient);
    }

    @BeforeClass
    public static void setUpTestClass() throws Exception {
        sandboxResponse = getResource(RESPONSE_JSON);
    }

    @Test
    public void taskOutputDecodeTest() throws Exception {
        mockWebServer.setDispatcher(makeSandboxDispatcher(request -> {
            if (request.getMethod().equals("GET") && request.getPath().equals("/task/123456/output")) {
                return new MockResponse().setBody(sandboxResponse);
            }
            return null;
        }));

        SandBoxService.SandboxConversionTaskOutput output = sandBoxService.taskOutput(123456L);
        assertNotNull(output);
    }

}
