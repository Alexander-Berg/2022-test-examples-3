package ru.yandex.search.yc;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.ProxyMultipartHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.json.dom.BasicContainerFactory;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.writer.JsonType;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.search.yc.config.YcSearchIndexerConfigBuilder;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;

public class YcIndexerCluster implements GenericAutoCloseable<IOException> {
    public static final String DEFAULT_LB_PARAMS
        = "&topic=topic&partition=1&offset=0&seqNo=1&message-create-time=0&message-write-time=0";
    // CSOFF: MultipleStringLiterals
    public static final String SERVICE = "yc_change_log";
    public static final String MARKETPLACE_SERVICE = "yc_change_log_marketplace_compute_preprod";
    private static final String SEARCH_BACKEND_CONFIG =
        Paths.getSourcePath(
            "mail/search/yc/yc_backend/files/yc_search_backend.conf");
    private static final String INDEXER_CONFIG =
        Paths.getSourcePath(
            "mail/search/yc/yc_indexer/debian/files/yc_search_indexer.cloud.conf");
    private static final String WORK_DIR = Paths.getSourcePath("cloud/search");

    private final GenericAutoCloseableChain<IOException> chain;
    private final TestSearchBackend searchBackend;
    private final StaticServer producer;
    private final YcIndexer indexer;

    public YcIndexerCluster(
            final TestBase testBase)
            throws Exception
    {
        this(testBase, null);
    }

