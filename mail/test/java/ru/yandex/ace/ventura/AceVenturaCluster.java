package ru.yandex.ace.ventura;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpStatus;

import ru.yandex.ace.ventura.proxy.AceVenturaProxy;
import ru.yandex.ace.ventura.proxy.config.AceVenturaProxyConfigBuilder;
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
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;

public class AceVenturaCluster implements GenericAutoCloseable<IOException> {
    // CSOFF: MultipleStringLiterals
    private static final String LUCENE_CONFIG =
        Paths.getSourcePath(
            "mail/search/aceventura/aceventura_backend/files"
            + "/aceventura_search_backend.conf");
    private static final String PROXY_CONFIG =
        Paths.getSourcePath(
            "mail/search/aceventura/aceventura_proxy/files/aceventura_proxy.conf");
    private final AceVenturaProxy proxy;
    private final TestSearchBackend searchBackend;
    private final StaticServer producer;
    private final StaticServer mailSearch;

    private final GenericAutoCloseableChain<IOException> chain;


    public AceVenturaCluster(final TestBase base) throws Exception {
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
            System.setProperty("MAIL_SEARCH_TVM_ID", "0");
            System.setProperty("YT_ACCESS_LOG", "");
            System.setProperty("CPU_CORES", "2");
            System.setProperty("SEARCH_THREADS", "2");
            System.setProperty("MERGE_THREADS", "2");
            System.setProperty("LIMIT_SEARCH_REQUESTS", "20");
            System.setProperty("INDEX_THREADS", "2");

            searchBackend =
                new TestSearchBackend(base, new File(LUCENE_CONFIG).toPath());
            chain.get().add(searchBackend);

            producer = new StaticServer(Configs.baseConfig());
            chain.get().add(producer);
            producer.start();
            producer.add(
                "*",
                new StaticHttpResource(
                    new ProxyHandler(searchBackend().indexerPort())));

            mailSearch = new StaticServer(Configs.baseConfig());
            chain.get().add(mailSearch);
            mailSearch.start();

            System.setProperty("PRODUCER_HOST", producer.host().toString());

            AceVenturaProxyConfigBuilder builder =
                new AceVenturaProxyConfigBuilder(
                    patchProxyConfig(
                        new IniConfig(
                            new File(PROXY_CONFIG))));
            System.setProperty("ACEVENTURA_QUEUE", "aceventura_change_log");
            builder.searchMapConfig().content(
                searchBackend.searchMapRule(
                    AceVenturaConstants.ACEVENTURA_QUEUE));

            builder.mailSearch().host(mailSearch.host());
            proxy = new AceVenturaProxy(builder.build());

            this.chain = chain.release();
            proxy.start();
        }
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }

    private IniConfig patchProxyConfig(
        final IniConfig config)
        throws Exception
    {
        config.sections().remove("log");
        config.sections().remove("accesslog");
        config.sections().remove("tvm2");
        config.sections().remove("auth");
        IniConfig server = config.section("server");
        server.sections().remove("free-space-signals");
        server.put("port", "0");
        config.section("searchmap").put("file", null);
        return config;
    }

    public AceVenturaProxy proxy() {
        return proxy;
    }

    public TestSearchBackend searchBackend() {
        return searchBackend;
    }

    public StaticServer producer() {
        return producer;
    }

    public StaticServer mailSearch() {
        return mailSearch;
    }

    public void addStatus(final AceVenturaPrefix prefix) {
        producer.add(
            "/_status?service=aceventura_change_log&prefix="
                + (prefix.hash() % SearchMap.SHARDS_COUNT)
                + "&allow_cached&all&json-type=dollar",

            new StaticHttpResource(
                new StaticHttpItem(
                    HttpStatus.SC_OK,
                    "[{\"localhost\":100500}]")));
    }
    // CSON: MultipleStringLiterals
}
