package ru.yandex.search.district;

import java.io.File;
import java.io.IOException;

import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.search.district.config.DistrictSearchProxyConfigBuilder;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;

public class DistrictSearchCluster
    implements GenericAutoCloseable<IOException>
{
    // CSOFF: MultipleStringLiterals
    private static final String SEARCH_BACKEND_CONFIG =
        Paths.getSourcePath(
            "mail/search/district/district_backend/files"
            + "/district_search_backend.conf");

    private static final String PROXY_CONFIG =
        Paths.getSourcePath(
            "mail/search/district/district_search_proxy/files"
            + "/district_search_proxy.conf");

    private final GenericAutoCloseableChain<IOException> chain;
    private final TestSearchBackend searchBackend;
    private final DistrictSearchProxy proxy;
    private final StaticServer producer;

    public DistrictSearchCluster(
        final TestBase testBase)
        throws Exception
    {
        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
                 new GenericAutoCloseableHolder<>(
                     new GenericAutoCloseableChain<>()))
        {
            System.setProperty("TVM_API_HOST", "");
            System.setProperty("INDEX_PATH", "");
            System.setProperty("TVM_CLIENT_ID", "");
            System.setProperty("TVM_ALLOWED_SRCS", "");
            System.setProperty("SECRET", "");
            System.setProperty("SERVER_NAME", "");
            System.setProperty("JKS_PASSWORD", "");
            searchBackend =
                new TestSearchBackend(new File(SEARCH_BACKEND_CONFIG));
            chain.get().add(searchBackend);

            producer = new StaticServer(Configs.baseConfig());
            chain.get().add(producer);

            System.setProperty(
                "PRODUCER_INDEXING_HOST",
                producer.host().toString());

            producer.add(
                "/_status?service=district_change_log&prefix=3&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");
            producer.add(
                "/_status?service=district_change_log&prefix=1&allow_cached"
                    + "&all&json-type=dollar",
                "[{\"localhost\":100500}]");
            producer.start();
            DistrictSearchProxyConfigBuilder builder =
                new DistrictSearchProxyConfigBuilder(
                    patchProxyConfig(
                        new IniConfig(
                            new File(PROXY_CONFIG))));
            builder.producerClientConfig().streaming(false);
            System.setProperty("DISTRICT_QUEUE", "district_change_log");
            System.setProperty("DISTRICT_CITY_QUEUE", "district_change_log");
            builder.searchMapConfig().content(
                searchBackend.searchMapRule(DistrictConstants.DISTRICT_QUEUE));

            //builder.producerClientConfig().host(producer.host());
            proxy = new DistrictSearchProxy(builder.build());
            chain.get().add(proxy);
            proxy.start();

            this.chain = chain.release();
        }
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }

    public TestSearchBackend searchBackend() {
        return searchBackend;
    }

    public DistrictSearchProxy proxy() {
        return proxy;
    }

    public StaticServer producer() {
        return producer;
    }

    private IniConfig patchProxyConfig(
        final IniConfig config)
        throws Exception
    {
        config.sections().remove("log");
        config.sections().remove("accesslog");
        config.sections().remove("tvm2");
        config.sections().remove("auth");
        config.section("server").sections().remove("https");
        IniConfig server = config.section("server");
        server.sections().remove("free-space-signals");
        server.put("port", "0");
        config.section("searchmap").put("file", null);
        return config;
    }
    // CSON: MultipleStringLiterals

    protected void addStatus(final String prefix) {
        producer.add(
            "/_status?service=district_change_log&prefix=" + prefix +
                "&allow_cached&all&json-type=dollar",
            "[{\"localhost\":100500}]");
    }
}
