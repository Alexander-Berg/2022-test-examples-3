package ru.yandex.canvas.service.video;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.exceptions.SandboxApiException;
import ru.yandex.canvas.service.SandBoxService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.canvas.utils.SandboxTestUtils.makeSandboxDispatcher;

@RunWith(SpringJUnit4ClassRunner.class)
public class SandboxTaskStatusTest {
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

    @Test
    public void statusSuccessTest() {
        mockWebServer.setDispatcher(makeSandboxDispatcher(request -> {
            if (request.getMethod().equals("GET") && request.getPath().equals("/task/123456")) {
                return new MockResponse().setBody("{\"status\": \"SUCCESS\"}");
            }
            return null;
        }));

        assertEquals(SandBoxService.SandboxTaskStatus.SUCCESS, sandBoxService.taskStatus(123456L));
    }


    @Test
    public void status404Test() {
        mockWebServer.setDispatcher(makeSandboxDispatcher(request -> {
            if (request.getMethod().equals("GET") && request.getPath().equals("/task/123456")) {
                return new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()).setBody("Not found");
            }
            return null;
        }));

        boolean catched = false;

        try {
            sandBoxService.taskStatus(123456L);
        } catch (SandboxApiException e) {
            catched = true;
        } finally {
            assertTrue(catched);
        }
    }
}
