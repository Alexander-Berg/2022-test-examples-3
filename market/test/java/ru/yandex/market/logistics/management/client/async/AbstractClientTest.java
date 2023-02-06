package ru.yandex.market.logistics.management.client.async;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.management.client.async.config.TestClientConfig;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.HttpTemplateImpl;

@SpringBootTest(classes = {
    TestClientConfig.class,
    JacksonAutoConfiguration.class,
})
public abstract class AbstractClientTest {
    @RegisterExtension
    final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    protected final String uri = "http://localhost:80";

    protected MockRestServiceServer mockServer;

    @Autowired
    protected LmsLgwCallbackClient lmsAsyncClient;

    @Autowired
    private HttpTemplate httpTemplate;

    @BeforeEach
    protected void setup() {
        RestTemplate restTemplate = ((HttpTemplateImpl) httpTemplate).getRestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @AfterEach
    void finish() {
        mockServer.verify();
    }

}
