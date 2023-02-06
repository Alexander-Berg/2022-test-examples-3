package ru.yandex.search.mail.kamaji;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;

import ru.yandex.client.tvm2.Tvm2ClientConfigBuilder;
import ru.yandex.client.tvm2.Tvm2ServiceConfigBuilder;
import ru.yandex.collection.PatternMap;
import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.config.FilterSearchConfigBuilder;
import ru.yandex.http.config.HttpHostConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.passport.tvmauth.Version;
import ru.yandex.stater.StaterConfigBuilder;
import ru.yandex.stater.StatersConfigBuilder;
import ru.yandex.test.search.backend.TestSearchBackend;

public class KamajiCluster implements GenericAutoCloseable<IOException> {
    private static final long TVM_RENEWAL_INTERVAL = 60000L;

    private final TestSearchBackend lucene;
    private final StaticServer backend;
    private final StaticServer slowIndexerProxy;
    private final StaticServer blackbox;
    private final StaticServer tikaite;
    private final StaticServer tvm2;
    private final StaticServer filterSearch;
    private final Kamaji kamaji;
    private final GenericAutoCloseableChain<IOException> chain;

    public KamajiCluster() throws Exception {
        this(null);
    }

    public KamajiCluster(final TestSearchBackend lucene) throws Exception {
        this(lucene, KamajiConfigDefaults.INSTANCE);
    }

