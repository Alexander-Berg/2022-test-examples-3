package ru.yandex.kitsune;

import java.io.IOException;
import java.io.StringReader;

import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.kitsune.config.KitsuneConfigBuilder;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.util.TestBase;

public class KitsuneCluster extends GenericAutoCloseableHolder<
        IOException,
        GenericAutoCloseableChain<IOException>> {
    private final KitsuneHttpServerComparator server;
    private final StaticServer headServer;
    private final StaticServer tailServer;

    public KitsuneCluster(TestBase env) throws Exception {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                     new GenericAutoCloseableHolder<>(
                             new GenericAutoCloseableChain<>())) {

            headServer = new StaticServer(Configs.baseConfig("Head"));
            chain.get().add(headServer);

            tailServer = new StaticServer(Configs.baseConfig("Tail"));
            chain.get().add(tailServer);

            final KitsuneConfigBuilder builder = new KitsuneConfigBuilder(new IniConfig(new StringReader("" +
                    "[server]\n" +
                    "port = 0\n" +
                    "connections = 1000\n" +
                    "timeout = 5s\n" +
                    "timer.resolution = 51ms\n" +
                    "linger = 1\n" +
                    "workers.min = 16\n" +
                    "workers.percent = 0\n" +
                    "" +
                    "[head]\n" +
                    "host = " + headServer.host().toString() + "\n" +
                    "connections = 1000\n" +
                    "timeout = 5s\n" +
                    "" +
                    "[tails.first_proxy]\n" +
                    "host = " + tailServer.host().toString() + "\n" +
                    "connections = 1000\n" +
                    "timeout = 5s\n" +
                    "proxy-patterns = handle1\n" +
                    "" +
                    "[proxy-patterns]\n" +
                    "handle1 = /scoring/some/path\n"
            )));
            server = new KitsuneHttpServerComparator(builder.build());
            chain.get().add(server);
            reset(chain.release());
        }
    }

    public void start() throws IOException {

        server.start();
        headServer.start();
        tailServer.start();
    }

    public KitsuneHttpServerComparator server() {
        return server;
    }

    public StaticServer headServer() {
        return headServer;
    }

    public StaticServer tailServer() {
        return tailServer;
    }
}