    public YcIndexerCluster(
        final TestBase testBase,
        final TestSearchBackend searchBackendTmp)
        throws Exception
    {
        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
                 new GenericAutoCloseableHolder<>(
                     new GenericAutoCloseableChain<>()))
        {
            System.setProperty("YC_QUEUE", SERVICE);
            System.setProperty("YC_MARKETPLACE_QUEUE", MARKETPLACE_SERVICE);
            System.setProperty("PORTO_HOST", "localhost");
            System.setProperty("BSCONFIG_IPORT", "0");
            System.setProperty("YC_INDEXER_PORT", "0");
            System.setProperty("YC_LOGBROKER_PORT", "0");
            System.setProperty("INDEX_PATH", ".");
            System.setProperty("TVM_API_HOST", "tvm-api.yandex-team.ru");
            System.setProperty("TVM_CLIENT_ID", "186");
            System.setProperty("SECRET", "186");
            System.setProperty("LIMIT_SEARCH_REQUESTS", "10");
            System.setProperty("INDEX_THREADS", "5");
            System.setProperty("SEARCH_THREADS", "5");
            System.setProperty("MERGE_THREADS", "2");
            System.setProperty("INDEX_ACCESS_LOG_FILE", "/logs/index-access.log");
            System.setProperty("INDEX_LOG_FILE", "/logs/index.log");
            System.setProperty("ERROR_LOG_FILE", "/logs/error.log");
            System.setProperty("FULL_LOG_FILE", "/logs/full.log");
            System.setProperty("ACCESS_LOG_FILE", "/logs/access.log");
            System.setProperty("SERVER_PORT", "0");
            System.setProperty("LOGS_DIR", "/logs");
            System.setProperty("SEARCHMAP_PATH", ".");
            System.setProperty("YC_INDEXER_WORK_DIR", WORK_DIR);

            if (searchBackendTmp == null) {
                searchBackend =
                        new TestSearchBackend(new File(SEARCH_BACKEND_CONFIG));
                chain.get().add(searchBackend);
            } else {
                searchBackend = searchBackendTmp;
            }

            System.setProperty("INDEX_BACKEND_HOST", searchBackend.indexerHost().toString());
            System.setProperty("MARKET_INDEX_BACKEND_HOST", searchBackend.indexerHost().toString());

            producer = new StaticServer(Configs.baseConfig());
            chain.get().add(producer);

            System.setProperty(
                "PRODUCER_INDEXING_HOST",
                producer.host().toString());

            StaticHttpResource searchBackendIndexProxy =
                new StaticHttpResource(
                    new ProxyHandler(searchBackend.indexerPort()));

            producer.add("/update?*", searchBackendIndexProxy);
            producer.add("/add?*", searchBackendIndexProxy);
            producer.add("/delete?*", searchBackendIndexProxy);
            producer.add("/modify?*", searchBackendIndexProxy);
            producer.add("/ping*", searchBackendIndexProxy);
            producer.add("/notify?*", new ProxyMultipartHandler(searchBackend.indexerPort()));
            producer.add(
                "/index_search_doc?*",
                new StaticHttpResource(
                    new ProxyMultipartHandler(
                        searchBackend.indexerPort())));

            producer.start();

            System.setProperty("YC_PRODUCER_PORT", String.valueOf(producer.port()));

            YcSearchIndexerConfigBuilder builder =
                new YcSearchIndexerConfigBuilder(
                    patchConfig(
                        new IniConfig(
                            new File(INDEXER_CONFIG))));
            builder.searchMapConfig().content(searchBackend.searchMapRule(SERVICE));
            builder.producerClientConfig().host(producer.host());
            indexer = new YcIndexer(builder.build());
            chain.get().add(indexer);
            this.chain = chain.release();
        }
    }


    public void start() throws IOException {
        indexer.start();
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }

    public TestSearchBackend searchBackend() {
        return searchBackend;
    }

    public YcIndexer indexer() {
        return indexer;
    }

    public StaticServer producer() {
        return producer;
    }

    private IniConfig patchConfig(
        final IniConfig config)
        throws Exception
    {
        config.sections().remove("log");
        config.sections().remove("accesslog");
        config.sections().remove("tvm2");
        config.sections().remove("auth");
        config.sections().remove("searchmap");
        config.section("server").sections().remove("https");
        IniConfig server = config.section("server");
        server.sections().remove("free-space-signals");
        server.put("port", "0");
        return config;
    }

    public static JsonMap doc(
        final String resourceId,
        final String cloudId,
        final String folderId,
        final String attributes,
        final String permissions)
        throws Exception
    {
        return doc(resourceId, cloudId, folderId, attributes, permissions, -1, false);
    }

    public static JsonMap doc(
        final String resourceId,
        final String cloudId,
        final String folderId,
        final String attributes,
        final String permissions,
        final long updateTs,
        final boolean deleted)
        throws Exception
    {
        String zdt;
        if (updateTs < 0) {
            zdt = "2019-08-15T15:12:04.999834+00:00";
        } else {
            zdt = ZonedDateTime
                .ofInstant(Instant.ofEpochMilli(updateTs), ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }

        String value = "{\n" +
            "  \"service\": \"managed-mongodb\",\n" +
            "  \"cloud_id\": \"" + cloudId + "\",\n" +
            "  \"folder_id\": \"" + folderId + "\",\n" +
            "  \"timestamp\": \"" + zdt + "\",\n" +
            "  \"attributes\": {\n" + attributes + "  },\n" +
            "  \"permission\": \"" + permissions + "\",\n" +
            "  \"resource_id\": \"" + resourceId + "\",\n" +
            "  \"resource_type\": \"cluster\",\n";

        if (deleted) {
            value += "  \"deleted\": \"" + zdt + "\"\n";
        } else {
            value += "  \"deleted\": \"\"\n";
        }

        value += "}";

        return TypesafeValueContentHandler.parse(value).asMap();
    }

    public JsonMap addDoc(
        final JsonMap doc)
        throws Exception
    {
        addDoc(JsonType.HUMAN_READABLE.toString(doc));
        JsonMap indexDoc = new JsonMap(BasicContainerFactory.INSTANCE);
        indexDoc.putAll(doc);
        indexDoc.remove("deleted");
        return indexDoc;
    }

    public void addDoc(
        final String doc)
        throws Exception
    {
        HttpPost add =
            new HttpPost(indexer().host() + "/api/yc/index?transfer_ts=1570450676326&add" + DEFAULT_LB_PARAMS);
        add.setEntity(new StringEntity(doc, StandardCharsets.UTF_8));
        HttpAssert.assertStatusCode(HttpStatus.SC_OK, add);
    }
    // CSON: MultipleStringLiterals
}

