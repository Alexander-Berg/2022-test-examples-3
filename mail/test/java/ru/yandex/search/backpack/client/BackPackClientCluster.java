package ru.yandex.search.backpack.client;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpStatus;

import ru.yandex.client.tvm2.Tvm2ClientConfigBuilder;
import ru.yandex.client.tvm2.Tvm2ServiceConfigBuilder;
import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.config.HttpHostConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.searchmap.SearchMapConfigBuilder;
import ru.yandex.passport.tvmauth.Version;
import ru.yandex.test.search.backend.TestDiskSearchBackend;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;


public class BackPackClientCluster extends GenericAutoCloseableHolder<
    IOException,
    GenericAutoCloseableChain<IOException>>
{
    public static final String ANSWER = "OK";
    public static final String URI = "/ping";

    private static final long TVM2_RENEWAL_INTERVAL = 60000L;

    private static final String HOMEDIR =
            Paths.getSourcePath(
                    "mail/search/backpack/test/client/");

    private static final String CLIENT_CONFIG =
            Paths.getSourcePath(
                    "mail/search/backpack/test/client/backpack_bacchus.conf");

    public static final String MDS_TVM2_TICKET = "3:serv:MDSTICKETn";

    private final BackPackClient backpackClient;
    private final StaticServer backpackClientServer;
    private final StaticServer tvm;
    //private final StaticServer producer;
    private final TestSearchBackend searchBackend;


    public BackPackClientCluster(final TestBase testBase) throws Exception {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                     new GenericAutoCloseableHolder<>(
                             new GenericAutoCloseableChain<>()))
        {

            // here we create backend for disk, we need index some docs and backup it

            this.searchBackend = new TestDiskSearchBackend(testBase);

            System.setProperty("TVM_API_HOST", "");
            System.setProperty("TVM_CLIENT_ID", "");
            System.setProperty("TVM_ALLOWED_SRCS", "");
            System.setProperty("SECRET", "");
            System.setProperty("HOMEDIR", HOMEDIR);

            String secret = "1234567890123456789011";

            BackPackClientConfigBuilder backpackClientConfig = new BackPackClientConfigBuilder( patchProxyConfig(new IniConfig(
                    new File(CLIENT_CONFIG))));

            tvm = new StaticServer(Configs.baseConfig("Tvm"));
            chain.get().add(tvm);
            tvm.add("/ticket/", HttpStatus.SC_OK);
            tvm.add(
                    "/2/keys/?lib_version=" + Version.get(),
                    IOStreamUtils.consume(
                            StaticServer.class.getResourceAsStream("tvm-keys.txt"))
                            .processWith(ByteArrayEntityFactory.INSTANCE));
            tvm.add(
                    "/2/ticket/",
                    "{\"4\":{\"ticket\":\"" + MDS_TVM2_TICKET + "\"}}");
            tvm.start();


            Tvm2ServiceConfigBuilder tvm2ServiceConfig =
                    new Tvm2ServiceConfigBuilder();
            new HttpHostConfigBuilder(Configs.hostConfig(tvm))
                    .copyTo(tvm2ServiceConfig);
            tvm2ServiceConfig.clientId(1);
            tvm2ServiceConfig.secret(secret);

            backpackClientConfig.tvm2ServiceConfig(tvm2ServiceConfig);
            backpackClientConfig.tvm2ClientConfig(
                    new Tvm2ClientConfigBuilder()
                            .destinationClientId("4")
                            .renewalInterval(TVM2_RENEWAL_INTERVAL));

            backpackClientConfig.tvm2ServiceConfig().host(tvm.host());

            backpackClientConfig.nameSpace("sandbox-tmp");
            //backpackClientConfig.backupPath("/test/testdata");
            backpackClientConfig.expire("7d");
            backpackClientConfig.directUpload(false);

        backpackClientConfig.searchMapConfig(new SearchMapConfigBuilder()
                    .content("change_log iNum:0,"
                    + "tag:sas1-0285_18073,"
                    + "host:sas1-0285.search.yandex.net,shards:32768-32798,"
                    + "zk:myt1-1496.search.yandex.net:18662/18663"
                    + "|myt1-1821.search.yandex.net:18662/18663"
                    + "|sas1-9317.search.yandex.net:18662/18663"
                    + "|myt1-1487.search.yandex.net:18662/18663"
                    + "|man1-7049.search.yandex.net:18662/18663"
                    + "|sas1-9184.search.yandex.net:18662/18663"
                    + "|man1-6366.search.yandex.net:18662/18663"
                    + "|man1-7970.search.yandex.net:18662/18663"
                    + "|sas1-9224.search.yandex.net:18662/18663,"
                    + "json_indexer_port:18077,"
                    + "search_port_ng:18074,"
                    + "search_port:18073"));

            backpackClient = new BackPackClient(backpackClientConfig.build(), searchBackend.lucene().index());
            chain.get().add(backpackClient);
            backpackClientServer = new StaticServer(Configs.baseConfig("Queue"));
            chain.get().add(backpackClientServer);
            // register - для честного хттп сервера
            //        register(
            //        new Pattern<>(QUEUE_URI, false),
            //        new StaticHttpItem(QUEUE_TEXT);
            reset(chain.release());
        }
    }

    private IniConfig patchProxyConfig(
            final IniConfig config)
    {
        config.sections().remove("log");
        config.sections().remove("accesslog");
        config.sections().remove("tvm2");
        config.sections().remove("auth");
        IniConfig server = config.section("server");
        server.sections().remove("free-space-signals");
        server.sections().remove("https");
        server.put("port", "0");
        config.section("searchmap").put("file", null);
        return config;
    }

    public void start() throws IOException {
        backpackClientServer.start();
        backpackClient.start();
    }

    public StaticServer backpackClientServer() {
        return backpackClientServer;
    }

    public BackPackClient backpackClient() {
        return backpackClient;
    }

    public TestSearchBackend searchBackend() { return searchBackend; }
}
