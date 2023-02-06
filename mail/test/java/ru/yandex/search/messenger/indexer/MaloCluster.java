package ru.yandex.search.messenger.indexer;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.apache.http.protocol.HttpRequestHandler;

import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.config.HttpHostConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.ProxyMultipartHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.string.URIParser;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;

public class MaloCluster
    extends MaloBaseCluster
    implements GenericAutoCloseable<IOException>
{
    // CSOFF: MultipleStringLiterals
    private static final String MALO_CONFIG =
        Paths.getSourcePath(
            "mail/search/messenger/malo_service/files/malo.conf");

    private final Malo malo;
    private final StaticServer moxy;

    private final GenericAutoCloseableChain<IOException> chain;

    public MaloCluster(final TestBase base) throws Exception {
        super(base);
        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
                 new GenericAutoCloseableHolder<>(
                     new GenericAutoCloseableChain<>()))
        {
            System.setProperty("PRODUCER_HOST", producer().host().toString());
            System.setProperty("TVM_API_HOST", "");
            System.setProperty("TVM_CLIENT_ID", "");
            System.setProperty("TVM_ALLOWED_SRCS", "");
            System.setProperty("SECRET", "");
            System.setProperty("SERVER_NAME", "");
            System.setProperty("JKS_PASSWORD", "");
            System.setProperty("INDEX_PATH", "");
            System.setProperty("MAIL_SEARCH_TVM_ID", "0");
            System.setProperty("YT_ACCESS_LOG", "");
            System.setProperty("SERVICE_CONFIG", "null.conf");
            System.setProperty("BSCONFIG_IPORT", "0");
            System.setProperty("CMNT_API", "empty-mail-producer");
            System.setProperty("MOXY_HOST", "localhost");
            System.setProperty("MESSENGER_ROUTER_TVM_CLIENT_ID", "");
            System.setProperty("MESSENGER_ROUTER_HOST", "localhost");
            System.setProperty("MAIL_PRODUCER", "empty-mail-producer");
            System.setProperty("CHATS_SERVICE", "chats_service");
            System.setProperty("USERS_SERVICE", "users_service");
            System.setProperty("MESSAGES_SERVICE", "messages_service");
            System.setProperty("TASKS_SERVICE", "tasks_service");
            System.setProperty("META_API_HOST", "");
            System.setProperty("META_API_TVM_CLIENT_ID", "");
            System.setProperty("TVM_CLIENT_ID", "");
            System.setProperty("ORG_USERS_SERVICE", "messenger_users");
            System.setProperty("ORG_CHATS_SERVICE", "messenger_chats");

            producer.add(
                "*",
                new StaticHttpResource(
                    new ProxyMultipartHandler(searchBackend().indexerPort())));


            moxy = new StaticServer(Configs.baseConfig("MoxyStaticServer"));
            chain.get().add(moxy);
            moxy.start();

            MaloConfigBuilder builder = new MaloConfigBuilder(
                    patchMaloConfig(
                        new IniConfig(
                            new File(MALO_CONFIG))));

            builder.moxy(Configs.hostConfig(moxy));
            builder.producer(Configs.hostConfig(producer));
            builder.chats(Configs.uriConfig(metaApi, "/meta_api/"));
            builder.users(Configs.uriConfig(metaApi, "/meta_api/"));
            builder.messages().uri(
                URIParser.INSTANCE.apply(
                    router.host().toString() + builder.messages().uri().getPath()));
            builder.rca(new HttpHostConfigBuilder()
                .host(rca().host())
                .connections(2));

            malo = new Malo(builder.build());

            this.chain = chain.release();

            Map<String, HttpRequestHandler> moxyMap = new LinkedHashMap<>();
            UnaryOperator<String> seqProc = (s) -> s.substring("/sequential".length());
            moxyMap.put(
                builder.chatsService(),
                new ProxyHandler(chatsBackend().searchPort(), seqProc));
            moxyMap.put(
                builder.usersService(),
                new ProxyHandler(chatsBackend().searchPort(), seqProc));
            moxyMap.put(
                builder.messagesService(),
                new ProxyHandler(messagesBackend().searchPort(), seqProc));

            addIndexingRoutes(producer, builder);
            moxy.add("/sequential*", new StaticHttpResource(new SelectByServiceHandler(moxyMap)));
            malo.start();
        }
    }

    public StaticServer moxy() {
        return moxy;
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

    public Malo malo() {
        return malo;
    }

    public TestSearchBackend searchBackend() {
        return chatsBackend;
    }

    public StaticServer producer() {
        return producer;
    }
}
