package ru.yandex.chemodan.util.test;

import com.xebialabs.restito.server.StubServer;

import ru.yandex.misc.io.IoFunction1V;
import ru.yandex.misc.io.IoFunction2V;
import ru.yandex.misc.ip.IpPortUtils;

/**
 * @author akirakozov
 */
public class StubServerUtils {
    public static void withStubServer(int port, IoFunction1V<StubServer> callback) {
        StubServer stubServer = null;
        try {
            stubServer = new StubServer(port).run();
            callback.apply(stubServer);
        } finally {
            if (stubServer != null) {
                stubServer.stop();
            }
        }
    }

    public static void withStubServer(IoFunction2V<Integer, StubServer> callback) {
        StubServer stubServer = null;
        try {
            int port = IpPortUtils.getFreeLocalPort().getPort();
            stubServer = new StubServer(port).run();
            callback.apply(port, stubServer);
        } finally {
            if (stubServer != null) {
                stubServer.stop();
            }
        }
    }
}
