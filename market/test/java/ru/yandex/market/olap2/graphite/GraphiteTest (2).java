package ru.yandex.market.olap2.graphite;

import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.olap2.util.SleepUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.Assert.assertTrue;

public class GraphiteTest {

    @Test
    @Ignore("sleep-based multithreading")
    public void mustReconnect() throws IOException {
        ServerSocket failingServer = new ServerSocket();
        failingServer.bind(null);
        int port = failingServer.getLocalPort();
        Graphite graphite = new Graphite(
            "localhost", port, "unittest", 200);
        graphite.runSender();
        SleepUtil.sleep(50);
        failingServer.close();
        while(!failingServer.isClosed()) {}
        ServerSocket failingServer2 = new ServerSocket(port);
        new Thread(() -> {
            try {
                Socket discard = failingServer2.accept();
                discard.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        SleepUtil.sleep(50);
        failingServer2.close();
        SleepUtil.sleep(50);

        graphite.report("tst", 123);
        ServerSocket serverSocket = new ServerSocket(port);
        Socket socket = serverSocket.accept();
        InputStream is = socket.getInputStream();
        byte buf[] = new byte[4096];
        int len = is.read(buf);
        assertTrue(new String(buf, 0, len).startsWith("one_min.marketstat.unittest.olap2etl.tst "));
    }
}
