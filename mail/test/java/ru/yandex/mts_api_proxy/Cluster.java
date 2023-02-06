package ru.yandex.mts_api_proxy;

import java.io.File;
import java.io.IOException;

import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.mts_api_proxy.config.MtsApiProxyConfigBuilder;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.passport.tvmauth.Version;
import ru.yandex.test.util.TestBase;

public class Cluster extends GenericAutoCloseableHolder<
        IOException,
        GenericAutoCloseableChain<IOException>> {
    public static final String TVM_CLIENT_ID = "2019898";
    public static final String TVM_SECRET = "1234567890123456789011";
    public static final String GOZORA_TVM_CLIENT_ID = "2036529";
    public static final String GOZORA_TVM_TICKET = "gozora tvm ticket";
    public static final String MTS_API_CONSUMER_KEY = "MTS_API_CONSUMER_KEY";
    public static final String MTS_API_CONSUMER_SECRET = "MTS_API_CONSUMER_SECRET";

    private final MtsApiProxyHttpServer server;
    private final StaticServer gozora;

    public Cluster(TestBase env) throws Exception {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                     new GenericAutoCloseableHolder<>(
                             new GenericAutoCloseableChain<>())) {

            final StaticServer tvm2 = new StaticServer(Configs.baseConfig("TVM2"));
            chain.get().add(tvm2);

            gozora = StaticServer.fromContext(env, "Gozora", "GOZORA_HOST", chain.get());
            System.setProperty("GOZORA_TVM_CLIENT_ID", GOZORA_TVM_CLIENT_ID);

            System.setProperty("MTS_API_HOST", "https://api.mts.ru");
            System.setProperty("MTS_API_CONSUMER_KEY", MTS_API_CONSUMER_KEY);
            System.setProperty("MTS_API_CONSUMER_SECRET", MTS_API_CONSUMER_SECRET);

            System.setProperty("NANNY_SERVICE_ID", "mts_api_proxy");

            System.setProperty("TVM_API_HOST", tvm2.host().toString());
            System.setProperty("TVM_CLIENT_ID", TVM_CLIENT_ID);
            System.setProperty("TVM_SECRET", TVM_SECRET);

            System.setProperty("ALLOWED_SRCS", "");

            System.setProperty("PORT", Integer.toString(0));
            System.setProperty("HTTPS_PORT", Integer.toString(0));
            System.setProperty("SERVER_NAME", "localhost");
            System.setProperty("JKS_PASSWORD", "password");

            final IniConfig ini =
                    new IniConfig(
                            new File(Paths.getSourcePath("mail/so/daemons/mts_api_proxy/config/files" +
                                    "/mts_api_proxy.conf")));
            ini.sections().remove("log");
            ini.sections().remove("accesslog");
            ini.sections().remove("stderr");
            ini.section("server").sections().remove("free-space-signals");
            ini.section("server").sections().remove("https");

            ini.sections().remove("auth");

            MtsApiProxyConfigBuilder builder = new MtsApiProxyConfigBuilder(ini);
            ini.checkUnusedKeys();

            tvm2.add(
                    "/2/keys/?lib_version=" + Version.get(),
                    IOStreamUtils.consume(
                                    StaticServer.class.getResourceAsStream("tvm-keys.txt"))
                            .processWith(ByteArrayEntityFactory.INSTANCE));
            tvm2.add(
                    "/2/ticket/",
                    "{" +
                            "\"" + GOZORA_TVM_CLIENT_ID + "\":{\"ticket\":\"" + GOZORA_TVM_TICKET + "\"}" +
                            "}");
            tvm2.start();

            server = new MtsApiProxyHttpServer(builder.build());
            chain.get().add(server);
            reset(chain.release());
        }
    }

    public void start() throws Exception {
        server.start();
    }

    @Override
    public void close() throws IOException {
        try {
            // Wait for postactions to complete
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            // ignore
        }
        super.close();
    }

    public MtsApiProxyHttpServer server() {
        return server;
    }

    public StaticServer gozora() {
        return gozora;
    }
}

