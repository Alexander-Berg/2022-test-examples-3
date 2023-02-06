package ru.yandex.common.util.xmlrpc;

import java.net.InetAddress;
import java.net.ServerSocket;

import org.junit.Test;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Тесты для {@link XmlRPCSecureWebServer}.
 *
 * @author Vladislav Bauer
 */
public class XmlRPCSecureWebServerTest {

    private static final String STORE_FILENAME = "cacerts";
    private static final String PASSWORD = "changeit";

    @Test
    public void testSmoke() throws Exception {
        String storePath = getClass().getClassLoader().getResource(STORE_FILENAME).getFile();
        final XmlRPCSecureWebServer server = new XmlRPCSecureWebServer(0, storePath, PASSWORD, PASSWORD);
        final ServerSocket serverSocket = server.createServerSocket(0, 0, InetAddress.getLocalHost());

        assertThat(serverSocket, notNullValue());
    }

}
