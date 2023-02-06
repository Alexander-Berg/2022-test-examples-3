package ru.yandex.ohio.backend;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.FakeTvmServer;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.ProxyMultipartHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.logbroker2.MessageSenderCluster;
import ru.yandex.ohio.backend.config.OhioBackendConfigBuilder;
import ru.yandex.ohio.indexer.OhioIndexer;
import ru.yandex.ohio.indexer.config.OhioIndexerConfigBuilder;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.searchmap.SearchMapConfigBuilder;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;
import ru.yandex.util.filesystem.CloseableDeleter;

public class OhioBackendCluster extends GenericAutoCloseableHolder<
    IOException,
    GenericAutoCloseableChain<IOException>>
{
    public static final String SERVICE_TICKET =
        "3:serv:CBAQ__________9_IggIv657EOGuew:Vo_zZ_k_zcQZG_yZ-xeON32sTHrbJ3q"
        + "nQ2QddiS1Jo1dx3ruAr6_-U5h47dhc_w9zO9E-je38NRD3Id8Z4FqVbYEwi0A37iLjM"
        + "-nAlDwKDPqG7rYxC0_2z0jQ0lwHDXECNwQEQTr7DEz8OXtiCIqe_ZmSmHDnwVLKwVrU"
        + "yGWDv0";

    public static final String USER_TICKET =
        "3:user:CA0Q__________9_GhYKBgiBuNyWCxCBuNyWCyDShdjMBCgB:NfXTyI4sTq2O5"
        + "X975_RG8pwdZqcHawHEA5spXUpZzQUygPn6TLkVTCsvk6YwOZX0dPJcC_eqBmWCY8Xx"
        + "lgPint8FLma8U6Ei77E29VqCaRffNZQxQfs1L6qckQK4hDakwF0WROv83eCSvQC4ipn"
        + "ZscyK89GB0vpDsdVP-VjlR2E";

    public static final String SECONDARY_USER_TICKET =
        "3:user:CA0Q__________9_GhQKBQip2pICEKnakgIg0oXYzAQoAQ:QZgV9iR0pLkswN-"
        + "P-vvUtIuh2ubptV7TDnx6yCCqk68mLEXVLWmsuidVL_xIljG8zw1T7kPxmfg_5vQ5A8"
        + "lDX1HvXbZ9qN3KNk984Blmy8JdG1zghvjPjUkV8Zzb0nDZzUshoH5-TUUnqRldGRuLU"
        + "eIzipQ3-6857lWOCgXnHQk";

    public static final String DARKSPIRIT_SERVICE_TICKET =
        "3:serv:CBAQ__________9_IggI4a57ENyMeg:NPg14HLHHd2OW6MH5xz7t0tC3TuzTsS"
        + "jB4hfpfWagXKyzNrnkRrcoIIMtQLok8WZKo5btlsxS1-giygYxFHnRTJiEsimmPLO2D"
        + "d79xRkrVx-J4_FzakJJPrloJeLRhJcIn8EMVyD-8rxf7LOPECmNq9En-t38XGUYCXyi"
        + "YpkvpY";

    public static final String DYNGO_SERVICE_TICKET =
        "3:serv:CBAQ__________9_IggI4a57EKjgew:UrFXsiFc2C8gZS-qm-xuLwgW5HaxCNX"
        + "ZFZQ6OIvoBJdf-FcoS-bXrQHuu9yx-Zcb20wrYaeko4OeWjgq_-KKsi97wKcoXOC_Nl"
        + "uyFxe8YAGVxESKwCaDvUR6XOaMSiqZZSirN-0Q5e9dNWUcYCLTgnw4XX2bMnbF1knTG"
        + "oWIsh0";

    public static final String FNSLINKER_SERVICE_TICKET =
        "3:serv:CBAQ__________9_IggI4a57EPDcew:SluYBW-AL38A6L6gb9sUtjYgjuMKLdp"
        + "qD37ikkCEsLA8lekig88u2ra76Rxp76ieAdRU0lx9PbPTdjDLj0YFjkM2yD9TmyNmFB"
        + "FhS9O4AY5Z-ZRYtIEAyjNifOfJbK60b2vgOj52zCufu8BPlTxymBFspFuSz6q3ilrbt"
        + "tnVkcU";

    public static final int DARKSPIRIT_TVM_ID = 2000476;
    public static final int DYNGO_TVM_ID = 2027560;
    public static final int FNSLINKER_TVM_ID = 2027120;
    public static final int BACKEND_POSITION = 100500;

    private final MessageSenderCluster messageSenderCluster;
    private final MessageSenderCluster yandexPayMessageSenderCluster;
    private final TestSearchBackend searchBackend;
    private final StaticServer producer;
    private final FakeTvmServer tvm2;
    private final StaticServer bunker;
    private final StaticServer darkspirit;
    private final StaticServer dyngo;
    private final StaticServer fnslinker;
    private final OhioBackend ohioBackend;
    private final OhioIndexer ohioIndexer;

    public OhioBackendCluster(final TestBase testBase) throws Exception {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            System.setProperty("TRUST_CLIENT_ID", "1");
            System.setProperty("TRUST_TOPICS", "1");
            System.setProperty("YANDEXPAY_CLIENT_ID", "1");
            System.setProperty("YANDEXPAY_TOPICS", "1");
            messageSenderCluster = new MessageSenderCluster(
                testBase,
                "",
                "",
                100,
                false,
                "mail/library/logbroker/logbroker2_consumer_service/files/"
                + "ohio.conf",
                "default");
            chain.get().add(messageSenderCluster);
            yandexPayMessageSenderCluster = new MessageSenderCluster(
                testBase,
                "",
                "",
                100,
                false,
                "mail/library/logbroker/logbroker2_consumer_service/files/"
                + "ohio.conf",
                "yandexpay");
            chain.get().add(yandexPayMessageSenderCluster);

            System.setProperty("SHARDS_PER_HOST", "7");
            System.setProperty("OLD_SEARCH_PORT", "0");
            System.setProperty("SEARCH_PORT", "0");
            System.setProperty("DUMP_PORT", "0");
            System.setProperty("INDEXER_PORT", "0");
            System.setProperty(
                "SEARCHMAP_PATH",
                Paths.getSourcePath(
                    "mail/ohio/ohio_backend_service/files"
                    + "/searchmap.txt"));
            searchBackend =
                new TestSearchBackend(
                    testBase,
                    new File(
                        Paths.getSourcePath(
                            "mail/ohio/ohio_backend_service/files"
                            + "/search_backend.conf")));
            chain.get().add(searchBackend);

            producer = new StaticServer(Configs.baseConfig("Producer"));
            chain.get().add(producer);

            tvm2 = FakeTvmServer.fromContext(testBase, chain.get());
            chain.get().add(tvm2);
            tvm2.addTicket(DARKSPIRIT_TVM_ID, DARKSPIRIT_SERVICE_TICKET);
            tvm2.addTicket(DYNGO_TVM_ID, DYNGO_SERVICE_TICKET);
            tvm2.addTicket(FNSLINKER_TVM_ID, FNSLINKER_SERVICE_TICKET);

            bunker = new StaticServer(Configs.baseConfig("Bunker"));
            chain.get().add(bunker);
            String bunkerUri =
                "/v1/cat?node=/passport-order-history/config&version=latest";
            bunker.add(
                bunkerUri,
                testBase.resource("bunker.json").toFile());
            bunker.start();

            darkspirit = new StaticServer(Configs.baseConfig("Darkspirit"));
            chain.get().add(darkspirit);

            dyngo = new StaticServer(Configs.baseConfig("Dyngo"));
            chain.get().add(dyngo);

            fnslinker = new StaticServer(Configs.baseConfig("FnsLinker"));
            chain.get().add(fnslinker);

            System.setProperty(
                "PRODUCER_PORT",
                Integer.toString(producer.port()));
            System.setProperty(
                "PRODUCER_HOST",
                producer.host().toString());
            System.setProperty("OHIO_BACKEND_PORT", "0");
            System.setProperty("OHIO_BACKEND_HTTPS_PORT", "0");
            System.setProperty("BALANCER_NAME", "ohio-backend.so.yandex.net");

            System.setProperty("TVM_API_HOST", tvm2.host().toString());
            System.setProperty("TVM_CLIENT_ID", "2021217");
            System.setProperty("SECRET", "1234567890123456789012");
            System.setProperty("ALLOWED_SRCS", "2021183");

            System.setProperty("SERVICE_CONFIG", "ohio_backend_test.conf");
            System.setProperty(
                "TERMINALS_INFOS_PATH",
                Paths.getSandboxResourcesRoot() + "/terminals.json");

            System.setProperty(
                "DARKSPIRIT_HOST",
                darkspirit.host().toString());
            System.setProperty(
                "DARKSPIRIT_TVM_ID",
                Long.toString(DARKSPIRIT_TVM_ID));
            System.setProperty(
                "CHECK_URL_BASE",
                "https://check.yandex.ru/pdf");
            System.setProperty(
                "BUNKER_URI",
                bunker.host().toString() + bunkerUri);

            System.setProperty("DYNGO_HOST", dyngo.host().toString());
            System.setProperty("DYNGO_TVM_ID", Long.toString(DYNGO_TVM_ID));

            System.setProperty("FNSLINKER_HOST", fnslinker.host().toString());
            System.setProperty(
                "FNSLINKER_TVM_ID",
                Long.toString(FNSLINKER_TVM_ID));

            IniConfig ini =
                new IniConfig(
                    new File(
                        Paths.getSourcePath(
                            "mail/ohio/ohio_backend_service/files"
                            + "/ohio_backend.conf")));
            TestBase.clearLoggerSection(ini.section("log"));
            TestBase.clearLoggerSection(ini.section("accesslog"));
            ini.section("server.https.keystore")
                .put(
                    "file",
                    Paths.getSourcePath(
                        "mail/library/http/http_test/main/resources/ru/yandex"
                        + "/http/test/localhost.jks"));
            ini.section("producer-store").sections().remove("https");
            ini.section("search").sections().remove("timeout");
            ini.section("darkspirit").sections().remove("https");
            ini.section("fiscal-storages").sections().remove("https");
            ini.section("dyngo").sections().remove("https");
            ini.section("fnslinker").sections().remove("https");

            CloseableDeleter localCacheDir =
                new CloseableDeleter(Files.createTempDirectory("local-cache"));
            chain.get().add(localCacheDir);
            ini.section("server")
                .put(
                    "local-cache-dir",
                    localCacheDir.path().toString());

            OhioBackendConfigBuilder builder =
                new OhioBackendConfigBuilder(ini);
            ini.checkUnusedKeys();

            builder.searchMapConfig(
                new SearchMapConfigBuilder()
                    .content(searchBackend.searchMapRule("ohio_index")));

            ohioBackend =
                new OhioBackend(
                    new OhioBackendConfigBuilder(builder.build()).build());
            chain.get().add(ohioBackend);

            System.setProperty("OHIO_INDEXER_PORT", "0");

            ini =
                new IniConfig(
                    new File(
                        Paths.getSourcePath(
                            "mail/ohio/ohio_lbconsumer_service/files"
                            + "/ohio_indexer.conf")));
            TestBase.clearLoggerSection(ini.section("log"));
            TestBase.clearLoggerSection(ini.section("accesslog"));
            TestBase.clearLoggerSection(ini.section("stderr"));
            ini.section("producer-store").sections().remove("https");

            OhioIndexerConfigBuilder indexerBuilder =
                new OhioIndexerConfigBuilder(ini);
            ini.checkUnusedKeys();

            ohioIndexer =
                new OhioIndexer(
                    new OhioIndexerConfigBuilder(indexerBuilder.build())
                        .build());
            chain.get().add(ohioIndexer);

            messageSenderCluster.targetServer().add(
                "/*",
                new StaticHttpResource(new ProxyHandler(ohioIndexer.port())));
            yandexPayMessageSenderCluster.targetServer().add(
                "/*",
                new StaticHttpResource(new ProxyHandler(ohioIndexer.port())));
            producer.add(
                "/_status*",
                "[{\"localhost\":" + BACKEND_POSITION + '}' + ']');
            producer.add(
                "/*",
                new StaticHttpResource(
                    new ProxyMultipartHandler(searchBackend.indexerPort())));

            reset(chain.release());
        }
    }

    public void start() throws Exception {
        messageSenderCluster.start();
        yandexPayMessageSenderCluster.start();
        producer.start();
        darkspirit.start();
        dyngo.start();
        fnslinker.start();
        ohioBackend.start();
        ohioIndexer.start();
    }

    public MessageSenderCluster messageSenderCluster() {
        return messageSenderCluster;
    }

    public MessageSenderCluster yandexPayMessageSenderCluster() {
        return yandexPayMessageSenderCluster;
    }

    public TestSearchBackend searchBackend() {
        return searchBackend;
    }

    public StaticServer darkspirit() {
        return darkspirit;
    }

    public StaticServer dyngo() {
        return dyngo;
    }

    public StaticServer fnslinker() {
        return fnslinker;
    }

    public OhioBackend ohioBackend() {
        return ohioBackend;
    }

    public OhioIndexer ohioIndexer() {
        return ohioIndexer;
    }
}

