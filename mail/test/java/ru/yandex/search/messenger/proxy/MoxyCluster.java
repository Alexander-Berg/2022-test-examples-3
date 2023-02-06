package ru.yandex.search.messenger.proxy;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

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
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.searchmap.SearchMapConfigBuilder;
import ru.yandex.parser.searchmap.User;
import ru.yandex.parser.string.URIParser;
import ru.yandex.search.messenger.indexer.Malo;
import ru.yandex.search.messenger.indexer.MaloBaseCluster;
import ru.yandex.search.messenger.indexer.MaloConfigBuilder;
import ru.yandex.search.messenger.proxy.config.MoxyConfigBuilder;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.search.prefix.PrefixType;
import ru.yandex.search.proxy.UpstreamConfigBuilder;
import ru.yandex.search.proxy.UpstreamsConfigBuilder;
import ru.yandex.test.util.TestBase;

public class MoxyCluster
    extends MaloBaseCluster
    implements GenericAutoCloseable<IOException>
{
    // CSOFF: MultipleStringLiterals

    private static final String MOXY_CONFIG =
        Paths.getSourcePath(
            "mail/search/messenger/moxy_service/files/moxy_messenger.conf");
    private static final String MALO_CONFIG =
        Paths.getSourcePath(
            "mail/search/messenger/malo_service/files/malo.conf");

    private final Malo malo;
    private final StaticServer maloMoxyProxy;
    private final StaticServer userSplit;
    private final Moxy moxy;
    private final StaticServer erratum;

    private final GenericAutoCloseableChain<IOException> chain;

    public MoxyCluster(final TestBase base) throws Exception {
        super(base);

        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
                 new GenericAutoCloseableHolder<>(
                     new GenericAutoCloseableChain<>()))
        {
            System.setProperty("TVM_API_HOST", "");
            System.setProperty("TVM_CLIENT_ID", "");
            System.setProperty("TVM_ALLOWED_SRCS", "");
            System.setProperty("SECRET", "");
            System.setProperty("SERVER_NAME", "");
            System.setProperty("JKS_PASSWORD", "");
            System.setProperty("INDEX_PATH", "");
            System.setProperty("INDEX_DIR", "");
            System.setProperty("MAIL_SEARCH_TVM_ID", "0");
            System.setProperty("YT_ACCESS_LOG", "");
            System.setProperty("SERVICE_CONFIG", "null.conf");
            System.setProperty("BSCONFIG_IPORT", "0");
            System.setProperty("BSCONFIG_IDIR", ".");
            System.setProperty("OLD_SEARCH_PORT", "0");
            System.setProperty("SEARCH_PORT", "0");
            System.setProperty("INDEX_PORT", "0");
            System.setProperty("DUMP_PORT", "0");
            System.setProperty("CONSUMER_PORT", "0");
            System.setProperty("CMNT_API", "empty-mail-producer");
            System.setProperty("MOXY_HOST", "localhost");
            System.setProperty("MESSENGER_ROUTER_TVM_CLIENT_ID", "");
            System.setProperty("MESSENGER_ROUTER_HOST", "localhost");
            System.setProperty("MAIL_PRODUCER", "empty-mail-producer");
            System.setProperty("CHATS_SERVICE", "messenger_chats");
            System.setProperty("USERS_SERVICE", "messenger_users");
            System.setProperty("MESSAGES_SERVICE", "messenger_messages");
            System.setProperty("TASKS_SERVICE", "tasks_service");
            System.setProperty("META_API_HOST", "");
            System.setProperty("META_API_TVM_CLIENT_ID", "");
            System.setProperty("TVM_CLIENT_ID", "");
            System.setProperty("CPU_CORES", "2");
            System.setProperty("SEARCH_THREADS", "2");
            System.setProperty("MERGE_THREADS", "2");
            System.setProperty("LIMIT_SEARCH_REQUESTS", "2");
            System.setProperty("INDEX_THREADS", "2");
            System.setProperty("MOXY_TVM_CONF", "moxy-notvm.conf");
            System.setProperty("HOSTNAME", "localhost");
            System.setProperty("NGINX_SSL_PORT", "0");
            System.setProperty("FULL_LOG_LEVEL", "all");
            System.setProperty("PROXY_WORKERS", "2");
            System.setProperty("LIMIT_RECENT_REQUESTS", "2");
            System.setProperty("REC_CHANNELS_GOOD_VERDICTS", "any");
            System.setProperty("ORG_USERS_SERVICE", "messenger_users");
            System.setProperty("ORG_CHATS_SERVICE", "messenger_chats");

            System.setProperty("PRODUCER_HOST", producer.host().toString());

            MaloConfigBuilder maloBuilder = new MaloConfigBuilder(
                patchMaloConfig(
                    new IniConfig(
                        new File(MALO_CONFIG))));

            erratum = new StaticServer(Configs.baseConfig());
            chain.get().add(erratum);
            erratum.start();

            maloMoxyProxy = new StaticServer(Configs.baseConfig());
            chain.get().add(maloMoxyProxy);
            maloMoxyProxy.start();

            maloBuilder.moxy(Configs.hostConfig(maloMoxyProxy));
            maloBuilder.producer(Configs.hostConfig(producer));
            maloBuilder.messages().uri(
                URIParser.INSTANCE.apply(router.host().toString() + maloBuilder.messages().uri().getPath()));
            maloBuilder.chats(Configs.uriConfig(metaApi, "/meta_api/"));
            maloBuilder.users(Configs.uriConfig(metaApi, "/meta_api/"));
            malo = new Malo(maloBuilder.build());
            chain.get().add(malo);

            System.setProperty("PRODUCER_HOST", producer.host().toString());
            System.setProperty("BSCONFIG_IDIR", "");
            System.setProperty("BSCONFIG_IPORT", "0");
            System.setProperty("SEARCHMAP_PATH", MOXY_CONFIG);
            System.setProperty("MOXY_TVM_CONF", "moxy-notvm.conf");

            MoxyConfigBuilder moxyBuilder =
                new MoxyConfigBuilder(patchProxyConfig(new IniConfig(new File(MOXY_CONFIG))));

            moxyBuilder.port(0);
            moxyBuilder.connections(2);
            moxyBuilder.topPostConfig().loadOnStartup(false);
            //moxyBuilder.topPostConfig().hardcodedChannels(null);
            moxyBuilder.topPostConfig().cachedParams(Collections.emptySet());
            producer.add(
                "/_status*",
                "[{$localhost\0:100500}]");
            addIndexingRoutes(producer, maloBuilder);

            chatsBackend.setQueueId(
                new User(maloBuilder.chatsService(),  new LongPrefix(0L)),
                100500L);
            chatsBackend.setQueueId(
                new User(maloBuilder.usersService(),  new LongPrefix(0L)),
                100500L);

            userSplit = new StaticServer(Configs.baseConfig());
            chain.get().add(userSplit);
            userSplit.start();
            userSplit.add("*", new StaticHttpResource(200));

            moxyBuilder.searchMapConfig(
                new SearchMapConfigBuilder()
                    .content(
                        chatsBackend.searchMapRule(moxyBuilder.usersService())
                            + chatsBackend.searchMapRule(moxyBuilder.chatsService())
                            + messagesBackend.searchMapRule(
                            moxyBuilder.messagesService(),
                            PrefixType.STRING)));

            moxyBuilder.recommendedChannelsConfig().cacheEnabled(false);
            moxyBuilder.searchConfig(Configs.targetConfig());
            moxyBuilder.mssngrRouterConfig(Configs.hostConfig(router));

            moxyBuilder.searchConfig(
                Configs.hostConfig(messagesBackend.searchHost()));
            moxyBuilder.indexerConfig(
                Configs.hostConfig(messagesBackend.indexerHost()));
            moxyBuilder.upstreamsConfig(
                new UpstreamsConfigBuilder().asterisk(
                    new UpstreamConfigBuilder().connections(2)));

            ErratumConfigBuilder erratumBuilder = new ErratumConfigBuilder();
            new HttpHostConfigBuilder(Configs.hostConfig(erratum))
                .copyTo(erratumBuilder);
            erratumBuilder.service("disk_search");
            moxyBuilder.misspellConfig(erratumBuilder);
            moxyBuilder.recentsCache(false);

            moxy = new Moxy(moxyBuilder.build());
            chain.get().add(moxy);

            maloMoxyProxy.add("*", new StaticHttpResource(new ProxyHandler(moxy.port())));

            malo.start();
            moxy.start();
            this.chain = chain.release();
        }
    }

    public StaticServer metaApi() {
        return metaApi;
    }

    public StaticServer userSplit() {
        return userSplit;
    }

    public Moxy moxy() {
        return moxy;
    }

    public Malo malo() {
        return malo;
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }

    private IniConfig patchMaloConfig(
        final IniConfig config)
        throws Exception
    {
        config.sections().remove("log");
        config.sections().remove("accesslog");
        config.sections().remove("tvm2");
        config.sections().remove("auth");
        config.section("http").sections().remove("free-space-signals");
        config.section("chats").sections().remove("https");
        config.section("users").sections().remove("https");
        config.section("messages").sections().remove("https");
        IniConfig server = config.section("server");
        server.put("port", "0");
        config.section("searchmap").put("file", null);
        return config;
    }

    public StaticServer erratum() {
        return erratum;
    }

    private static IniConfig patchProxyConfig(
        final IniConfig config)
        throws Exception
    {
        config.sections().remove("log");
        config.sections().remove("accesslog");
        config.sections().remove("tvm2");
        config.sections().remove("auth");
        config.sections().remove("server");
        config.sections().remove("router");
        config.sections().remove("top-posts");

        config.section("searchmap").put("file", null);
        return config;
    }
}
