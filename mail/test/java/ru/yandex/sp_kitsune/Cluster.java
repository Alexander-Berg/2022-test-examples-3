package ru.yandex.sp_kitsune;

import java.io.IOException;
import java.nio.file.Path;

import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.kitsune.config.KitsuneConfigBuilder;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.util.TestBase;

public class Cluster extends GenericAutoCloseableHolder<
        IOException,
        GenericAutoCloseableChain<IOException>> {
    public static final String TVM_CLIENT_ID = "2019898";
    public static final String TVM_SECRET = "1234567890123456789011";

    private final SpKitsuneHttpServer server;
    final StaticServer headServer;
    final StaticServer tailServer;

    public Cluster(TestBase env, Path configPath, String route) throws Exception {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                     new GenericAutoCloseableHolder<>(
                             new GenericAutoCloseableChain<>())) {

            final StaticServer tvm2 = new StaticServer(Configs.baseConfig("TVM2"));
            chain.get().add(tvm2);

            headServer = new StaticServer(Configs.baseConfig("HEAD"));
            chain.get().add(headServer);

            tailServer = new StaticServer(Configs.baseConfig("TAIL"));
            chain.get().add(tailServer);

            System.setProperty("CPU_CORES", "2");
            System.setProperty("KITSUNE_PORT", "0");
            System.setProperty("TIMER_RESOLUTION", "51ms");
            System.setProperty("ROUTE", route);
            System.setProperty("PANEL_TITLE", "title");
            System.setProperty("PANEL_TAG", "tag");

            System.setProperty("TVM_API_HOST", tvm2.host().toString());
            System.setProperty("TVM_CLIENT_ID", TVM_CLIENT_ID);
            System.setProperty("SECRET", TVM_SECRET);

            System.setProperty("KITSUNE_HEAD_HOST", headServer.host().toString());

            final IniConfig ini = new IniConfig(configPath.toFile());
            ini.sections().remove("log");
            ini.sections().remove("accesslog");
            ini.sections().remove("stderr");
            ini.sections().remove("tvm2");
            ini.sections().get("server").sections().remove("http-check");

            final IniConfig tailsSection = ini.sections().getOrDefault("tails", null);
            if (tailsSection != null) {
                tailsSection.sections().get("testing").put("host", tailServer.host().toString());
            }

            final KitsuneConfigBuilder builder = new KitsuneConfigBuilder(ini);
            ini.checkUnusedKeys();

            server = new SpKitsuneHttpServer(builder.build());
            chain.get().add(server);
            reset(chain.release());
        }
    }

    public void start() throws IOException {
        server.start();
        headServer.start();
        tailServer.start();
    }

    public SpKitsuneHttpServer server() {
        return server;
    }

    public StaticServer head() {
        return headServer;
    }

    public StaticServer tail() {
        return tailServer;
    }
}

