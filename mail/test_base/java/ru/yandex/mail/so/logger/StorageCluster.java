package ru.yandex.mail.so.logger;

import ru.yandex.mail.so.logger.config.BatchSaverConfig;
import ru.yandex.mail.so.logger.config.StoragesConfig;
import ru.yandex.parser.config.ConfigException;

public interface StorageCluster<C extends BatchSaverConfig> extends Cluster, DataHandler {

    StoragesConfig<C> storagesConfig() throws ConfigException;

    C storageConfig();

    void addBlock(final String storageId, final String body);

    void deleteBlock(final String storageId);
}
