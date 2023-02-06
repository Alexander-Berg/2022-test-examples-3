package ru.yandex.search.disk.indexer;

import java.io.IOException;

import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;

public class DiskIndexerCluster implements GenericAutoCloseable<IOException> {
    private final StaticServer mpfs;
    private final StaticServer producer;
    private final DiskIndexer indexer;
    private final GenericAutoCloseableChain<IOException> chain;

    public DiskIndexerCluster() throws Exception {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                    new GenericAutoCloseableHolder<>(
                        new GenericAutoCloseableChain<>()))
        {
            mpfs = new StaticServer(Configs.baseConfig("MPFS"));
            chain.get().add(mpfs);
            producer = new StaticServer(Configs.baseConfig("Producer"));
            chain.get().add(producer);

            DiskIndexerConfigBuilder config = new DiskIndexerConfigBuilder();
            config.port(0);
            config.connections(2);
            config.mpfsConfig(Configs.uriConfig(mpfs, "/json/snapshot"));
            config.producerConfig(Configs.hostConfig(producer));
            config.searchProxy(null);
            indexer = new DiskIndexer(new ImmutableDiskIndexerConfig(config));
            chain.get().add(indexer);
            this.chain = chain.release();
        }
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }

    public StaticServer mpfs() {
        return mpfs;
    }

    public StaticServer producer() {
        return producer;
    }

    public DiskIndexer indexer() {
        return indexer;
    }

    public void start() throws IOException {
        mpfs.start();
        producer.start();
        indexer.start();
    }
}

