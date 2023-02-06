package ru.yandex.search.messenger.proxy;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpStatus;

import ru.yandex.client.producer.ProducerClientConfigBuilder;
import ru.yandex.devtools.test.Paths;
import ru.yandex.erratum.ErratumConfigBuilder;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.config.HttpHostConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.parser.searchmap.SearchMapConfigBuilder;
import ru.yandex.search.messenger.proxy.config.MoxyConfig;
import ru.yandex.search.messenger.proxy.config.MoxyConfigBuilder;
import ru.yandex.search.messenger.proxy.config.MoxyConfigDefaults;
import ru.yandex.search.prefix.PrefixType;
import ru.yandex.search.proxy.UpstreamConfigBuilder;
import ru.yandex.search.proxy.UpstreamsConfigBuilder;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;

public class ProxyCluster implements GenericAutoCloseable<IOException> {
    public static final String DEFAULT_LUCENE_CONF =
        Paths.getSourcePath(
            "mail/search/messenger/search_backend_messenger_config/files"
                + "/search_backend.conf");

    public static final String CHATS_LUCENE_CONF =
        Paths.getSourcePath(
            "mail/search/messenger/chats_backend/files"
                + "/chats_backend.conf");

    public static final String MESSAGES_LUCENE_CONF =
        Paths.getSourcePath(
            "mail/search/messenger/search_backend_messenger_config/files"
            + "/search_backend_messages.conf");

    private static final String DISK_SEARCH = "disk-search";
    private static final String ASTERISK = "*";
    private static final String DELETE = "/delete*";
    private static final String MODIFY = "/modify*";

    private final TestSearchBackend backend;
    private final StaticServer erratum;
    private final StaticServer producer;
    private final StaticServer router;
    private final StaticServer userSplit;
    private final Moxy moxy;
    private final GenericAutoCloseableChain<IOException> chain;

    public ProxyCluster(final TestBase testBase) throws Exception {
        this(testBase, false);
    }

    public ProxyCluster(final TestBase testBase, final boolean useProducer)
        throws Exception
    {
        this(testBase, useProducer, false);
    }

    public ProxyCluster(
        final TestBase testBase,
        final String searchBackendConf,
        final boolean useProducer)
        throws Exception
    {
        this(
            testBase,
            MoxyConfigDefaults.INSTANCE,
            useProducer,
            false,
            searchBackendConf,
            Integer.MAX_VALUE);
    }


    public ProxyCluster(
        final TestBase testBase,
        final boolean useProducer,
        final boolean fallbackToSearchMap)
        throws Exception
    {
        this(testBase, useProducer, fallbackToSearchMap, Integer.MAX_VALUE);
    }

    // CSOFF: ParameterNumber
    public ProxyCluster(
        final TestBase testBase,
        final boolean useProducer,
        final boolean fallbackToSearchMap,
        final long fatUserDocs)
        throws Exception
    {
        this(
            testBase,
            MoxyConfigDefaults.INSTANCE,
            useProducer,
            fallbackToSearchMap,
            DEFAULT_LUCENE_CONF,
            fatUserDocs);
    }

