package org.apache.zookeeper.server.quorum;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;

import org.apache.zookeeper.ZookeeperServerSupplier;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZKDatabase;
import org.apache.zookeeper.server.ZooHttpServer;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;

import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.http.test.Configs;
import ru.yandex.http.util.server.ImmutableBaseServerConfig;
import ru.yandex.logger.PrefixedLogger;

public class QueueNode implements GenericAutoCloseable<IOException> {
    private final QuorumPeerConfig config = new QuorumPeerConfig();
    private final ZookeeperServerSupplier quorumPeer;
    private final ZooHttpServer httpServer;
    private final ServerCnxnFactory cnxnFactory;

    public QueueNode(final String testName) throws Exception {
        this(Files.createTempDirectory(testName + "_zoonode").toFile());
    }

    public QueueNode(final File nodeDir) throws Exception {
        config.dataLogDir = nodeDir;
        config.dataDir = nodeDir;

        PrefixedLogger logger = ZooKeeperServer.createLogger();

        FileTxnSnapLog txnSnapLog = new FileTxnSnapLog(
            config.getDataLogDir(),
            config.getDataDir(),
            config.getMaxQueueMemSize(),
            config.getMaxQueueStorageSize(),
            logger);

        ZKDatabase zkDb = new ZKDatabase(txnSnapLog, logger);
        ZooKeeperServer zkServer = new ZooKeeperServer(
            txnSnapLog,
            config.tickTime,
            config.minSessionTimeout,
            config.maxSessionTimeout,
            zkDb);
        quorumPeer = new BasicZookeeperServerProvider(zkServer);

        ImmutableBaseServerConfig zooHttpConfig = Configs.baseConfig("ZooHttpServer");
        System.err.println("ZooHttpServer config " + zooHttpConfig.name());
        httpServer = new ZooHttpServer(zooHttpConfig, zkDb, quorumPeer);

        cnxnFactory = ServerCnxnFactory.createFactory();
        cnxnFactory.configure(new InetSocketAddress(0),
            config.getMaxClientCnxns());
    }

    public int zkPort() {
        return quorumPeer.getActiveServer().getClientPort();
    }

    public ZooHttpServer httpServer() {
        return httpServer;
    }

    public void start() throws Exception {
        cnxnFactory.startup(quorumPeer.getActiveServer());
        httpServer.start();
    }

    @Override
    public void close() throws IOException {
        httpServer.close();
        cnxnFactory.closeAll();
    }

    private final class BasicZookeeperServerProvider
        implements ZookeeperServerSupplier
    {
        private final ZooKeeperServer server;

        public BasicZookeeperServerProvider(final ZooKeeperServer server) {
            this.server = server;
        }

        @Override
        public ZooKeeperServer getActiveServer() {
            return server;
        }
    }
}
