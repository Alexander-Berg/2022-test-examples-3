package ru.yandex.ace.ventura.salo;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import ru.yandex.ace.ventura.salo.config.AceVenturaSaloConfigBuilder;
import ru.yandex.ace.ventura.salo.config.MdbConfigBuilder;
import ru.yandex.ace.ventura.salo.mdb.MdbUpdaterType;
import ru.yandex.client.tvm2.Tvm2ClientConfigBuilder;
import ru.yandex.devtools.test.Paths;
import ru.yandex.dispatcher.ZooClusterConfig;
import ru.yandex.dispatcher.ZoolooserCluster;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.ProxyMultipartHandler;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.passport.tvmauth.Version;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;

public class AceVenturaSaloCluster
    implements GenericAutoCloseable<IOException>
{
    // CSOFF: MultipleStringLiterals
    private static final long TVM_RENEWAL_INTERVAL = 60000L;
    public static final String SERVICE = "change_log_0";
    private static final String SEARCH_BACKEND_CONFIG =
        Paths.getSourcePath(
            "mail/search/aceventura/aceventura_backend/files"
            + "/aceventura_search_backend.conf");
    private static final String SALO_CONFIG =
        Paths.getSourcePath(
            "mail/search/aceventura/aceventura_salo/files/aceventura_salo.conf");

    private final GenericAutoCloseableChain<IOException> chain;
    private final TestSearchBackend searchBackend;
    private final StaticServer producer;
    private final StaticServer msal;
    private final StaticServer blackbox;
    private final StaticServer mailProxy;
    private final StaticServer aceventuraProxy;
    private final AceVenturaSalo salo;
    private final StaticServer tvm2;
    private final ZoolooserCluster zoolooserCluster;

    public AceVenturaSaloCluster(
        final TestBase testBase)
        throws Exception
    {
        this(testBase, false, false);
    }

    public AceVenturaSaloCluster(
        final TestBase testBase,
        final boolean zoolooser,
        final boolean fetchOldStat)
        throws Exception
    {
        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
                 new GenericAutoCloseableHolder<>(
                     new GenericAutoCloseableChain<>()))
        {
            System.setProperty("ACEVENTURA_QUEUE", SERVICE);
            System.setProperty("PORTO_HOST", "localhost");
            System.setProperty("BSCONFIG_IHOST", "localhost");
            System.setProperty("HOSTNAME", "localhost");
            System.setProperty("BSCONFIG_IPORT", "0");
            System.setProperty("INDEX_PATH", "");
            System.setProperty("ACESALO_TVM_CLIENT_ID", "11");
            System.setProperty("TVM_CLIENT_ID", "11");
            System.setProperty("SECRET", "AAAAAAAAAAAAAAAAAAAAAA==");
            System.setProperty("CPU_CORES", "2");
            System.setProperty("SEARCH_THREADS", "2");
            System.setProperty("MERGE_THREADS", "2");
            System.setProperty("LIMIT_SEARCH_REQUESTS", "2");
            System.setProperty("INDEX_THREADS", "2");

            searchBackend =
                new TestSearchBackend(new File(SEARCH_BACKEND_CONFIG));
            chain.get().add(searchBackend);

            blackbox = new StaticServer(Configs.baseConfig());
            chain.get().add(blackbox);
            blackbox.start();

            mailProxy = new StaticServer(Configs.baseConfig());
            chain.get().add(mailProxy);
            mailProxy.start();

            aceventuraProxy = new StaticServer(Configs.baseConfig());
            chain.get().add(aceventuraProxy);
            aceventuraProxy.start();

            aceventuraProxy.add(
                "/sequential/search*",
                "{\"hitsArray\":[], \"hitsCount\":1}");

            msal = new StaticServer(Configs.baseConfig());
            chain.get().add(msal);
            msal.start();

            msal.add("/list-shards", "[\"xdb-tst166591\"]");
            if (zoolooser) {
                ZooClusterConfig zooConfig =
                    new ZooClusterConfig(testBase)
                        .searchBackend(searchBackend.lucene())
                        .startConsumer(true)
                        .startProducer(true);

                zoolooserCluster = new ZoolooserCluster(zooConfig);
                producer = null;
                chain.get().add(zoolooserCluster);

                System.setProperty(
                    "PRODUCER_INDEXING_HOST",
                    zoolooserCluster.producer().host().toString());
                System.setProperty("ACEVENTURA_QUEUE", "change_log_0");
            } else {
                zoolooserCluster = null;
                producer = new StaticServer(Configs.baseConfig());
                chain.get().add(producer);

                System.setProperty(
                    "PRODUCER_INDEXING_HOST",
                    producer.host().toString());

                StaticHttpResource searchBackendIndexProxy =
                    new StaticHttpResource(
                        new ProxyHandler(searchBackend.indexerPort()));

                producer.add("/update?*", searchBackendIndexProxy);
                producer.add("/add?*", searchBackendIndexProxy);
                producer.add("/delete?*", searchBackendIndexProxy);
                producer.add("/modify?*", searchBackendIndexProxy);
                producer.add("/ping*", searchBackendIndexProxy);
                producer.add(
                    "/notify?*",
                    new StaticHttpResource(
                        new ProxyMultipartHandler(
                            searchBackend.indexerPort())));

                producer.add(
                    "/_producer_lock?service=change_log_0"
                        + "&session-timeout=600000&producer-name=test_xdb-tst166591",
                    new StaticHttpItem("xdb-tst166591@1"));
                producer.add(
                    "/_producer_position?service=change_log_0"
                        + "&producer-name=test_xdb-tst166591:0",
                    new StaticHttpItem("-1"));

                producer.start();
            }

            tvm2 = new StaticServer(Configs.baseConfig("TVM2"));
            chain.get().add(tvm2);
            tvm2.start();

            System.setProperty("TVM_API_HOST", tvm2.host().toString());

            AceVenturaSaloConfigBuilder builder =
                new AceVenturaSaloConfigBuilder(
                    patchSaloConfig(
                        new IniConfig(
                            new File(SALO_CONFIG))));
            builder.fetchOldAbookUsageOnReindex(fetchOldStat);
            builder.workersPerMdb(1);
            builder.workersLookahead(0);
            builder.workerQueueLength(1);
            builder.requestsBatchSize(1);
            builder.selectLength(4000);

            builder.msalConfig().host(msal.host());
            if (producer != null) {
                builder.zoolooserConfig().host(producer.host());
            } else {
                builder.zoolooserConfig().host(zoolooserCluster.producer().host());
            }

            builder.tvm2ClientConfig(
                new Tvm2ClientConfigBuilder()
                    .destinationClientId(
                        builder.bpBlackboxClientId() + ',' +  builder.corpBlackboxClientId())
                    .renewalInterval(TVM_RENEWAL_INTERVAL));
            tvm2.add(
                "/2/keys/?lib_version=" + Version.get(),
                IOStreamUtils.consume(
                    StaticServer.class.getResourceAsStream("tvm-keys.txt"))
                    .processWith(ByteArrayEntityFactory.INSTANCE));
            tvm2.add(
                "/2/ticket/",
                "{\"222\":{\"ticket\":\"here the ticket\"},"
                    + "\"223\":{\"ticket\":\"another ticket\"}}");

            MdbConfigBuilder mdbCfgBuilder = new MdbConfigBuilder();
            mdbCfgBuilder.mdbProviderType(MdbProviderType.ACEVENTURA);
            mdbCfgBuilder.msalConfig(Configs.hostConfig(msal));
            mdbCfgBuilder.shardsUpdater(MdbUpdaterType.MSAL);
            mdbCfgBuilder.name("test");

            builder.providers(Collections.singletonList(mdbCfgBuilder));
            builder.blackboxConfig(Configs.hostConfig(blackbox));
            builder.corpBlackboxConfig(Configs.hostConfig(blackbox));
            builder.mailProxyConfig(Configs.hostConfig(mailProxy));
            builder.aceventuraProxyConfig(Configs.hostConfig(aceventuraProxy));
            builder.sessionTimeout(TimeUnit.MINUTES.toMillis(10));

            salo = new AceVenturaSalo(builder.build());
            chain.get().add(salo);
            this.chain = chain.release();
        }
    }


    public void start() throws IOException {
        salo.start();
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }

    public TestSearchBackend searchBackend() {
        return searchBackend;
    }

    public AceVenturaSalo salo() {
        return salo;
    }

    public StaticServer producer() {
        return producer;
    }

    public StaticServer msal() {
        return msal;
    }

    private IniConfig patchSaloConfig(
        final IniConfig config)
        throws Exception
    {
        config.sections().remove("log");
        config.sections().remove("accesslog");
        config.sections().remove("auth");
        Set<String> sectionsToRemove = new LinkedHashSet<>();
        for (String name: config.sections().keySet()) {
            if (name.startsWith("mdb-")) {
                sectionsToRemove.add(name);
            }
        }

        for (String name: sectionsToRemove) {
            config.sections().remove(name);
        }
        config.section("server").sections().remove("https");
        IniConfig server = config.section("server");
        server.sections().remove("free-space-signals");
        server.put("port", "0");
        return config;
    }
    // CSON: MultipleStringLiterals

    public static String blackboxUriSuid(final long uid) {
        return "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
            + "&dbfields=subscription.suid.2&sid=2&uid=" + uid;
    }

    public static String blackboxResponse(
        final long uid,
        final long suid)
    {
        return "{\"users\":[{\"id\":\"" + uid
            + "\",\"uid\":{\"value\":\"" + uid
            + "\",\"lite\":false,\"hosted\":false},\"login\":\"user" + uid
            + "\",\"have_password\":true,\"have_hint\":true,\"karma\":{"
            + "\"value\":0},\"karma_status\":{\"value\":6000},"
            + "\"dbfields\":{\"subscription.suid.2\":\"" + suid + "\"}}]}";
    }

    public void addBlackbox(final long uid, final long suid) {
        blackbox.add(blackboxUriSuid(uid), blackboxResponse(uid, suid));
    }

    public void addMailIndexResponse(final long suid, final String response) {
        mailProxy.add(
            "/sequential/search?&service=abook&text=abook_last_contacted:*" +
                "&get=abook_email,abook_last_contacted&json-type=dollar&prefix=" + suid,
            new StaticHttpItem(response));
    }
}
