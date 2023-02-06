package ru.yandex.search.mail.indexer.pg;

import java.io.IOException;

import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;

public class PgIndexerCluster implements GenericAutoCloseable<IOException> {
    private final StaticServer msal;
    private final StaticServer producer;
    private final PgIndexer indexer;
    private final GenericAutoCloseableChain<IOException> chain;

    public PgIndexerCluster(final int producerBatchSize) throws Exception {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            msal = new StaticServer(Configs.baseConfig("MSAL"));
            chain.get().add(msal);
            producer = new StaticServer(Configs.baseConfig("Producer"));
            chain.get().add(producer);

            PgIndexerConfigBuilder config = new PgIndexerConfigBuilder();
            config.port(0);
            config.connections(2);
            config.msalConfig(Configs.hostConfig(msal));
            config.producerConfig(Configs.hostConfig(producer));
            config.serviceName("change_log");
            config.midsPerRequest(2 + 2);
            config.producerBatchSize(producerBatchSize);
            indexer = new PgIndexer(config.build());
            chain.get().add(indexer);
            this.chain = chain.release();
        }
    }

    public StaticServer msal() {
        return msal;
    }

    public StaticServer producer() {
        return producer;
    }

    public PgIndexer indexer() {
        return indexer;
    }

    public void start() throws IOException {
        msal.start();
        producer.start();
        indexer.start();
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }
}

