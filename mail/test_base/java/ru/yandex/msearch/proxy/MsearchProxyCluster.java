package ru.yandex.msearch.proxy;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.embedded.PreparedDbProvider;

import ru.yandex.blackbox.BlackboxUserinfo;
import ru.yandex.client.producer.ProducerClientConfigBuilder;
import ru.yandex.client.tvm2.Tvm2ClientConfigBuilder;
import ru.yandex.client.tvm2.Tvm2ServiceConfigBuilder;
import ru.yandex.collection.PatternMap;
import ru.yandex.devtools.test.Paths;
import ru.yandex.erratum.ErratumConfigBuilder;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.config.DnsConfigBuilder;
import ru.yandex.http.config.HttpHostConfigBuilder;
import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.config.URIConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.FakeTvmServer;
import ru.yandex.http.test.StaticServer;
import ru.yandex.msearch.proxy.api.Api;
import ru.yandex.msearch.proxy.config.DkimStatsConfigDefaults;
import ru.yandex.msearch.proxy.config.EnlargeConfigBuilder;
import ru.yandex.msearch.proxy.config.ImmutableMsearchProxyConfig;
import ru.yandex.msearch.proxy.config.MsearchProxyConfigBuilder;
import ru.yandex.msearch.proxy.config.RankingConfig;
import ru.yandex.msearch.proxy.config.RankingConfigDefaults;
import ru.yandex.msearch.proxy.config.SoCheckConfigBuilder;
import ru.yandex.msearch.proxy.config.SuggestConfigBuilder;
import ru.yandex.msearch.proxy.config.SuggestConfigDefaults;
import ru.yandex.msearch.proxy.dispatcher.DispatcherFactory;
import ru.yandex.msearch.proxy.logger.Logger;
import ru.yandex.msearch.proxy.search.Searcher;
import ru.yandex.msearch.proxy.searchmap.SearchMap;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.searchmap.SearchMapConfigBuilder;
import ru.yandex.search.msal.pool.AddressPgConfigDefaults;
import ru.yandex.search.proxy.UpstreamConfigBuilder;
import ru.yandex.search.proxy.UpstreamsConfigBuilder;
import ru.yandex.stater.StaterConfigBuilder;
import ru.yandex.stater.StatersConfigBuilder;
import ru.yandex.test.search.backend.TestMailSearchBackend;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;

