package ru.yandex.market.fmcg.bff.test;

import org.mockserver.integration.ClientAndServer;
import ru.yandex.market.fmcg.core.util.Network;

import java.io.IOException;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

public enum MockServerUtil {
    INSTANCE;

    private ClientAndServer mockServer;
    private int port;

    MockServerUtil() {
        try {
            port = Network.getFreeServerPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mockServer = startClientAndServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopMockServer));
    }

    private void stopMockServer() {
        mockServer.stop();
    }

    public ClientAndServer mockServer() {
        return mockServer;
    }

    public int getPort() {
        return port;
    }

    public String getUrl() {
        return "http://localhost:" + getPort();
    }

    public void reset() {
        mockServer.reset();
    }
}
