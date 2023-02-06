package ru.yandex.mail.so.logger;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.http.impl.client.CloseableHttpClient;

import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.mail.so.logger.config.AuxiliaryStorageConfig;
import ru.yandex.mail.so.logger.config.AuxiliaryStoragesConfigBuilder;
import ru.yandex.mail.so.logger.config.ImmutableAuxiliaryStoragesConfig;
import ru.yandex.parser.config.ConfigException;
import ru.yandex.parser.config.IniConfig;

public class AuxiliaryStorageCluster implements StorageCluster<AuxiliaryStorageConfig> {
    public static final String AUXILIARY_STORAGE = "logs-consumer";
    public static final String STORE_DELIVERY = "/store/delivery";

    private static final int CONNECTIONS = 100;

    private final CloseableHttpClient client = Configs.createDefaultClient();
    private final GenericAutoCloseableChain<IOException> chain;
    private final AuxiliaryStoragesConfigBuilder auxiliaryStoragesConfig;
    private final StaticServer logsConsumer;
    private final Map<String, HttpResource> data = new HashMap<>();
    private final Logger logger;

    public AuxiliaryStorageCluster(final long batchMinSize, final Logger logger) throws Exception {
        this.logger = logger;
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                     new GenericAutoCloseableHolder<>(new GenericAutoCloseableChain<>()))
        {
            logsConsumer = new StaticServer(Configs.baseConfig("LogsConsumer"));
            chain.get().add(logsConsumer);

            IniConfig auxiliaryStoragesConfigSection = new IniConfig(new StringReader(
                "[" + AUXILIARY_STORAGE + "]"
                + "\ntype = logs-consumer"
                + "\nhost = localhost"
                + "\nport = " + logsConsumer.httpPort()
                + "\npath = " + STORE_DELIVERY
                + "\ncompression = lzo"
                + "\nworkers = 128"
                + "\nbatch-min-size = " + batchMinSize
                + "\nbatch-save-period = 10s"
                + "\nbatch-save-retries = 1"
                + "\nbatch-save-retry-timeout = 2s"
                + "\nconnections = " + CONNECTIONS + '\n'));
            auxiliaryStoragesConfig = new AuxiliaryStoragesConfigBuilder(auxiliaryStoragesConfigSection);
            auxiliaryStoragesConfigSection.checkUnusedKeys();
            this.chain = chain.release();
        }
    }

    @Override
    public void start() throws IOException {
        logsConsumer.start();
        logger.info("AuxiliaryStorageCluster STARTED");
    }

    @Override
    @SuppressWarnings("try")
    public void close() throws IOException {
        logger.info("AuxiliaryStorageCluster SHUTDOWN");
        try (CloseableHttpClient ignored = this.client) {
            chain.close();
        }
    }

    @Override
    public ImmutableAuxiliaryStoragesConfig storagesConfig() throws ConfigException {
        return auxiliaryStoragesConfig.build();
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public int port() throws IOException {
        return logsConsumer.httpPort();
    }

    @Override
    public AuxiliaryStorageConfig storageConfig() {
        return auxiliaryStoragesConfig.storageConfigs().get(AUXILIARY_STORAGE);
    }

    @Override
    public void add(String uri, HttpResource resource) {
        data.put(uriPreprocessor().apply(uri), resource);
        logsConsumer.add(uri, resource);
    }

    @Override
    public void addBlock(String storageId, String body) {
    }

    @Override
    public void deleteBlock(String storageId) {
    }

    @Override
    public Map<String, HttpResource> data() {
        return data;
    }
}
