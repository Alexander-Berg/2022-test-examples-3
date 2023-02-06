package ru.yandex.market.logistics.logistics4shops.client;

import javax.annotation.Nonnull;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.logistics4shops.client.config.TestClientConfig;
import ru.yandex.market.logistics4shops.client.api.OrderBoxApi;

@ExtendWith({
    SpringExtension.class,
    SoftAssertionsExtension.class,
})
@SpringBootTest(classes = {
    TestClientConfig.class,
})
@TestPropertySource("/client-test.properties")
public class AbstractClientTest {
    @InjectSoftAssertions
    protected SoftAssertions softly;

    @Autowired
    private RestTemplate clientRestTemplate;

    @Autowired
    protected OrderBoxApi orderBoxApi;

    protected MockRestServiceServer mockServer;

    @BeforeEach
    public void setUpMockServer() {
        mockServer = MockRestServiceServer.createServer(clientRestTemplate);
    }

    @Nonnull
    protected final String getBaseUrl() {
        return orderBoxApi.getApiClient().getBasePath();
    }
}
