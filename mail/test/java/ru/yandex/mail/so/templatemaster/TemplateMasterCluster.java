package ru.yandex.mail.so.templatemaster;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.mail.so.templatemaster.config.TemplateMasterConfigBuilder;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;

public class TemplateMasterCluster extends GenericAutoCloseableHolder<
    IOException,
    GenericAutoCloseableChain<IOException>>
{
    private final TestSearchBackend searchBackend;
    private final TemplateMaster templateMaster;
    private final StaticServer producer;
    // Searchmap must be loaded before TM aquires port
    private final StaticServer tmMirror;

    public TemplateMasterCluster(TestBase env) throws Exception {
        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
                 new GenericAutoCloseableHolder<>(
                     new GenericAutoCloseableChain<>()))
        {

            System.setProperty("SHARDS_PER_HOST", "217");
            System.setProperty("LUCENE_DUMP_PORT", "0");
            System.setProperty("LUCENE_SEARCH_PORT", "0");
            System.setProperty("LUCENE_INDEXER_PORT", "0");
            System.setProperty("LUCENE_OLD_HTTP_PORT", "0");
            System.setProperty("NANNY_SERVICE_ID", "nanny-test");
            System.setProperty("INSTANCE_TAG_CTYPE", "unittest");
            System.setProperty("YASM_TAG", "does_not_apply");
            searchBackend = new TestSearchBackend(
                env,
                false,
                Path.of(
                    Paths.getSourcePath(
                        "mail/template_master/backend/files/lucene.conf")));
            chain.get().add(searchBackend);

            producer = new StaticServer(Configs.baseConfig("producer"));
            chain.get().add(producer);

            tmMirror = new StaticServer(Configs.baseConfig("tmMirror"));
            chain.get().add(tmMirror);

            Path searchmap = Files.createTempFile("searchmap-", ".txt");
            Files.writeString(
                searchmap,
                "template_master host:localhost,shards:0-217"
                    + ",search_port:" + tmMirror.port()
                    + ",json_indexer_port:" + tmMirror.port() + "\n");

            final TemplateMasterConfigBuilder
                builder =
                new TemplateMasterConfigBuilder(new IniConfig(new StringReader(
                    "stable-template-cache = 1M\n" +
                        "unstable-template-cache = 1M\n" +
                        "matching-threshold = 0.1\n" +
                        "hits-to-stabilize = 6\n" +
                        "indexing-tags-cutoff-threshold = 8192\n" +
                        "unstable-eviction.maximum-size = 15M\n" +
                        "unstable-eviction.guaranteed-time = 10m\n" +
                        "unstable-eviction.maximum-time = 20h\n" +
                        "[server]\n" +
                        "port = 0\n" +
                        "connections = 1000\n" +
                        "timeout = 5s\n" +
                        "timer.resolution = 51ms\n" +
                        "linger = 1\n" +
                        "workers.min = 2\n" +
                        "workers.percent = 0\n[search]\n" +
                        "connections = 200\n" +
                        "\n" +
                        "[indexer]\n" +
                        "connections = 200\n" +
                        "\n" +
                        "[lucene-index]\n" +
                        "host = " + searchBackend.indexerUri() + "\n" +
                        "connections = 200\n" +
                        "\n" +
                        "[lucene-search]\n" +
                        "host = " + searchBackend.searchUri() + "\n" +
                        "connections = 200\n" +
                        "\n" +
                        "[producer]\n" +
                        "host = localhost:" + producer.port() + "\n" +
                        "connections = 1000\n" +
                        "timeout = 1m\n" +
                        "allow-cached = true\n" +
                        "fallback-to-searchmap = true\n" +
                        "max-total-time = 200\n" +
                        "cache-ttl = 1m\n" +
                        "streaming = true\n" +
                        "cache-update-interval = 10s\n" +
                        "[producer-async-client]\n" +
                        "host = $(producer.host)\n" +
                        "connections = 1000\n" +
                        "[searchmap]\n" +
                        "file = " + searchmap.toAbsolutePath() + "\n"
                )));
            templateMaster = new TemplateMaster(builder.build());
            chain.get().add(templateMaster);

            tmMirror.add(
                "/*",
                new StaticHttpResource(
                    new ProxyHandler(templateMaster.port())));

            reset(chain.release());
        }
    }

    public void start() throws IOException {
        templateMaster.start();
        producer.start();
        tmMirror.start();

        producer.add("/_status*","[{\"localhost\":-1}]");
    }


    public TestSearchBackend searchBackend() {
        return searchBackend;
    }

    public TemplateMaster templateMaster() {
        return templateMaster;
    }

    public StaticServer producer() {
        return producer;
    }

    @Override
    public void close() throws IOException {
        // Fire&Forget to producer used; waiting before closing reactor
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
        } finally {
            super.close();
        }
    }

    public String TMUri() throws IOException {
        return "http://localhost:" + templateMaster.port();
    }
}
