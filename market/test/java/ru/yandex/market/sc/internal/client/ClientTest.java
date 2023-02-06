package ru.yandex.market.sc.internal.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockserver.integration.ClientAndServer;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

public abstract class ClientTest {

    protected ClientAndServer mockServer;

    protected ScIntClient scIntClient;
    protected ScLogisticsClient scLogisticsClient;

    @BeforeEach
    void init() {
        try {
            mockServer = startClientAndServer(8888, 8889, 1080, 0);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
        scIntClient = new ScIntClient("http://localhost", mockServer.getLocalPort(),
                ScIntClientConfiguration.restTemplate());
        scLogisticsClient = new ScLogisticsClientImpl("http://localhost", mockServer.getLocalPort(),
                ScIntClientConfiguration.restTemplate());
    }

    @AfterEach
    public void stopMockServer() {
        mockServer.stop();
    }
}
