package ru.yandex.passport.phone.ownership;

import java.io.File;
import java.io.IOException;

import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.passport.phone.ownership.config.PhoneOwnershipProxyConfigBuilder;
import ru.yandex.search.prefix.PrefixType;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;

public class PhoneOwnershipCluster implements GenericAutoCloseable<IOException> {
    private final GenericAutoCloseableChain<IOException> chain;
    private final StaticServer producer;
    private final StaticServer edna;
    private final EdnaHandler ednaHandler;
    private final PhoneOwnershipProxy proxy;
    private final TestSearchBackend searchBackend;
    private static final String SEARCH_BACKEND_CONFIG =
        Paths.getSourcePath(
            "mail/search/passport/phone/phone_squatter_backend/files/phone_squatter_backend.conf");
    private static final String SERVICE = "phone_squatter_test";

    public PhoneOwnershipCluster(final TestBase testBase)
        throws Exception {
        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
                 new GenericAutoCloseableHolder<>(
                     new GenericAutoCloseableChain<>())) {
            System.setProperty("PO_QUEUE", SERVICE);
            System.setProperty("LOGIN", "admin");
            System.setProperty("PASSWORD", "1234");
            System.setProperty("SERVER_PORT", "0");
            System.setProperty("PROXY_WORKERS", "2");
            System.setProperty("LIMIT_SEARCH_REQUESTS", "100");
            System.setProperty("SEARCH_THREADS", "2");
            System.setProperty("MERGE_THREADS", "2");
            System.setProperty("INDEX_THREADS", "2");
            System.setProperty("INDEX_PATH", "./");
            System.setProperty("EDNA_HOST", "localhost");

            producer = new StaticServer(Configs.baseConfig());
            edna = new StaticServer(Configs.baseConfig());
            ednaHandler = new EdnaHandler(System.getProperty("LOGIN"), System.getProperty("PASSWORD"));
            StaticHttpResource ednaHandlerResource =
                new StaticHttpResource(ednaHandler);
            edna.add("/edna/imsi*", ednaHandlerResource);
            String s = producer.host().toString();
            System.out.println("producer host in cluster: " + s);

            searchBackend =
                new TestSearchBackend(testBase, new File(SEARCH_BACKEND_CONFIG));
            chain.get().add(searchBackend);

//            s = searchBackend.indexerHost().getHostName();
//            s = searchBackend.indexerHost().toString();
//
//            s = searchBackend.searchHost().getHostName();
//            s = searchBackend.searchHost().toString();
//            String indexerPort = String.valueOf(searchBackend.indexerHost().getPort());
//            System.setProperty("INDEXER_PORT", indexerPort);
//            String searchPort = String.valueOf(searchBackend.searchHost().getPort());
//            System.setProperty("SEARCH_PORT", searchPort);

            StaticHttpResource searchBackendIndexProxy =
                new StaticHttpResource(
                    new ProxyHandler(searchBackend.indexerPort()));

//            System.err.println("indexer port: " + searchBackend.indexerPort());
//            System.err.println("search port: " + searchBackend.searchPort());

            StaticHttpResource searchBackendSearchProxy =
                new StaticHttpResource(
                    new ProxyHandler(searchBackend.searchPort()));

            producer.add("/update?*", searchBackendIndexProxy);
            producer.add("/add?*", searchBackendIndexProxy);
            producer.add("/delete?*", searchBackendIndexProxy);
            producer.add("/modify?*", searchBackendIndexProxy);
            producer.add("/ping*", searchBackendIndexProxy);
            producer.add("/search*", searchBackendSearchProxy);
            producer.add("/_status*", "[{$localhost\0:100500}]");

            PhoneOwnershipProxyConfigBuilder configBuilder = new PhoneOwnershipProxyConfigBuilder(
                PhoneOwnershipCluster.loadConfig(PhoneOwnershipCluster.PHONE_OWNERSHIP_CONFIG));
            String searchmap = searchBackend.searchMapRule(System.getProperty("PO_QUEUE"), PrefixType.STRING);
            //searchmap += searchBackend.searchMapRule(YcConstants.YC_MARKETPLACE_QUEUE, PrefixType.STRING);
//            System.err.println("SEARCHMAP " + searchmap);

            configBuilder.searchMapConfig().content(searchmap);
            configBuilder.producerClientConfig().host(producer.host());
            configBuilder.ednaConfig().host(edna.host());

            proxy = new PhoneOwnershipProxy(configBuilder.build());
            chain.get().add(producer);
            chain.get().add(proxy);
            this.chain = chain.release();
            producer.start();
            edna.start();
        }
    }

    public static final String PHONE_OWNERSHIP_CONFIG =
        Paths.getSourcePath(
            "mail/search/passport/phone_ownership_proxy/files/phone_ownership_proxy.conf");

    public static IniConfig loadConfig(final String path) throws Exception {
        System.setProperty("TVM_API_HOST", "");
        System.setProperty("TVM_CLIENT_ID", "");
        System.setProperty("TVM_ALLOWED_SRCS", "");
        System.setProperty("SECRET", "");
        System.setProperty("SERVER_NAME", "");
        System.setProperty("JKS_PASSWORD", "");
        System.setProperty("SEARCHMAP_PATH", "");
        System.setProperty("YC_SEARCH_BASE_DIR", ".");
        System.setProperty("PROXY_WORKERS", "2");
        System.setProperty("PRODUCER_HOST", "localhost");
        System.setProperty("PRODUCER_CLOUD_HOST", "localhost");
        System.setProperty("PRODUCER_INDEXING_HOST", "localhost");
        return patchProxyConfig(new IniConfig(new File(path)));
    }

    public static IniConfig patchProxyConfig(final IniConfig config) throws Exception {
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

    public PhoneOwnershipProxy proxy() {
        return proxy;
    }

    public EdnaHandler ednaHandler() {
        return ednaHandler;
    }

    public TestSearchBackend searchBackend() throws IOException {
        return searchBackend;
    }

    public void start() throws IOException {
        proxy.start();
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }
}
