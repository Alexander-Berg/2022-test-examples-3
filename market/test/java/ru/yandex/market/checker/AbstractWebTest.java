package ru.yandex.market.checker;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootTest(properties = "spring.config.name=test-context.properties", classes = {TestConfig.class})
public abstract class AbstractWebTest {
    public static MockWebServer mockBackEnd;

    @Autowired
    private WebClient webClient;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    public WebClient getWebClient() {
        return webClient;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
