package ru.yandex.market.replenishment.autoorder.service.client;

import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;

public class JnsClientTest extends FunctionalTest {

    @Autowired
    private JnsClient jnsClient;

    private MockWebServer mockWebServer;

    @Before
    public void setUp() {
        this.mockWebServer = new MockWebServer();
        ReflectionTestUtils.setField(jnsClient, "baseUrl", mockWebServer.url("/").toString());
    }

    @Test
    public void test() {
        mockWebServer.enqueue(new MockResponse()
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .throttleBody(64, 5, TimeUnit.MILLISECONDS)
            .setBody("")
            .setResponseCode(200));
        jnsClient.sendTelegramMessage("test", "channel");
    }

    @Test
    public void testFail() {
        mockWebServer.enqueue(new MockResponse()
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .throttleBody(64, 5, TimeUnit.MILLISECONDS)
            .setBody("error")
            .setResponseCode(404));
        jnsClient.sendTelegramMessage("test", "channel");
    }

    @Test
    public void testBadUrl() {
        ReflectionTestUtils.setField(jnsClient, "baseUrl", "test/test");
        jnsClient.sendTelegramMessage("test", "channel");
    }
}
