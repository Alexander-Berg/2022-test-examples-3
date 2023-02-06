package ru.yandex.market.tpl.common.util.test;

import java.io.IOException;

import de.flapdoodle.embed.process.runtime.Network;
import org.mockserver.integration.ClientAndServer;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

/**
 * @author valter
 * Copypasted from supercheck
 */
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
