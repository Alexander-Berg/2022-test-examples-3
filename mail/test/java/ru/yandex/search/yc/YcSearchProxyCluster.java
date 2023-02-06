package ru.yandex.search.yc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.http.HttpStatus;

import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.searchmap.SearchMap;
import ru.yandex.search.prefix.Prefix;
import ru.yandex.search.prefix.PrefixType;
import ru.yandex.search.yc.config.YCProxyConfig;
import ru.yandex.search.yc.config.YCProxyConfigBuilder;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;

public class YcSearchProxyCluster implements GenericAutoCloseable<IOException> {
    // CSOFF: MultipleStringLiterals
    public static final String MARKETPLACE_SERVICE = "yc_change_log_marketplace_compute_preprod";
    public static final String CLOUD_PROXY_CONFIG =
        Paths.getSourcePath("mail/search/yc/yc_search_proxy/debian/files/yc_search_proxy_cloud.conf");
    public static final String LUCENE_CONFIG =
        Paths.getSourcePath(
            "mail/search/yc/yc_backend/files/yc_search_backend.conf");
    private static final String PROXY_CONFIG =
        Paths.getSourcePath(
            "mail/search/yc/yc_search_proxy/files/yc_search_proxy_http.conf");

    private static final String WORK_DIR = Paths.getSourcePath("cloud/search");
    private final YcSearchProxy proxy;
    private final TestSearchBackend searchBackend;
    private final StaticServer producer;

    private final GenericAutoCloseableChain<IOException> chain;
    private final GrpcStaticServer iamServer;
    private final IamStaticService iamService;
    public YcSearchProxyCluster(final TestBase base) throws Exception {
        this (base, null);
    }

    public YcSearchProxyCluster(final TestBase base, final YCProxyConfig config) throws Exception {
        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
                 new GenericAutoCloseableHolder<>(
                     new GenericAutoCloseableChain<>()))
        {
            String tmpDir = Files.createTempDirectory(base.testName.getMethodName()).toAbsolutePath().toString();
            System.setProperty("INDEX_PATH", tmpDir);
            System.setProperty("INDEX_DIR", tmpDir);
            System.setProperty("LIMIT_SEARCH_REQUESTS", "10");
            System.setProperty("INDEX_THREADS", "5");
            System.setProperty("SEARCH_THREADS", "5");
            System.setProperty("INDEX_ACCESS_LOG_FILE", "/logs/index-access.log");
            System.setProperty("INDEX_LOG_FILE", "/logs/index.log");
            System.setProperty("ERROR_LOG_FILE", "/logs/error.log");
            System.setProperty("MERGE_THREADS", "2");
            System.setProperty("SERVER_PORT", "0");
            System.setProperty("FULL_LOG_FILE", "/logs/full.log");
            System.setProperty("ACCESS_LOG_FILE", "/logs/access.log");

            searchBackend =
                new TestSearchBackend(base, new File(LUCENE_CONFIG).toPath());
            chain.get().add(searchBackend);

            iamService = new IamStaticService();
            iamServer = new GrpcStaticServer(iamService);

            chain.get().add(iamServer);
            iamServer.start();

            producer = new StaticServer(Configs.baseConfig());
            chain.get().add(producer);
            producer.start();
            producer.add(
                "*",
                new StaticHttpResource(
                    new ProxyHandler(searchBackend().indexerPort())));


            YCProxyConfigBuilder builder;
            if (config == null) {
                builder = new YCProxyConfigBuilder(loadConfig(PROXY_CONFIG));
            } else {
                builder = new YCProxyConfigBuilder(config);
            }

            builder.producerClientConfig().host(producer.host());

            System.setProperty("YC_QUEUE", "yc_change_log");
            System.setProperty("YC_MARKETPLACE_QUEUE", MARKETPLACE_SERVICE);
            String searchmap = searchBackend.searchMapRule(YcConstants.YC_QUEUE, PrefixType.STRING);
            searchmap += searchBackend.searchMapRule(YcConstants.YC_MARKETPLACE_QUEUE, PrefixType.STRING);
            System.err.println("SEARCHMAP " + searchmap);
            builder.searchMapConfig().content(searchmap);
            builder.iamConfig().host(iamServer.host());
            proxy = new YcSearchProxy(builder.build());

            this.chain = chain.release();
            proxy.start();
        }
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }

    public static IniConfig loadConfig(final String path) throws Exception {
        System.setProperty("TVM_API_HOST", "");
        System.setProperty("TVM_CLIENT_ID", "");
        System.setProperty("YC_PROXY_SERVER_NAME", "");
        System.setProperty("TVM_ALLOWED_SRCS", "");
        System.setProperty("SECRET", "");
        System.setProperty("SERVER_NAME", "");
        System.setProperty("JKS_PASSWORD", "");
        System.setProperty("SEARCHMAP_PATH", "");
        System.setProperty("YC_SEARCH_BASE_DIR", ".");
        System.setProperty("PROXY_WORKERS", "2");
        System.setProperty("YC_INDEXER_WORK_DIR", WORK_DIR);
        System.setProperty("ALLOWED_SERVICES", "all");
        System.setProperty("IAM_HOST", "localhost");
        System.setProperty("PRODUCER_HOST", "localhost");
        System.setProperty("PRODUCER_CLOUD_HOST", "localhost");
        System.setProperty("PRODUCER_INDEXING_HOST", "localhost");

        return patchProxyConfig(new IniConfig(new File(path)));
    }

    public static IniConfig patchProxyConfig(
        final IniConfig config)
        throws Exception
    {
        config.sections().remove("log");
        config.sections().remove("accesslog");
        config.sections().remove("tvm2");
        config.sections().remove("auth");
        IniConfig server = config.section("server");
        server.sections().remove("free-space-signals");
        server.sections().remove("https");
        IniConfig iam = config.section("iam");
        iam.sections().remove("https");

        server.put("port", "0");
        config.section("searchmap").put("file", null);
        return config;
    }

    public YcSearchProxy proxy() {
        return proxy;
    }

    public TestSearchBackend searchBackend() {
        return searchBackend;
    }

    public StaticServer producer() {
        return producer;
    }

    public IamStaticService iamService() {
        return iamService;
    }

    public void addStatus(final Prefix prefix) {
        this.addStatus("yc_change_log", prefix);
    }

    public void addStatus(final String service, final Prefix prefix) {
        producer.add(
            "/_status?service=" + service + "&prefix="
                + (prefix.hash() % SearchMap.SHARDS_COUNT)
                + "&allow_cached&all&json-type=dollar",
            new StaticHttpResource(
                new StaticHttpItem(
                    HttpStatus.SC_OK,
                    "[{\"localhost\":100500}]")));
    }
    // CSON: MultipleStringLiterals
}