    public ProxyCluster(
        final TestBase testBase,
        final MoxyConfig config,
        final boolean useProducer,
        final boolean fallbackToSearchMap,
        final String searchBackendConf,
        final long fatUserDocs)
        throws Exception
    {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            System.setProperty("SERVICE_CONFIG", "null.conf");
            System.setProperty("CPU_CORES", "1");
            System.setProperty("INDEX_PATH", "");
            System.setProperty("INDEX_THREADS", "5");
            System.setProperty("MERGE_THREADS", "1");
            System.setProperty("BLOCK_CASHE_SIZE", "2G");
            System.setProperty("COMPRESSED_CASHE_SIZE", "2G");
            System.setProperty("SEARCH_THREADS", "6");
            System.setProperty("LIMIT_SEARCH_REQUESTS", "2");
            backend = new TestSearchBackend(
                testBase,
                true,
                new File(searchBackendConf));
            chain.get().add(backend);

            erratum = new StaticServer(Configs.baseConfig("Erratum"));
            chain.get().add(erratum);
            erratum.add(ASTERISK, HttpStatus.SC_SERVICE_UNAVAILABLE);

            router = new StaticServer(Configs.baseConfig("MessengerRouter"));
            chain.get().add(router);
            router.start();

            userSplit = new StaticServer(Configs.baseConfig("UserSplit"));
            chain.get().add(userSplit);
            userSplit.start();

            String configPath =
                Paths.getSourcePath(
                    "mail/search/messenger/moxy/main/bundle/moxy.conf");
            System.setProperty("CHATS_SERVICE", "messenger_chats");
            System.setProperty("USERS_SERVICE", "messenger_users");
            System.setProperty("MESSAGES_SERVICE", "messenger_messages");
            System.setProperty("PRODUCER_HOST", "localhost");
            System.setProperty("BSCONFIG_IDIR", "");
            System.setProperty("BSCONFIG_IPORT", "0");
            System.setProperty("BLOCK_CASHE_SIZE", "200M");
            System.setProperty("COMPRESSED_CASHE_SIZE", "200M");
            System.setProperty("SEARCHMAP_PATH", configPath);
            System.setProperty("MOXY_TVM_CONF", "moxy-notvm.conf");
            System.setProperty("PROXY_WORKERS", "2");
            System.setProperty("LIMIT_RECENT_REQUESTS", "2");
            System.setProperty("ORG_USERS_SERVICE", "messenger_users");
            System.setProperty("ORG_CHATS_SERVICE", "messenger_chats");
            MoxyConfigBuilder builder = new MoxyConfigBuilder(config);
            builder.port(0);
            builder.connections(2);
            builder.topPostConfig().loadOnStartup(false);

            builder.searchMapConfig(
                new SearchMapConfigBuilder()
                    .content(
                        backend.searchMapRule(builder.usersService())
                            + backend.searchMapRule(builder.chatsService())
                            + backend.searchMapRule(
                            builder.messagesService(),
                            PrefixType.STRING)));
            builder.searchConfig(Configs.targetConfig());
            builder.mssngrRouterConfig(Configs.hostConfig(router));
            builder.indexerConfig(Configs.targetConfig());
            builder.upstreamsConfig(
                new UpstreamsConfigBuilder().asterisk(
                    new UpstreamConfigBuilder().connections(2)));

            ErratumConfigBuilder erratumBuilder = new ErratumConfigBuilder();
            new HttpHostConfigBuilder(Configs.hostConfig(erratum))
                .copyTo(erratumBuilder);
            erratumBuilder.service(DISK_SEARCH);
            builder.misspellConfig(erratumBuilder);

            if (useProducer) {
                producer = new StaticServer(Configs.baseConfig("Producer"));
                chain.get().add(producer);
                producer.add(
                    DELETE,
                    new StaticHttpResource(
                        new ProxyHandler(backend.indexerPort())));
                producer.add(
                    MODIFY,
                    new StaticHttpResource(
                        new ProxyHandler(backend.indexerPort())));
                builder.producerClientConfig(
                    new ProducerClientConfigBuilder()
                        .connections(2)
                        .host(producer.host())
                        .fallbackToSearchMap(fallbackToSearchMap));
            } else {
                producer = null;
            }

            moxy = new Moxy(builder.build());
            chain.get().add(moxy);
            this.chain = chain.release();
        }
    }
    // CSON: ParameterNumber

    public void start() throws IOException {
        // backend already started
        if (producer != null) {
            producer.start();
        }
        erratum.start();
        moxy.start();
    }

    public TestSearchBackend backend() {
        return backend;
    }

    public StaticServer erratum() {
        return erratum;
    }

    public StaticServer producer() {
        return producer;
    }

    public Moxy proxy() {
        return moxy;
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }
}

