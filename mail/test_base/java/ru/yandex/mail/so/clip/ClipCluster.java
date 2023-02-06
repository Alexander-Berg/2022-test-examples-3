package ru.yandex.mail.so.clip;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import org.apache.http.impl.client.CloseableHttpClient;

import ru.yandex.collection.Pattern;
import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.server.sync.BaseHttpServer;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.FakeTvmServer;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.PostStidHandler;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.ProxyMultipartHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.test.TikaiteProxyHandler;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.mail.so.clip.config.ClipConfigBuilder;
import ru.yandex.mail.so2.So2Cluster;
import ru.yandex.msearch.proxy.MsearchProxyCluster;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.util.TestBase;
import ru.yandex.tikaite.config.ImmutableTikaiteConfig;
import ru.yandex.tikaite.config.TikaiteConfigBuilder;
import ru.yandex.tikaite.server.Server;

public class ClipCluster extends MsearchProxyCluster {
    public static final long BACKEND_POSITION = 10000L;
    public static final String APE_TVM_CLIENT_ID = "200";
    public static final String APE_TVM_TICKET = "APE ticket";
    public static final String SRW_TVM_CLIENT_ID = "2000273";
    public static final String SRW_TVM_TICKET = "SRW ticket";

    private final So2Cluster so2Cluster;
    private final StaticServer lenulca;
    private final Server tikaite;
    private final BaseHttpServer<ImmutableBaseServerConfig> tikaiteProxy;
    private final So2Cluster tikaiteSo2Cluster;
    private final FakeTvmServer tvm2;
    private final ClipServer clip;

    public ClipCluster(final TestBase testBase) throws Exception {
        super(
            testBase,
            MsearchProxyCluster.SOSEARCH_CONFIG,
            new MsearchProxyCluster.MproxyClusterContext().producer());
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(release()))
        {
            producer().add(
                "/_status*",
                "[{\"localhost\":" + BACKEND_POSITION + '}' + ']');
            producer().add(
                "/*",
                new StaticHttpResource(
                    new ProxyMultipartHandler(
                        backend().indexerPort())));

            so2Cluster = new So2Cluster(testBase);
            chain.get().add(so2Cluster);

            so2Cluster.senders().add(
                "/*",
                new StaticHttpResource(new ProxyHandler(proxy().port())));

            lenulca = new StaticServer(Configs.baseConfig("Lenulca"));
            chain.get().add(lenulca);

            tikaite = new Server(tikaiteConfig(lenulca));
            chain.get().add(tikaite);

            tikaiteProxy =
                new BaseHttpServer<>(Configs.baseConfig("TikaiteProxy"));
            chain.get().add(tikaiteProxy);

            System.setProperty("TIKAITE_HOST", tikaiteProxy.host().toString());

            System.setProperty("ADDITIONAL_CONFIG", "empty.conf");
            System.setProperty("INSTANCE_TAG_CTYPE", "prod");
            tikaiteSo2Cluster = new So2Cluster(
                testBase,
                "mail/so/daemons/so2/so2_config/files/tikaite-extract.conf",
                "TikaiteSO2");
            chain.get().add(tikaiteSo2Cluster);

            tikaiteProxy.register(
                new Pattern<>("/tikaite", false),
                new TikaiteProxyHandler(tikaite.host()));
            tikaiteProxy.register(
                new Pattern<>("/headers", true),
                new ProxyHandler(tikaite.host()));
            tikaiteProxy.register(
                new Pattern<>("/mail/handler", false),
                new TikaiteProxyHandler(tikaite.host()));
            tikaiteProxy.register(
                new Pattern<>("/extract", false),
                new PostStidHandler(
                    lenulca,
                    so2Cluster.so2().host(),
                    "/extract?only-so2"));

            tvm2 = FakeTvmServer.fromContext(testBase, chain.get());

            tvm2.addTicket(APE_TVM_CLIENT_ID, APE_TVM_TICKET);
            tvm2.addTicket(SRW_TVM_CLIENT_ID, SRW_TVM_TICKET);

            System.setProperty("CLIP_PORT", "0");
            System.setProperty("APE_TVM_CLIENT_ID", APE_TVM_CLIENT_ID);
            System.setProperty("SRW_TVM_CLIENT_ID", SRW_TVM_CLIENT_ID);
            System.setProperty("MAX_CHANGE_SIZE", "2");
            System.setProperty(
                "CONFIG_DIRS",
                Paths.getSourcePath(
                    "mail/tools/nanny_helpers/nanny_service_base/files"));
            IniConfig ini =
                new IniConfig(
                    new File(
                        Paths.getSourcePath(
                            "mail/so/daemons/clip/clip_service/files"
                            + "/clip.conf")));
            ini.sections().remove("log");
            ini.sections().remove("accesslog");
            ini.sections().remove("stderr");
            ini.section("server").sections().remove("files-staters");

            IniConfig mainSection =
                ini.sectionOrNull("extract-modules.extract-module.main");
            if (mainSection != null) {
                mainSection.put(
                    "dsl-script",
                    Paths.getSourcePath(
                        "mail/so/daemons/clip/clip_service/files/"
                        + mainSection.getString("dsl-script")));
            }

            ClipConfigBuilder builder = new ClipConfigBuilder(ini);
            ini.checkUnusedKeys();

            clip = new ClipServer(new ClipConfigBuilder(builder).build());
            chain.get().add(clip);

            reset(chain.release());
        }
    }

    private static ImmutableTikaiteConfig tikaiteConfig(
        final StaticServer lenulca)
        throws Exception
    {
        IniConfig ini =
            new IniConfig(
                new StringReader(
                    "log.level.min = all\n"
                    + "server.port = 0\n"
                    + "server.workers.min = 2\n"
                    + "server.workers.percent = 0\n"
                    + "server.connections = 50\n"
                    + "server.timeout = 5000\n"
                    + "storage.timeout = 5000\n"
                    + "storage.connections = 100\n"
                    + "storage.host = localhost\n"
                    + "storage.port = " + lenulca.port()
                    + "\nextractor.received-chain-parser.yandex-nets.file = "
                    + Paths.getSandboxResourcesRoot() + "/yandex-nets.txt\n"));
        return new TikaiteConfigBuilder(new TikaiteConfigBuilder(ini)).build();
    }

    @Override
    public void start() throws IOException {
        super.start();
        so2Cluster.start();
        lenulca.start();
        tikaite.start();
        tikaiteProxy.start();
        tikaiteSo2Cluster.start();
        clip.start();
    }

    @Override
    public void close() throws IOException {
        try (GenericAutoCloseableChain<IOException> guard = release()) {
            if (guard != null) {
                String stats = HttpAssert.stats(clip.host());
                HttpAssert.assertStat(
                    "factors-access-violations_dmmm",
                    Integer.toString(0),
                    stats);
                HttpAssert.assertStat(
                    "lua-errors-stater_dmmm",
                    Integer.toString(0),
                    stats);
            }
        }
    }

    public So2Cluster so2Cluster() {
        return so2Cluster;
    }

    public StaticServer lenulca() {
        return lenulca;
    }

    public ClipServer clip() {
        return clip;
    }

    public CloseableHttpClient client() {
        return so2Cluster.client();
    }
}

