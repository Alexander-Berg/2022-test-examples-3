package ru.yandex.market.delivery.transport_manager.client;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.delivery.transport_manager.client.config.TestClientConfiguration;
import ru.yandex.market.delivery.transport_manager.client.config.TmApiProperties;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = {
        TestClientConfiguration.class,
    },
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class AbstractClientTest {

    @RegisterExtension
    protected final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    @Autowired
    protected TmApiProperties tmApiProperties;

    protected MockRestServiceServer mockServer;

    @Autowired
    protected RestTemplate clientRestTemplate;

    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(clientRestTemplate);
    }

    @AfterEach
    public void tearDown() {
        mockServer.verify();
    }
}