public class MsearchProxyCluster extends GenericAutoCloseableHolder<
    IOException,
    GenericAutoCloseableChain<IOException>>
{
    public static final String PROD_CONFIG =
        "mail/search/mail/msearch_proxy_service_config/files/"
        + "msearch-proxy-prod.conf";
    public static final String SOSEARCH_CONFIG =
        "mail/search/mail/msearch_proxy_service_config/files/"
        + "sosearch-proxy-prod.conf";
    public static final String MOPS_TVM_ID = "1";
    public static final String MOPS_TVM_TICKET = "mops ticket";
    public static final String FILTER_SEARCH_TVM_ID = "5";
    public static final String FILTER_SEARCH_TVM_TICKET =
        "here another ticket";
    public static final String CORP_FILTER_SEARCH_TVM_ID = "6";
    public static final String CORP_FILTER_SEARCH_TVM_TICKET =
        "here and another ticket";

    private static final long TVM_RENEWAL_INTERVAL = 60000L;
    private static final int TIMEOUT = 10000;

    private static final String LABELS = "/labels";
    private static final String FOLDERS = "/folders";

    private final TestSearchBackend backend;
    private final StaticServer filterSearch;
    private final StaticServer threadSearch;
    private final StaticServer erratum;
    private final StaticServer userSplit;
    private final StaticServer iexProxy;
    private final StaticServer corpFilterSearch;
    private final StaticServer blackbox;
    private final StaticServer corpBlackbox;
    private final FakeTvmServer tvm2;
    private final StaticServer producer;
    private final StaticServer soCheck;
    private final StaticServer corpMl;
    private final StaticServer mops;
    private final StaticServer tupita;
    private final StaticServer furita;
    private final HttpServer proxy;
    private final IniConfig iniConfig;

    public MsearchProxyCluster(final TestBase testBase) throws Exception {
        this(testBase, false, false, false);
    }

    public MsearchProxyCluster(final TestBase testBase, boolean useProducer)
        throws Exception
    {
        this(testBase, useProducer, false, false);
    }

    public MsearchProxyCluster(
            final TestBase testBase,
            boolean useProducer,
            boolean useErratum)
            throws Exception
    {
        this(testBase, useProducer, useErratum, false);
    }

    public ImmutableMsearchProxyConfig config(
        final MproxyClusterContext clusterContext)
        throws Exception
    {
        return config(clusterContext, null);
    }

    public ImmutableMsearchProxyConfig config(
        final MproxyClusterContext clusterContext,
        final IniConfig iniConfig)
        throws Exception
    {
        MsearchProxyConfigBuilder builder;
        if (iniConfig != null) {
            builder = new MsearchProxyConfigBuilder(iniConfig);
        } else {
            builder = new MsearchProxyConfigBuilder();
        }

        builder.port(0);
        builder.connections(2);
        builder.workers(2);
        if (clusterContext.searchMap() == null) {
            builder.searchMapConfig(
                new SearchMapConfigBuilder()
                    .content(
                        backend.searchMapRule(builder.pgQueue())
                        + backend.searchMapRule(builder.pgCorpQueue())
                        + backend.searchMapRule(builder.oracleQueue())
                        + backend.searchMapRule(builder.oracleCorpQueue())
                        + backend.searchMapRule("mdb100")
                        + backend.searchMapRule("mdb200")
                        + backend.searchMapRule("subscriptions_prod_1")
                        + backend.searchMapRule("subscriptions_prod_2")
                        + backend.searchMapRule("pg")));
        } else {
            builder.searchMapConfig(
                new SearchMapConfigBuilder()
                    .content(clusterContext.searchMap()));
        }

        if (clusterContext.useMops()) {
            builder.mopsClientConfig(new HttpHostConfigBuilder().connections(2).host(mops.host()));
        }

        builder.tupitaConfig(Configs.hostConfig(tupita));
        builder.furitaConfig(Configs.hostConfig(furita));

        builder.dnsConfig(
            new DnsConfigBuilder(Configs.dnsConfig())
                .dnsHostsMapping(clusterContext.dnsHostsMapping()));
        builder.searchConfig(Configs.targetConfig());
        builder.indexerConfig(Configs.targetConfig());
        builder.upstreamsConfig(
            new UpstreamsConfigBuilder().asterisk(
                new UpstreamConfigBuilder()
                    .connections(2)
                    .timeout(TIMEOUT)
                    .poolTimeout(TIMEOUT)));
        builder.filterSearchConfig(
            Configs.filterSearchConfig(filterSearch));
        builder.threadsConfig(
            Configs.filterSearchConfig(threadSearch, "/threads_info?"));

        builder.labelsConfig(Configs.uriConfig(filterSearch, LABELS));
        builder.foldersConfig(Configs.uriConfig(filterSearch, FOLDERS));
        builder.corpFilterSearchConfig(
            Configs.filterSearchConfig(corpFilterSearch));
        builder.corpLabelsConfig(Configs.uriConfig(corpFilterSearch, LABELS));
        builder.corpFoldersConfig(
            Configs.uriConfig(corpFilterSearch, FOLDERS));
        builder.blackboxConfig(
            new HttpHostConfigBuilder(Configs.hostConfig(blackbox))
                .statersConfig(
                    new StatersConfigBuilder().staters(
                        new PatternMap<>(
                            new StaterConfigBuilder()
                                .prefix("blackbox")))));
        builder.corpBlackboxConfig(
            new HttpHostConfigBuilder(Configs.hostConfig(corpBlackbox))
                .statersConfig(
                    new StatersConfigBuilder().staters(
                        new PatternMap<>(
                            new StaterConfigBuilder()
                                .prefix("corp-blackbox")))));

        Tvm2ServiceConfigBuilder tvm2ServiceConfig =
            new Tvm2ServiceConfigBuilder();
        new HttpHostConfigBuilder(Configs.hostConfig(tvm2))
            .copyTo(tvm2ServiceConfig);
        tvm2ServiceConfig.clientId(1);
        tvm2ServiceConfig.secret("1234567890123456789011");

        Tvm2ClientConfigBuilder tvm2ClientConfig =
            new Tvm2ClientConfigBuilder();
        tvm2ClientConfig.destinationClientId("4");
        tvm2ClientConfig.renewalInterval(TVM_RENEWAL_INTERVAL);

        builder.tvm2ServiceConfig(tvm2ServiceConfig);
        builder.blackboxTvm2ClientConfig(tvm2ClientConfig);
        builder.corpBlackboxTvm2ClientConfig(tvm2ClientConfig);
        builder.filterSearchTvm2ClientConfig(tvm2ClientConfig);
        builder.corpFilterSearchTvm2ClientConfig(tvm2ClientConfig);

        builder.multisearchConfig(null);
        builder.iexProxyConfig(Configs.hostConfig(iexProxy));

        builder.enlargeConfig(
            new EnlargeConfigBuilder(
                new IniConfig(
                    new StringReader(
                        "tabs = pg:/search/tabs?prefix=${uid}&get=*"
                            + "&text=message_type:4&length=10"
                            + "&hr\n"
                        + "facts = change_log:/search/iex?prefix=${uid}"
                            + "&get=fact_mid&text=fact_mid:*"
                            + "&length=10\n"))));

        builder.subscriptionsConfig().pinnedDisplayNames(
            new File(
                Paths.getSourcePath(
                    "mail/search/mail/msearch_proxy_service_config/files"
                    + "/subscriptions.pinned.names")));
        if (clusterContext.useSocheck()) {
            SoCheckConfigBuilder soCheckConfig = new SoCheckConfigBuilder();
            new URIConfigBuilder(Configs.uriConfig(soCheck, "/check"))
                .copyTo(soCheckConfig);
            soCheckConfig.banSpam(true);
            soCheckConfig.fakeRequest("Привет");
            soCheckConfig.fakeRequestInterval(2);
            builder.soCheckConfig(soCheckConfig);
        } else {
            builder.soCheckConfig(null);
        }

        HttpTargetConfigBuilder suggestSearch =
            new HttpTargetConfigBuilder(
                Configs.targetConfig())
                .connectTimeout(TIMEOUT)
                .poolTimeout(TIMEOUT)
                .sessionTimeout(TIMEOUT);
        builder.suggestConfig(
            new SuggestConfigBuilder(
                SuggestConfigDefaults.INSTANCE)
                .searchClientConfig(suggestSearch));

        builder.indexSearchRequests(true);
        builder.topRelevant(clusterContext.topRelevant());
        builder.proxyConfig()
            .timeout(TIMEOUT)
            .poolTimeout(TIMEOUT)
            .connections(2);
        builder.keyboardBackendConfig(Configs.targetConfig());
        builder.corpMlConfig(Configs.hostConfig(corpMl));

        if (clusterContext.useErratum()) {
            builder.erratumConfig(
                new ErratumConfigBuilder()
                    .connections(2)
                    .timeout(TIMEOUT)
                    .poolTimeout(TIMEOUT)
                    .host(erratum.host())
                    .service("mail-search"));
        } else {
            builder.erratumConfig(null);
        }

        if (clusterContext.useMatrixnet()) {
            builder.rankingConfig(clusterContext.matrixnet());
            builder.rankingConfig().excludedExperiments(
                Collections.singleton("40425"));
        } else {
            builder.rankingConfig(RankingConfigDefaults.INSTANCE);
        }

        if (clusterContext.useUserSplit()) {
            builder.userSplitConfig(
                new HttpHostConfigBuilder()
                    .connections(2)
                    .timeout(TIMEOUT)
                    .poolTimeout(TIMEOUT)
                    .host(userSplit.host()));
        } else {
            builder.userSplitConfig(null);
        }

        if (clusterContext.useProducer()) {
            ProducerClientConfigBuilder producerBuilder =
                new ProducerClientConfigBuilder()
                    .connections(2)
                    .timeout(TIMEOUT)
                    .poolTimeout(TIMEOUT)
                    .host(producer.host())
                    .allowCached(true);

            if (clusterContext.fallbackSearchMap()) {
                producerBuilder.fallbackToSearchMap(true);
            }

            builder.producerClientConfig(producerBuilder);
            builder.producerStoreConfig(producerBuilder);
        } else {
            builder.producerClientConfig(null);
            builder.producerStoreConfig(null);
        }

        builder.subscriptionsConfig()
            .connections(2)
            .timeout(TIMEOUT)
            .poolTimeout(TIMEOUT);

        if (iniConfig == null) {
            builder.pureSearch(true);
            Map<String, String> filters = new HashMap<>();
            filters.put(
                "trips",
                "((message_type:((s_aviaticket OR s_travel OR s_zdticket) "
                    + "AND NOT (news OR personalnews OR people)) "
                    + "AND NOT has_user_type:1) OR user_type:trips)");
            filters.put(
                "people",
                "((message_type:people AND NOT has_user_type:1) OR user_type:people)");
            filters.put(
                "social",
                "((message_type:64 AND NOT has_user_type:1) OR user_type:social)");
            filters.put("hamon", "lids:${user.hamon.lid}");
            builder.filtersConfig().rawFilters(filters);
        }

        builder.dkimStatsConfig(DkimStatsConfigDefaults.INSTANCE)
            .dkimStatsConfig()
            .cacheTtl(clusterContext.dkimCacheTtl())
            .cacheCapacity(1048576)
            .updateConcurrency(2)
            .retryInterval(100L);

        if (clusterContext.usePostgres()) {
            PreparedDbProvider provider =
                    PreparedDbProvider.forPreparer(LiquibasePreparer.forClasspathLocation("init.xml"), new ArrayList<>());

            builder.pgPoolConfig(AddressPgConfigDefaults.INSTANCE)
                    .pgPoolConfig()
                    .url(provider.createDatabase())
                    .user("postgres")
                    .password("postgres")
                    .driverName("org.postgresql.Driver")
                    .pingQuery("select 1");
        } else {
            builder.pgPoolConfig(null);
        }

        return builder.build();
    }

    public MsearchProxyCluster(
        final TestBase testBase,
        boolean useProducer,
        boolean useErratum,
        boolean usePostgres)
        throws Exception
    {
        this(
            testBase,
            new MproxyClusterContext()
                    .producer(useProducer)
                    .erratum(useErratum)
                    .usePostgres(usePostgres)
        );
    }

    public MsearchProxyCluster(
        final TestBase testBase,
        final MproxyClusterContext clusterContext)
        throws Exception
    {
        this(testBase, null, clusterContext);
    }

    public MsearchProxyCluster(
        final TestBase testBase,
        final String configPath)
        throws Exception
    {
        this(testBase, configPath, new MproxyClusterContext());
    }

    public MsearchProxyCluster(
        final TestBase testBase,
        final String configPath,
        final MproxyClusterContext clusterContext)
        throws Exception
    {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            chain.get().add(() -> testBase.clearContext());

            backend = new TestMailSearchBackend(testBase);
            chain.get().add(backend);

            filterSearch =
                new StaticServer(Configs.baseConfig("FilterSearch"));
            chain.get().add(filterSearch);
            filterSearch.add("/labels?*", "{\"labels\":{}}");
            filterSearch.add("/folders?*", "{\"folders\":{}}");

            corpFilterSearch =
                new StaticServer(Configs.baseConfig("CorpFilterSearch"));
            chain.get().add(corpFilterSearch);
            corpFilterSearch.add("/labels?*", "{\"labels\":{}}");
            corpFilterSearch.add("/folders?*", "{\"folders\":{}}");

            blackbox =
                StaticServer.fromContext(
                    testBase,
                    "Blackbox",
                    "BLACKBOX_HOST",
                    chain.get());
            blackbox.add(blackboxUri(0L), blackboxResponse(0L, 1L, "pg"));

            corpMl = new StaticServer(Configs.baseConfig("CorpML"));
            chain.get().add(corpMl);

            corpBlackbox =
                StaticServer.fromContext(
                    testBase,
                    "CorpBlackbox",
                    "CORP_BLACKBOX_HOST",
                    chain.get());
            corpBlackbox.add(
                blackboxUri(BlackboxUserinfo.CORP_UID_BEGIN),
                blackboxResponse(
                    BlackboxUserinfo.CORP_UID_BEGIN,
                    BlackboxUserinfo.CORP_UID_BEGIN + 1L,
                    "mdb100"));

            tvm2 = FakeTvmServer.fromContext(testBase, chain.get());

            tvm2.addTicket(MOPS_TVM_ID, MOPS_TVM_TICKET);
            tvm2.addTicket(4, "here the ticket");
            tvm2.addTicket(FILTER_SEARCH_TVM_ID, FILTER_SEARCH_TVM_TICKET);
            tvm2.addTicket(
                CORP_FILTER_SEARCH_TVM_ID,
                CORP_FILTER_SEARCH_TVM_TICKET);

            if (clusterContext.useProducer()) {
                producer = new StaticServer(Configs.baseConfig("Producer"));
                chain.get().add(producer);
            } else {
                producer = null;
            }

            if (clusterContext.useErratum()) {
                erratum = new StaticServer(Configs.baseConfig("Erratum"));
                chain.get().add(erratum);
            } else {
                erratum = null;
            }

            if (clusterContext.useUserSplit()) {
                userSplit = new StaticServer(Configs.baseConfig("UserSplit"));
                chain.get().add(userSplit);
            } else {
                userSplit = null;
            }

            iexProxy = new StaticServer(Configs.baseConfig("IexProxy"));
            chain.get().add(iexProxy);

            if (clusterContext.useSocheck()) {
                soCheck = new StaticServer(Configs.baseConfig("SoCheck"));
                chain.get().add(soCheck);
            } else {
                soCheck = null;
            }

            threadSearch =
                new StaticServer(Configs.baseConfig("ThreadSearch"));
            chain.get().add(threadSearch);

            if (clusterContext.useMops()) {
                mops = new StaticServer(Configs.baseConfig("MopsServer"));
            } else {
                mops = null;
            }

            tupita = new StaticServer(Configs.baseConfig("Tupita"));
            chain.get().add(tupita);
            furita = new StaticServer(Configs.baseConfig("Furita"));
            chain.get().add(furita);

            ImmutableMsearchProxyConfig config;
            if (configPath == null) {
                iniConfig = null;
                config = config(clusterContext, null);
            } else {
                initProperty("TVM_API_HOST");
                initProperty("TVM_CLIENT_ID");
                initProperty("CORP_ML_TVM_ID");
                String mopsHost;
                if (mops == null) {
                    mopsHost = "localhost";
                } else {
                    mopsHost = mops.host().toString();
                }
                System.setProperty("MOPS_HOST", mopsHost);
                System.setProperty("MOPS_TVM_ID", MOPS_TVM_ID);
                initProperty("BLACKBOX_CLIENT_ID");
                initProperty("CORP_BLACKBOX_CLIENT_ID");
                System.setProperty(
                    "FILTER_SEARCH_TVM_ID",
                    FILTER_SEARCH_TVM_ID);
                System.setProperty( // backward compatibility
                    "FILTERSEARCH_CLIENT_ID",
                    FILTER_SEARCH_TVM_ID);
                System.setProperty(
                    "CORP_FILTER_SEARCH_TVM_ID",
                    CORP_FILTER_SEARCH_TVM_ID);
                System.setProperty( // backward compatibility
                    "CORP_FILTERSEARCH_CLIENT_ID",
                    CORP_FILTER_SEARCH_TVM_ID);
                initProperty("SECRET");
                initProperty("BSCONFIG_IDIR");
                initProperty("SERVER_NAME");
                initProperty("JKS_PASSWORD");
                initProperty("TVM_BP_ALLOWED_SRCS");
                initProperty("TVM_CORP_ALLOWED_SRCS");
                initProperty("TVM_ALLOWED_SUBSCRIPTIONS");
                initProperty("TVM_ALLOWED_ASYNC_FURITA");
                initProperty("TVM_ALLOWED_CHEMODAN");
                initProperty("TVM_ALLOWED_CORP_ASYNC_FURITA");
                initProperty("TVM_ALLOWED_ML");
                initProperty("TVM_SPANIEL_ALLOWED");
                initProperty("TVM_ALLOWED_MULTISEARCH");
                System.setProperty("DISK_PROXY_HOST", "localhost");
                System.setProperty("PS3183ATTACHSHIELD", "false");

                System.setProperty(
                    "FILTER_SEARCH_HOST",
                    filterSearch.host().toString());
                System.setProperty(
                    "CORP_FILTER_SEARCH_HOST",
                    corpFilterSearch.host().toString());
                if (producer != null) {
                    System.setProperty(
                        "PRODUCER_HOST",
                        producer.host().toString());
                } else {
                    System.setProperty("PRODUCER_HOST", "localhost");
                }

                System.setProperty("WEBATTACH_HOST", "localhost");
                System.setProperty("CORP_WEBATTACH_HOST", "localhost");
		System.setProperty("CPU_CORES", "2");

                iniConfig = patchConfig(
                    new IniConfig(
                        new File(
                            Paths.getSourcePath(
                                "mail/search/mail/msearch_proxy_service_config"
                                + "/files/msearch-proxy-prod.conf"))));

                iniConfig.section("server").sections().remove("https");
                if (iniConfig.sectionOrNull("mops") != null) {
                    iniConfig.section("mops").sections().remove("https");
                }

                config = config(clusterContext, iniConfig);
            }

            Logger.init(
                config.loggers().preparedLoggers().asterisk(),
                config.errorLogConfig().build(
                    config.loggers().handlersManager()));
            SearchMap.init(
                new StringReader(config.searchMapConfig().content()));
            Searcher.init(new IniConfig(new StringReader("")));
            DispatcherFactory.init(
                new IniConfig(new StringReader("")),
                config);
            proxy = new HttpServer(
                config,
                new Api(new IniConfig(new StringReader("")), config));
            if (iniConfig != null) {
                iniConfig.checkUnusedKeys();
            }

            chain.get().add(proxy);
            reset(chain.release());
        }
    }

    public static void initProperty(final String propertyName) {
        System.setProperty(propertyName, System.getProperty(propertyName, ""));
    }

    public static String blackboxUri(final long uid) {
        return "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
            + "&dbfields=hosts.db_id.2,subscription.suid.2&sid=2&uid=" + uid;
    }

    public static String blackboxUriSuid(final long uid) {
        return "/blackbox/?format=json&method=userinfo&userip=127.0.0.1"
            + "&dbfields=subscription.suid.2&sid=2&uid=" + uid;
    }

    public static String blackboxResponse(
        final long uid,
        final long suid,
        final String db)
    {
        return "{\"users\":[{\"id\":\"" + uid
            + "\",\"uid\":{\"value\":\"" + uid
            + "\",\"lite\":false,\"hosted\":false},\"login\":\"user" + uid
            + "\",\"have_password\":true,\"have_hint\":true,\"karma\":{"
            + "\"value\":0},\"karma_status\":{\"value\":6000},"
            + "\"dbfields\":{\"subscription.suid.2\":\"" + suid
            + "\",\"hosts.db_id.2\":\"" + db + "\"}}]}";
    }

    public void start() throws IOException {
        filterSearch.start();
        corpFilterSearch.start();
        threadSearch.start();
        iexProxy.start();
        corpMl.start();
        if (producer != null) {
            producer.start();
        }

        if (mops != null) {
            mops.start();
        }
        if (erratum != null) {
            erratum.start();
        }
        if (userSplit != null) {
            userSplit.start();
        }
        if (soCheck != null) {
            soCheck.start();
        }

        tupita.start();
        furita.start();
        proxy.start();
    }

    public TestSearchBackend backend() {
        return backend;
    }

    public StaticServer filterSearch() {
        return filterSearch;
    }

    public StaticServer corpFilterSearch() {
        return corpFilterSearch;
    }

    public StaticServer blackbox() {
        return blackbox;
    }

    public StaticServer corpBlackbox() {
        return corpBlackbox;
    }

    public StaticServer producer() {
        return producer;
    }

    public HttpServer proxy() {
        return proxy;
    }

    public StaticServer erratum() {
        return erratum;
    }

    public StaticServer userSplit() {
        return userSplit;
    }

    public StaticServer iexProxy() {
        return iexProxy;
    }

    public StaticServer soCheck() {
        return soCheck;
    }

    public StaticServer threadSearch() {
        return threadSearch;
    }

    public StaticServer corpMl() {
        return corpMl;
    }

    public StaticServer mops() {
        return mops;
    }

    public StaticServer tupita() {
        return tupita;
    }

    public StaticServer furita() {
        return furita;
    }

    public static IniConfig patchConfig(
        final IniConfig config)
        throws Exception
    {
        config.sections().remove("access_log");
        config.sections().remove("index_log");
        config.sections().remove("auth");
        config.sections().remove("index_access_log");
        config.sections().remove("error_log");
        config.sections().remove("full_log");
        config.sections().remove("tskv_log");
        config.sections().remove("mail-search-relevance");
        config.sections().remove("tvm2");
        config.section("corp-blackbox").sections().remove("tvm2");
        config.section("blackbox").sections().remove("tvm2");
        config.sections().remove("searchmap");
        config.sections().remove("dispatcher_factory");
        config.sections().remove("mail_searcher");
        config.sections().remove("api");
        config.sections().remove("folderlist");
        config.sections().remove("mop");
        IniConfig server = config.sectionOrNull("server");
        server.sections().remove("free-space-signals");
        server.put("port", Integer.toString(0));
        server.put("workers.percent", Integer.toString(0));
        server.put("workers.min", Integer.toString(2));
        for (IniConfig section: config.sections().values()) {
            if (section.keys().contains("uri")) {
                URI uri = section.getURI("uri");
                uri = new URI(uri.getScheme().toLowerCase(Locale.ROOT),
                    "localhost:0",
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment());

                section.put("uri", uri.toString());
            }

            if (section.keys().contains("host")) {
                section.put("host", "http://localhost:0");
            }
        }

        return config;
    }

    public static final class MproxyClusterContext {
        private boolean useErratum = false;
        private boolean useProducer = false;
        private boolean useMops = false;
        private boolean fallbackSearchMap = false;
        private boolean useUserSplit = false;
        private boolean topRelevant = false;
        private boolean useSocheck = false;
        private RankingConfig matrixnetRanking = null;
        private String searchMap = null;
        private Map<String, String> dnsHostsMapping = Collections.emptyMap();
        private long dkimCacheTtl = 0L;
        private boolean usePostgres = false;

        public MproxyClusterContext erratum(boolean use) {
            this.useErratum = use;
            return this;
        }

        public boolean useUserSplit() {
            return useUserSplit;
        }

        public MproxyClusterContext userSplit() {
            this.useUserSplit = true;
            return this;
        }

        public boolean useErratum() {
            return useErratum;
        }

        public MproxyClusterContext producer() {
            return producer(true);
        }

        public MproxyClusterContext producer(
            final boolean use,
            final boolean fallback)
        {
            this.useProducer = use;
            this.fallbackSearchMap = fallback;
            return this;
        }

        public MproxyClusterContext producer(boolean use) {
            return producer(use, false);
        }

        protected boolean useProducer() {
            return useProducer;
        }

        public MproxyClusterContext matrixnet(final RankingConfig config) {
            this.matrixnetRanking = config;
            return this;
        }

        public boolean useMatrixnet() {
            return matrixnetRanking != null;
        }

        public RankingConfig matrixnet() {
            return matrixnetRanking;
        }

        public boolean topRelevant() {
            return topRelevant;
        }

        public MproxyClusterContext enableTopRelevant() {
            this.topRelevant = true;
            return this;
        }

        public String searchMap() {
            return searchMap;
        }

        public MproxyClusterContext searchMap(final String searchMap) {
            this.searchMap = searchMap;
            return this;
        }

        public boolean fallbackSearchMap() {
            return fallbackSearchMap;
        }

        public MproxyClusterContext useSocheck(final boolean useSocheck) {
            this.useSocheck = useSocheck;
            return this;
        }

        public boolean useSocheck() {
            return useSocheck;
        }

        public MproxyClusterContext dnsHostsMapping(
            final Map<String, String> dnsHostsMapping)
        {
            this.dnsHostsMapping = dnsHostsMapping;
            return this;
        }

        public Map<String, String> dnsHostsMapping() {
            return dnsHostsMapping;
        }

        public MproxyClusterContext dkimCacheTtl(final long dkimCacheTtl) {
            this.dkimCacheTtl = dkimCacheTtl;
            return this;
        }

        public long dkimCacheTtl() {
            return dkimCacheTtl;
        }

        public boolean useMops() {
            return useMops;
        }

        public MproxyClusterContext useMops(final boolean useMops) {
            this.useMops = useMops;
            return this;
        }

        public boolean usePostgres() {
            return usePostgres;
        }

        public MproxyClusterContext usePostgres(boolean usePostgres) {
            this.usePostgres = usePostgres;
            return this;
        }
    }
}

