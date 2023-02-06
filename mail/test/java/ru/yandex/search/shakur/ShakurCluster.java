package ru.yandex.search.shakur;

import java.io.File;
import java.io.IOException;

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
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;

public class ShakurCluster implements GenericAutoCloseable<IOException> {
    // CSOFF: MultipleStringLiterals
    private static final String SEARCH_BACKEND_CONFIG =
        Paths.getSourcePath(
            "mail/search/passport/shakur/shakur_backend/files"
                + "/shakur_backend.conf");
    private static final String PROXY_CONFIG =
        Paths.getSourcePath(
            "mail/search/passport/shakur/shakur_proxy_service/files/shakur.conf");
    private final Shakur proxy;
    private final TestSearchBackend searchBackend;
    private final StaticServer producer;

    private final GenericAutoCloseableChain<IOException> chain;


    public ShakurCluster(final TestBase base) throws Exception {
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
            System.setProperty("FIELDS_CONF_DIR", ".");
            System.setProperty("INDEX_DIR", ".");
            System.setProperty("FULL_LOG_LEVEL", "all");
            System.setProperty("HTTP_PORT", "80");
            System.setProperty("HOSTNAME", "localhost");
            System.setProperty("BSCONFIG_IDIR", ".");
            System.setProperty("PROXY_WORKERS", "5");
            System.setProperty("PASSPORT_MIN_PWD_FREQ", "10");

            searchBackend =
                new TestSearchBackend(base, new File(SEARCH_BACKEND_CONFIG).toPath());
            chain.get().add(searchBackend);

            producer = new StaticServer(Configs.baseConfig());
            chain.get().add(producer);
            producer.start();
            producer.add(
                "*",
                new StaticHttpResource(
                    new ProxyHandler(searchBackend().indexerPort())));

            System.setProperty("PRODUCER_HOST", producer.host().toString());

            ShakurConfigBuilder builder =
                new ShakurConfigBuilder(
                    patchProxyConfig(
                        new IniConfig(
                            new File(PROXY_CONFIG))));

            builder.searchMapConfig().content(
                searchBackend.searchMapRule(
                    builder.shakurService()));

            proxy = new Shakur(builder.build());

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

    public Shakur proxy() {
        return proxy;
    }

    public TestSearchBackend searchBackend() {
        return searchBackend;
    }

    public StaticServer producer() {
        return producer;
    }

//    public StaticServer mailSearch() {
//        return mailSearch;
//    }
//
    public void addStatus() {
        producer.add(
            "/_status?service=shakur&prefix=0"
                + "&allow_cached&all&json-type=dollar",

            new StaticHttpResource(
                new StaticHttpItem(
                    HttpStatus.SC_OK,
                    "[{\"localhost\":100500}]")));
    }
    // CSON: MultipleStringLiterals
}
