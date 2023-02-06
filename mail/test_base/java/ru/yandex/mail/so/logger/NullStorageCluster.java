package ru.yandex.mail.so.logger;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

import ru.yandex.http.test.HttpResource;
import ru.yandex.mail.so.logger.config.BatchSaverConfig;
import ru.yandex.mail.so.logger.config.StoragesConfig;

public class NullStorageCluster<C extends BatchSaverConfig> implements StorageCluster<C> {
    private final Logger logger;

    NullStorageCluster(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void start() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public StoragesConfig<C> storagesConfig() {
        return null;
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public int port() throws IOException {
        return 0;
    }

    @Override
    public void addBlock(final String storageId, final String body) {
    }

    @Override
    public void deleteBlock(final String storageId) {
    }

    @Override
    public void add(final String uri, final HttpResource resource) {
    }

    @Override
    public C storageConfig() {
        return null;
    }

    @Override
    public Map<String, HttpResource> data() {
        return Collections.emptyMap();
    }
}
