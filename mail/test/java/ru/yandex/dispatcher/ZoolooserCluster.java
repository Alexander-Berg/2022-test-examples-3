package ru.yandex.dispatcher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.zookeeper.ZookeeperServerSupplier;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZKDatabase;
import org.apache.zookeeper.server.ZooHttpServer;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;

import ru.yandex.dispatcher.consumer.ConsumerServer;
import ru.yandex.dispatcher.producer.Producer;
import ru.yandex.dispatcher.producer.ProducerConfigBuilder;
import ru.yandex.dispatcher.producer.ProducerConfigDefaults;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.logger.PrefixedLogger;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.util.TestBase;
import ru.yandex.util.filesystem.CloseableDeleter;

public class ZoolooserCluster
    extends GenericAutoCloseableHolder
        <IOException, GenericAutoCloseableChain<IOException>>
{
    private final Producer producer;
    private final List<QueueNode> queueNodes;
    private final ConsumerServer consumer;
    private final StaticServer backend;
    private final String searchmap;

    public ZoolooserCluster(final TestBase testBase) throws Exception {
        this(testBase, true, true);
    }

    public ZoolooserCluster(
        final TestBase testBase,
        final boolean startProducer,
        final boolean startConsumer)
        throws Exception
    {
        this(testBase, 1, true, true);
    }

    public ZoolooserCluster(
        final TestBase testBase,
        final int queues,
        final boolean startProducer,
        final boolean startConsumer)
        throws Exception
    {
        this(new ZooClusterConfig(testBase).queues(queues).startConsumer(startConsumer).startProducer(startProducer));
    }

    public ZoolooserCluster(final ZooClusterConfig config) throws Exception {
        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
                 new GenericAutoCloseableHolder<>(
                     new GenericAutoCloseableChain<>()))
        {
            CloseableDeleter tmpDirHolder =
                new CloseableDeleter(
                    Files.createTempDirectory(
                        config.testBase().testName.getMethodName()));
            chain.get().add(tmpDirHolder);
            File tmpDir = tmpDirHolder.path().toFile();
            System.setProperty("zookeeper.snapCount", "1000");

            PrefixedLogger logger = config.testBase().logger;
            logger.info("Temporary dir " + tmpDir.getAbsolutePath());

            StringBuilder searchmapContent = new StringBuilder();

            if (config.startConsumer() && config.searchBackend() == null) {
                backend = new StaticServer(Configs.baseConfig("SearchBackend"));
                chain.get().add(backend);
                backend.start();
            } else {
                backend = null;
            }

            QuorumPeerConfig quorumPeerConfig = new QuorumPeerConfig();
            FileTxnSnapLog txnSnapLog = new FileTxnSnapLog(
                tmpDir,
                tmpDir,
                quorumPeerConfig.getMaxQueueMemSize(),
                quorumPeerConfig.getMaxQueueStorageSize(),
                logger);
            ZKDatabase zkDb = new ZKDatabase(txnSnapLog, logger);

            queueNodes = new ArrayList<>();
            for (int i = 0; i < config.queues(); i++) {
                QueueNode queueNode = new QueueNode(txnSnapLog, zkDb);

                chain.get().add(queueNode);
                queueNode.start();

                String zkStr =
                    queueNode.httpServer().host().getHostName()
                        + ':' + queueNode.zkPort()
                        + '/' + queueNode.httpServer().port();
                String queueName = "change_log_" + i;
                searchmapContent.append(queueName);
                searchmapContent.append(
                    " iNum:718,tag:localhost_26763");
                searchmapContent.append(
                    ",host:localhost,shards:0-10,zk:");
                searchmapContent.append(zkStr);
                if (backend != null) {
                    searchmapContent.append(",json_indexer_port:");
                    searchmapContent.append(backend.port());
                    backend.add(
                        "/getQueueId?service=" + queueName + "&shard=*",
                        "-1");
                } else if (config.searchBackend() != null) {
                    searchmapContent.append(",json_indexer_port:");
                    searchmapContent.append(config.searchBackend().jsonServerPort());
                    searchmapContent.append(
                        ",search_port_ng:");
                    searchmapContent.append(config.searchBackend().searchServerPort());
                } else {
                    searchmapContent.append(",json_indexer_port:26767");
                    searchmapContent.append(
                        ",search_port_ng:26764,search_port:26763\n");
                }

                searchmapContent.append(
                    ",search_port:26763\n");
                queueNodes.add(queueNode);
            }

            File searchmapFile =
                tmpDir.toPath()
                    .resolve("./searchmap_mail.txt").toAbsolutePath().toFile();

            try (FileWriter writer = new FileWriter(searchmapFile)) {
                writer.write(searchmapContent.toString());
            }

            this.searchmap = searchmapContent.toString();
            logger.info(searchmapContent.toString());
            if (config.startProducer()) {
                ProducerConfigBuilder producerConfig =
                    new ProducerConfigBuilder(ProducerConfigDefaults.INSTANCE)
                        .zooHttpTargetConfig(Configs.targetConfig())
                        .searchMapPath(searchmapFile.getAbsolutePath())
                        .port(0)
                        .connections(100);
                producer = new Producer(producerConfig.build());
                chain.get().add(producer);
                producer.start();
            } else {
                producer = null;
            }

            if (config.startConsumer()) {
                String consumerConfig = "hostname_resolver = system_cmd\n"
                    + "consumer_type = async\n"
                    + "max_per_route = 100\n"
                    + "[searchmap]\nfile = " + searchmapFile.getAbsolutePath()
                    + "\n"
                    + "[http_server]\nport=0\nconnections=10\nworkers.min=2\nworkers.percent=0\n"
                    + "[system_cmd_resolver]\ncmd = echo localhost\n"
                    + "[zoolooser]\n"
                    + "workers = 1\n"
                    + "timeout = 1000\n"
                    + "advance_on_missed = 0\n"
                    + "ignore_position = true\n"
                    + "responseless = true\n"
                    + "status-grouping-time = 20000\n"
                    + "prefetch-count = 2\n"
                    + "next-id-finder-min-host-count-pct = 75\n"
                    + "producers = ";

                if (producer != null) {
                    consumerConfig += producer.host().toHostString();
                }

                consumerConfig += "\nconsumer-tags = localhost_26763\n"
                    + "[async-consumer]\n"
                    + "workers = 3\n"
                    + "connections = 100\n"
                    + "timeout = 5s\n"
                    + "watchdog-delay = 3h\n";

                consumer = new ConsumerServer(
                    new IniConfig(
                        new StringReader(consumerConfig)));
                chain.get().add(consumer);
                consumer.start();
            } else {
                consumer = null;
            }

            reset(chain.release());
        }
    }

    public Producer producer() {
        return producer;
    }

    public QueueNode queueNode() {
        return queueNodes.get(0);
    }

    public List<QueueNode> queueNodes() {
        return queueNodes;
    }

    public ConsumerServer consumer() {
        return consumer;
    }

    public StaticServer backend() {
        return backend;
    }

    public String searchmap() {
        return searchmap;
    }

    public class QueueNode implements GenericAutoCloseable<IOException> {
        private final QuorumPeerConfig config = new QuorumPeerConfig();
        private final ZookeeperServerSupplier quorumPeer;
        private final ZooHttpServer httpServer;
        private final ServerCnxnFactory cnxnFactory;

        public QueueNode(
            final FileTxnSnapLog txnSnapLog,
            final ZKDatabase zkDb)
            throws Exception
        {
            ZooKeeperServer zkServer = new ZooKeeperServer(
                txnSnapLog,
                config.getTickTime(),
                config.getMinSessionTimeout(),
                config.getMaxSessionTimeout(),
                zkDb);
            quorumPeer = new BasicZookeeperServerProvider(zkServer);

            httpServer =
                new ZooHttpServer(Configs.baseConfig(), zkDb, quorumPeer);

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
