package ru.yandex.market.capacity.storage.client;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.capacity.storage.client.config.TestClientConfig;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.HttpTemplateImpl;

@SpringBootTest(classes = {
    TestClientConfig.class,
    JacksonAutoConfiguration.class
})
@ParametersAreNonnullByDefault
@ExtendWith(SoftAssertionsExtension.class)
public abstract class AbstractClientTest {

    protected static final String URI = "http://localhost:80";

    @InjectSoftAssertions
    protected JUnitSoftAssertions softly;

    protected MockRestServiceServer mockServer;

    @Autowired
    public CapacityStorageClient client;

    @Autowired
    private HttpTemplate httpTemplate;


    @BeforeEach
    protected void setUp() {
        RestTemplate restTemplate = ((HttpTemplateImpl) httpTemplate).getRestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @AfterEach
    protected void finish() {
        mockServer.verify();
        mockServer.reset();
    }

    protected UriComponentsBuilder getUriBuilder(String path) {
        return UriComponentsBuilder
            .fromHttpUrl(URI)
            .path(path);
    }

}