    public KamajiCluster(
        final TestSearchBackend lucene,
        final KamajiConfig kamajiConf)
        throws Exception
    {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            if (lucene == null) {
                this.lucene = null;
                backend = new StaticServer(Configs.baseConfig("Backend"));
                chain.get().add(backend);
                backend.start();
            } else {
                this.lucene = lucene;
                backend = null;
            }
            slowIndexerProxy =
                new StaticServer(Configs.baseConfig("Slow-Indexer-Proxy"));
            chain.get().add(slowIndexerProxy);
            blackbox = new StaticServer(Configs.baseConfig("Blackbox"));
            chain.get().add(blackbox);
            tikaite = new StaticServer(Configs.baseConfig("Tikaite"));
            chain.get().add(tikaite);
            tvm2 = new StaticServer(Configs.baseConfig("TVM2"));
            chain.get().add(tvm2);
            filterSearch = new StaticServer(Configs.baseConfig("WMI"));
            chain.get().add(filterSearch);

            KamajiConfigBuilder config = new KamajiConfigBuilder(kamajiConf);
            config.port(0);
            config.connections(2 + 2);
            config.workers(2 + 2);
            if (lucene == null) {
                config.backendConfig(Configs.hostConfig(backend));
                config.searchConfig(Configs.hostConfig(backend));
            } else {
                config.backendConfig(Configs.hostConfig(lucene.indexerPort()));
                config.searchConfig(Configs.hostConfig(lucene.searchPort()));
            }
            config.slowIndexerConfig(Configs.hostConfig(slowIndexerProxy));
            config.blackboxConfig(
                new HttpHostConfigBuilder(Configs.hostConfig(blackbox))
                    .statersConfig(
                        new StatersConfigBuilder().staters(
                            new PatternMap<>(
                                new StaterConfigBuilder()
                                    .prefix("blackbox")))));
            config.tikaiteConfig(Configs.hostConfig(tikaite));

            Tvm2ServiceConfigBuilder tvm2ServiceConfig =
                new Tvm2ServiceConfigBuilder();
            new HttpHostConfigBuilder(Configs.hostConfig(tvm2))
                .copyTo(tvm2ServiceConfig);
            tvm2ServiceConfig.clientId(1);
            tvm2ServiceConfig.secret("1234567890123456789011");

            config.tvm2ServiceConfig(tvm2ServiceConfig);

            String tikaiteTvmClientId = "4";
            String blackboxTvmClientId = "6";
            String filterSearchTvmClientId = "10";
            //String corpFilterSearchTvmClientId = "11";
            config.tvm2ClientConfig(
                new Tvm2ClientConfigBuilder()
                    .destinationClientId(
                        tikaiteTvmClientId
                        + ','
                        + blackboxTvmClientId
                        + ','
                        + filterSearchTvmClientId)
                    .renewalInterval(TVM_RENEWAL_INTERVAL));
            config.tikaiteTvmClientId(tikaiteTvmClientId);
            config.blackboxTvmClientId(blackboxTvmClientId);
            config.filterSearchTvmClientId(filterSearchTvmClientId);
            //config.corpFilterSearchTvmClientId("11");

            config.filterSearchConfig(
                new FilterSearchConfigBuilder()
                    .uri(
                        URI.create(filterSearch.host() + "/filter_search"))
                    .connections(2)
                    .batchSize(2));

            config.preserveFields(
                new HashSet<>(
                    Arrays.asList(
                        "user_type",
                        "clicks_total_count",
                        "clicks_serp_count")));

            config.mdbs(Pattern.compile("(mdb\\d+|pg)"));
            tvm2.add(
                "/2/keys/?lib_version=" + Version.get(),
                IOStreamUtils.consume(
                    StaticServer.class.getResourceAsStream("tvm-keys.txt"))
                    .processWith(ByteArrayEntityFactory.INSTANCE));
            tvm2.add(
                "/2/ticket/",
                "{\"4\":{\"ticket\":\"here the ticket\"},"
                + "\"6\":{\"ticket\":\"another ticket\"},"
                + "\"10\":{\"ticket\":\"yet another ticket\"}}");
            slowIndexerProxy.start();
            blackbox.start();
            tikaite.start();
            tvm2.start();
            filterSearch.start();
            kamaji = new Kamaji(config.build());
            chain.get().add(kamaji);
            slowIndexerProxy.register(
                new ru.yandex.collection.Pattern<>("", true),
                new ProxyHandler(kamaji.host()));
            this.chain = chain.release();
        }
    }

    public TestSearchBackend lucene() {
        return lucene;
    }

    public StaticServer slowIndexerProxy() {
        return slowIndexerProxy;
    }

    public StaticServer backend() {
        return backend;
    }

    public StaticServer blackbox() {
        return blackbox;
    }

    public StaticServer tikaite() {
        return tikaite;
    }

    public StaticServer tvm2() {
        return tvm2;
    }

    public StaticServer filterSearch() {
        return filterSearch;
    }

    public Kamaji kamaji() {
        return kamaji;
    }

    public void start() throws IOException {
        kamaji.start();
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }

    public static KamajiConfigBuilder loadConfig(
        final String path)
        throws Exception
    {
        final String localhost = "http://localhost:0";
        System.setProperty("TVM_API_HOST", localhost);
        System.setProperty("TIKAITE_SRW_HOST", localhost);
        System.setProperty("TVM_CLIENT_ID", "11");
        System.setProperty("BLACKBOX_CLIENT_ID", "44");
        System.setProperty("CORP_BLACKBOX_CLIENT_ID", "5");
        System.setProperty("SECRET", "AAAAAAAAAAAAAAAAAAAAAA==");
        System.setProperty("ROBOT_UID", "22");

        System.setProperty("BSCONFIG_INAME", "kamaji");
        System.setProperty("BSCONFIG_IDIR", ".");
        System.setProperty("INUM", "0");
        System.setProperty("BSCONFIG_IPORT", String.valueOf(0));
        System.setProperty(
            "LUCENE_FIELDS_CONFIG_DIR",
            Paths.getSourcePath(
                "mail/search/mail/search_backend_mail_config/files"));

        IniConfig iniConfig = new IniConfig(new File(path));
        iniConfig.sections().remove("accesslog");
        iniConfig.sections().remove("log");

        KamajiConfigBuilder config = new KamajiConfigBuilder(iniConfig);
        return config;
    }
}

