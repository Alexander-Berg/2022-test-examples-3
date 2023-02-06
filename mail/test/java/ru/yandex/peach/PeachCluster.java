package ru.yandex.peach;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpHost;

import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;

public class PeachCluster implements GenericAutoCloseable<IOException> {
    public static final File CONFIG =
        new File(
            Paths.getSourcePath(
                "mail/search/disk/search_backend_disk_config/files"
                + "/search_backend.conf"));

    static {
        System.setProperty(
            "LUCENE_DISK_CONFIG_INCLUDE",
            "search_backend_thin.conf");
    }

    private final TestSearchBackend storage;
    private final StaticServer backend;
    private final GenericAutoCloseableHolder<IOException, Peach> peach;
    private final GenericAutoCloseable<IOException> closeable;
    private final boolean startBackend;

    public PeachCluster(final TestBase testBase) throws Exception {
        this(testBase, config());
    }

    public PeachCluster(
        final TestBase testBase,
        final PeachConfigBuilder config)
        throws Exception
    {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            storage = new TestSearchBackend(testBase, CONFIG);
            chain.get().add(storage);
            backend = new StaticServer(Configs.baseConfig("Backend"));
            chain.get().add(backend);
            peach = new GenericAutoCloseableHolder<>(peach(storage, config));
            chain.get().add(peach);
            closeable = chain.release();
        }
        startBackend = true;
    }

    public PeachCluster(final TestSearchBackend storage, final StaticServer backend)
        throws Exception
    {
        this(storage, backend, config());
    }

    public PeachCluster(
        final TestSearchBackend storage,
        final StaticServer backend,
        final PeachConfigBuilder config)
        throws Exception
    {
        this.storage = storage;
        this.backend = backend;
        peach = new GenericAutoCloseableHolder<>(peach(storage, config));
        closeable = peach;
        startBackend = false;
    }

    public static PeachQueueConfigBuilder queueConfig() throws Exception {
        return new PeachQueueConfigBuilder()
            .backendConfig(Configs.targetConfig())
            .concurrency(2)
            .deadlineParam("")
            .batchSize(1);
    }

    public static PeachConfigBuilder config() throws Exception {
        PeachConfigBuilder builder = new PeachConfigBuilder();
        builder.port(0);
        builder.connections(2);
        builder.searchQueryParams("&this-is-a-peach-request");
        builder.pkField("id");
        builder.seqField("version");
        builder.urlField("peach_url");
        builder.queueField("peach_queue");
        builder.queuesConfig().put(null, queueConfig());
        return builder;
    }

    private static Peach peach(
        final TestSearchBackend storage,
        final PeachConfigBuilder config)
        throws Exception
    {
        config.searchConfig(Configs.hostConfig(storage.searchPort()));
        config.indexerConfig(Configs.hostConfig(storage.indexerPort()));
        return new Peach(config.build());
    }

    public void start() throws IOException {
        // storage already started
        if (startBackend) {
            backend.start();
        }
        peach.get().start();
    }

    public TestSearchBackend storage() {
        return storage;
    }

    public StaticServer backend() {
        return backend;
    }

    public HttpHost peachHost() throws IOException {
        return new HttpHost("localhost", peach.get().port());
    }

    @Override
    public void close() throws IOException {
        closeable.close();
    }
}

