package ru.yandex.market.logistic.pechkin.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public abstract class AbstractClientTest {

    protected static WireMockServer wireMockServer;

    protected ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    public static void startMockServer() {
        wireMockServer = new WireMockServer(wireMockConfig().port(1111));
        wireMockServer.start();
    }

    @AfterAll
    public static void stopMockServer() {
        wireMockServer.stop();
    }

}
