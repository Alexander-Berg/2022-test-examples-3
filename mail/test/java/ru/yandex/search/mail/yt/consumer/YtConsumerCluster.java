package ru.yandex.search.mail.yt.consumer;

import java.io.File;
import java.io.IOException;

import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.search.mail.yt.consumer.alice.AliceConsumerBuildContext;
import ru.yandex.search.mail.yt.consumer.config.ImmutableYtConsumerConfig;
import ru.yandex.search.mail.yt.consumer.config.SourceConsumerBuildContext;
import ru.yandex.search.mail.yt.consumer.config.YtConsumerConfigBuilder;
import ru.yandex.search.mail.yt.consumer.cypress.NodeType;

public class YtConsumerCluster implements GenericAutoCloseable<IOException> {
    private static final String ANY_PORT = "0";
    private static final int JOB_CHECK_INTERVAL = 200;

    private static final String PORT = "port";
    private final YtConsumer ytProducer;
    private final YtCypressCluster yt;
    private final StaticServer producer;

    private final ImmutableYtConsumerConfig producerConfig;
    private final GenericAutoCloseableChain<IOException> chain;

    static {
        System.setProperty("BSCONFIG_IPORT", ANY_PORT);
        System.setProperty("BSCONFIG_INAME", "ytproducer");
        System.setProperty("BSCONFIG_IDIR", ".");
    }

    public YtConsumerCluster() throws Exception {
        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
                 new GenericAutoCloseableHolder<>(
                     new GenericAutoCloseableChain<>()))
        {
            yt = new YtCypressCluster(Configs.baseConfig("Yt"));
            chain.get().add(yt);
            yt.start();

            producer = new StaticServer(Configs.baseConfig("Producer"));
            chain.get().add(producer);
            producer.start();

            IniConfig ytProducerIniConfig =
                new IniConfig(
                    new File(
                        Paths.getSourcePath(
                            "mail/library/yt_consumer/main/bundle"
                            + "/yt-consumer.conf")));

            YtConsumerConfigBuilder config =
                new YtConsumerConfigBuilder(patchConfig(ytProducerIniConfig));

            config.ytConfig().cluster(yt.host());

            for (SourceConsumerBuildContext sourceConfig : config.consumers()) {
                if (sourceConfig instanceof AliceConsumerBuildContext) {
                    String monitorPath =
                        ((AliceConsumerBuildContext) sourceConfig)
                            .aliceConfig().monitorPath();

                    System.out.println("Creating " + monitorPath);
                    yt.create(monitorPath, NodeType.MAP_NODE);
                }
            }

            config.workersConfig().checkInterval(JOB_CHECK_INTERVAL);

            this.producerConfig = config.build();

            ytProducer = new YtConsumer(config.build());
            chain.get().add(ytProducer);

            this.chain = chain.release();
        }
    }

    public void start() throws IOException {
        ytProducer.start();
    }

    @Override
    public void close() throws IOException {
        this.chain.close();
    }

    protected IniConfig patchConfig(final IniConfig config)
        throws IOException
    {
        config.sections().remove("accesslog");
        config.sections().remove("log");
        config.sections().remove("stdout");
        config.sections().remove("stderr");
        config.sectionOrNull("server").put(PORT, ANY_PORT);
        config.put(
            "alice-consumer.producer.host",
            producer.host().toHostString());
        config.put(
            "alice-consumer.batch-size",
            "2");
        config.put(
            "alice-consumer.names-file",
            Paths.getSourcePath(
                "mail/library/yt_consumer/main/bundle/human_names.txt"));
        final String localScheduler = "localhost:0";
        config.put("schedulers", localScheduler);
        config.put("alice-consumer.schedulers", localScheduler);
        config.put("alice-consumer.keep-completed-count", "1");
        config.put("mobile-actions-consumer.schedulers", localScheduler);
        config.put(
            "mobile-actions-consumer.producer.host",
            producer.host().toHostString());
        config.put(
            "mobile-actions-consumer.batch-size",
            "3");

        return config;
    }

    public YtConsumer ytProducer() {
        return ytProducer;
    }

    public YtCypressCluster yt() {
        return yt;
    }

    public StaticServer producer() {
        return producer;
    }

    public ImmutableYtConsumerConfig config() {
        return producerConfig;
    }
}
