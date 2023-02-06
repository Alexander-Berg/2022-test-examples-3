package ru.yandex.iex.proxy;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;

import ru.yandex.cokemulator.Cokemulator;
import ru.yandex.cokemulator.CokemulatorConfigBuilder;
import ru.yandex.cokemulator.DataType;
import ru.yandex.collection.Pattern;
import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.server.sync.BaseHttpServer;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.PostStidHandler;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.test.TikaiteProxyHandler;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.mail.so2.So2Cluster;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.util.TestBase;
import ru.yandex.tikaite.config.ImmutableTikaiteConfig;
import ru.yandex.tikaite.config.TikaiteConfigBuilder;
import ru.yandex.tikaite.server.Server;

public class MailStorageCluster implements GenericAutoCloseable<IOException> {
    private static final String ASTERISK = "*";
    private static final String TESTING_IEX_URL =
        "http://definitely-wrong-endpoint";
//         TODO: proper mock
//        "http://iex-proxy-testing.n.yandex-team.ru/iex/factextract/";

    private final CloseableHttpClient client = Configs.createDefaultClient();
    private final GenericAutoCloseableChain<IOException> chain;
    private final StaticServer lenulca;
    private final StaticServer mulcaGate;
    private final LenulcaHandler lenulcaHandler;
    private final LenulcaHandler mulcaGateHandler;
    private final So2Cluster so2Cluster;
    private final Server tikaite;
    private final BaseHttpServer<ImmutableBaseServerConfig> tikaiteProxy;
    private final Cokemulator cokemulator;
    private final StaticServer cokemulatorEmulator; //C2I

    public MailStorageCluster(final TestBase testBase) throws Exception {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            lenulca = new StaticServer(Configs.baseConfig("Lenulca"));
            chain.get().add(lenulca);
            lenulcaHandler = new LenulcaHandler();
            lenulca.add(ASTERISK, new StaticHttpResource(lenulcaHandler));

            mulcaGate = new StaticServer(Configs.baseConfig("MulcaGate"));
            chain.get().add(mulcaGate);
            mulcaGateHandler = new LenulcaHandler();
            mulcaGate.add(ASTERISK, new StaticHttpResource(mulcaGateHandler));

            System.setProperty("ADDITIONAL_CONFIG", "empty.conf");
            so2Cluster = new So2Cluster(
                testBase,
                "mail/so/daemons/so2/so2_config/files/tikaite-extract.conf");
            chain.get().add(so2Cluster);

            tikaite = new Server(getConfig(lenulca.port()));
            chain.get().add(tikaite);
            tikaiteProxy =
                new BaseHttpServer<>(Configs.baseConfig("TikaiteProxy"));
            chain.get().add(tikaiteProxy);
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

            CokemulatorConfigBuilder cokilder = new CokemulatorConfigBuilder();
            cokilder
                .port(0)
                .connections(2)
                .concurrency(2)
                .storageConfig(Configs.hostConfig(lenulca))
                .tikaiteConfig(Configs.hostConfig(tikaite))
                .contentType(ContentType.TEXT_PLAIN)
                .dataType(DataType.MAIL_TEXT);
            cokilder.jniWrapperConfig()
                .libraryName(
                    Paths.getBuildPath(
                        "mail/library/jniwrapper/dynamic_test"
                        + "/libjniwrapper-test.so"));
            cokilder.jniWrapperConfig()
                .mainName("jniwrapper_test_copy");
            cokemulator = new Cokemulator(cokilder.build());
            chain.get().add(cokemulator);
            cokemulatorEmulator =
                new StaticServer(Configs.baseConfig("CokeProxy"));
            chain.get().add(cokemulatorEmulator);

            cokemulatorEmulator.add(
                ASTERISK,
                new StaticHttpResource(
                    new CokIexProxyHandler(cokemulator.port(), iexUrl())));

            this.chain = chain.release();
        }
    }

    public static String iexUrl() {
        return TESTING_IEX_URL;
    }

    public StaticServer cokemulator() {
        return cokemulatorEmulator;
    }

    public int lenulcaPort() throws IOException {
        return lenulca.port();
    }

    public BaseHttpServer<ImmutableBaseServerConfig> tikaite() {
        return tikaiteProxy;
    }

    public StaticServer mulcaGate() {
        return mulcaGate;
    }

    public int cokemulatorPort() throws IOException {
        return cokemulatorEmulator.port();
    }

    public void put(final String stid, final URL url)
        throws URISyntaxException
    {
        File file = new File(url.toURI());
        lenulcaHandler.add(stid, file);
        mulcaGateHandler.add(stid, file);
    }

    public void put(final String stid, final File file) {
        lenulcaHandler.add(stid, file);
        mulcaGateHandler.add(stid, file);
    }

    public static Reader getConfigReader(final String suffix) {
        return getConfigReader(1, suffix);
    }

    public static Reader getConfigReader(final int port) {
        return getConfigReader(port, "");
    }

    public static Reader getConfigReader(
        final int port,
        final String suffix)
    {
        return new StringReader(
            "log.level.min = all\nserver.port = 0\nserver.workers.min = 2\n"
            + "server.workers.percent = 50\nserver.connections = 50\n"
            + "server.timeout = 5000\nstorage.timeout = 5000\n"
            + "storage.connections = 100\nstorage.host = localhost\n"
            + "storage.port = " + port
            + "\nextractor.received-chain-parser.yandex-nets.file = "
            + Paths.getSandboxResourcesRoot() + "/yandex-nets.txt\n"
            + suffix);
    }

    public static ImmutableTikaiteConfig getConfig()
        throws ConfigException, IOException
    {
        return getConfig(1);
    }

    public static ImmutableTikaiteConfig getConfig(final int port)
        throws ConfigException, IOException
    {
        return getConfig(port, "");
    }

    public static ImmutableTikaiteConfig getConfig(final String suffix)
        throws ConfigException, IOException
    {
        return getConfig(1, suffix);
    }

    public static ImmutableTikaiteConfig getConfig(
        final int port,
        final String suffix)
        throws ConfigException, IOException
    {
        IniConfig properties = new IniConfig(getConfigReader(port, suffix));
        return new TikaiteConfigBuilder(properties).build();
    }

    public void start() throws IOException {
        lenulca.start();
        mulcaGate.start();
        so2Cluster.start();
        tikaite.start();
        tikaiteProxy.start();
        cokemulator.start();
        cokemulatorEmulator.start();
    }

    @Override
    @SuppressWarnings("try")
    public void close() throws IOException {
        try {
            // Wait for postactions to complete
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            // ignore
        }
        try (CloseableHttpClient client = this.client) {
            chain.close();
        }
    }
}
