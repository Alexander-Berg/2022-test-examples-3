package ru.yandex.search.district;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.http.HttpHost;

import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ProxyMultipartHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.search.district.indexer.DistrictIndexerProxy;
import ru.yandex.search.district.indexer.config.DistrictIndexerConfigBuilder;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;

public class DistrictIndexerCluster
    implements GenericAutoCloseable<IOException> {
    // CSOFF: MultipleStringLiterals
    // CSOFF: MagicNumber
    private static final String CONFIG =
        Paths.getSourcePath(
            "mail/search/district/district_indexer/files"
                + "/district-indexer.conf");
    private static final String SEARCH_BACKEND_CONFIG =
        Paths.getSourcePath(
            "mail/search/district/district_backend/files"
                + "/district_search_backend.conf");

    private final GenericAutoCloseableChain<IOException> chain;
    private final DistrictIndexerProxy indexer;
    private final StaticServer producer;
    private final TestSearchBackend searchBackend;

    public DistrictIndexerCluster(final TestBase testBase) throws Exception {
        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
                 new GenericAutoCloseableHolder<>(
                     new GenericAutoCloseableChain<>())) {
            producer = new StaticServer(Configs.baseConfig());
            chain.get().add(producer);
            producer.start();

            System.setProperty("INDEX_PATH", "");
            Path tmpPath =
                Files.createTempDirectory(testBase.testName.getMethodName());
            searchBackend =
                new TestSearchBackend(tmpPath, new File(SEARCH_BACKEND_CONFIG));

            chain.get().add(searchBackend);

            File searchmapFile =
                tmpPath
                    .resolve("./searchmap.txt").toAbsolutePath().toFile();

            try (FileWriter writer = new FileWriter(searchmapFile)) {
                writer.write(searchBackend.searchMapRule(
                    "distirct_change_log",
                    0,
                    10));
                writer.write(searchBackend.searchMapRule(
                    "distirct_city_change_log",
                    0,
                    10));
            }

            producer.add("*", new StaticHttpResource(new ProxyMultipartHandler(searchBackend.indexerPort())));

            System.setProperty("DISTRICT_QUEUE", "distirct_change_log");
            System.setProperty(
                "DISTRICT_CITY_QUEUE",
                "distirct_city_change_log");
            System.setProperty("TVM_API_HOST", "");
            System.setProperty("TVM_CLIENT_ID", "");
            System.setProperty("TVM_ALLOWED_SRCS", "");
            System.setProperty("SECRET", "");
            System.setProperty("SERVER_NAME", "");
            System.setProperty("JKS_PASSWORD", "");
            System.setProperty("PRODUCER_SERVER_NAME", "");
            System.setProperty("BSCONFIG_IDIR", "");
            System.setProperty(
                "BSCONFIG_IPORT",
                String.valueOf(producer.port()));

            DistrictIndexerConfigBuilder builder =
                new DistrictIndexerConfigBuilder(patchIndexerConfig(new
                    IniConfig(
                    new File(CONFIG))));
            builder.producerConfig().host(producer.host());
            builder.searchMapPath(searchmapFile.getAbsolutePath());

            indexer = new DistrictIndexerProxy(builder.build());
            chain.get().add(indexer);
            indexer.start();

            this.chain = chain.release();
        }
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }

    private IniConfig patchIndexerConfig(
        final IniConfig config)
        throws Exception {
        config.sections().remove("log");
        config.sections().remove("accesslog");
        config.sections().remove("tvm2");
        config.sections().remove("auth");
        config.section("server").sections().remove("https");
        IniConfig server = config.section("server");
        server.sections().remove("free-space-signals");
        server.put("port", "0");
        config.section("searchmap").put("path", null);
        return config;
    }

    public HttpHost host() throws IOException {
        return indexer.host();
    }

    public TestSearchBackend searchBackend() {
        return searchBackend;
    }

    public DistrictIndexerProxy indexer() {
        return indexer;
    }

    public StaticServer producer() {
        return producer;
    }
    // CSON: MultipleStringLiterals
    // CSON: MagicNumber
}
