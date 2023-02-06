package ru.yandex.logbroker2;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import ru.yandex.collection.IntPair;
import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.nio.client.AsyncClient;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerLockMessage;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.logbroker2.config.ImmutableLogbroker2ConsumerConfig;
import ru.yandex.logbroker2.config.ImmutableLogbroker2SingleConsumerConfig;
import ru.yandex.logbroker2.config.Logbroker2ConsumerConfigBuilder;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.util.TestBase;

public class MessageSenderCluster extends GenericAutoCloseableHolder<
    IOException,
    GenericAutoCloseableChain<IOException>>
{
    private static final String CONFIG_PATH =
        "mail/library/logbroker/logbroker2_consumer_service/files"
        + "/consumer.conf";
    private static final byte[] SOURCE_ID =
        "some id".getBytes(StandardCharsets.UTF_8);

    private final StaticServer targetServer;
    private final StaticServer anotherServer;
    private final SharedConnectingIOReactor reactor;
    private final AsyncClient client;
    private final MessageSender messageSender;
    private final LBTopicContext topicContext;

    public MessageSenderCluster(
        final TestBase testBase,
        final String serviceConfig,
        final String targetUri,
        final int sendBatchSize,
        final boolean gzipRequests)
        throws Exception
    {
        this(
            testBase,
            serviceConfig,
            targetUri,
            sendBatchSize,
            gzipRequests,
            CONFIG_PATH,
            "default");
    }

    public MessageSenderCluster(
        final TestBase testBase,
        final String serviceConfig,
        final String targetUri,
        final int sendBatchSize,
        final boolean gzipRequests,
        final String configPath,
        final String consumerName)
        throws Exception
    {
        this(
            testBase,
            serviceConfig,
            targetUri,
            sendBatchSize,
            gzipRequests,
            configPath,
            consumerName,
            null);
    }

    public MessageSenderCluster(
        final TestBase testBase,
        final String serviceConfig,
        final String targetUri,
        final int sendBatchSize,
        final boolean gzipRequests,
        final String configPath,
        final String consumerName,
        String targetHost)
        throws Exception
    {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            if (targetHost == null) {
                targetServer = new StaticServer(Configs.baseConfig("Target"));
                chain.get().add(targetServer);
                targetHost = targetServer.host().toString();

                anotherServer = new StaticServer(Configs.baseConfig("Another"));
                chain.get().add(anotherServer);
            } else {
                targetServer = null;
                anotherServer = null;
            }

            System.setProperty("SERVICE_CONFIG", serviceConfig);
            System.setProperty("TARGET_HOST", targetHost);
            System.setProperty("TARGET_URI", targetHost + targetUri);
            System.setProperty("CLIENT_ID", "1");
            System.setProperty("BALANCER_HOSTS", "lbkx.logbroker.yandex.net");
            System.setProperty("TOPICS", "some-topic");
            System.setProperty(
                "SEND_BATCH_SIZE",
                Integer.toString(sendBatchSize));
            System.setProperty("READ_ONLY_LOCAL", "false");
            System.setProperty(
                "GZIP_REQUESTS",
                Boolean.toString(gzipRequests));
            System.setProperty("SERVICE_PORT", "0");
            System.setProperty("SRC_CLIENT_ID", "2");
            System.setProperty("SECRET", "00:00");
            System.setProperty("LB_TVM_CLIENT_ID", "3");
            System.setProperty("INSTANCE_TAG_CTYPE", "prod");
            System.setProperty("DEBUG_FLAGS", "log-payload");
            System.setProperty("EXTRA_CONFIG", "debug-flags.conf");

            System.setProperty(
                "CONFIG_DIRS",
                Paths.getSourcePath(
                    "mail/tools/nanny_helpers/nanny_service_base/files"));

            IniConfig ini =
                new IniConfig(new File(Paths.getSourcePath(configPath)));
            TestBase.clearLoggerSection(ini.section("log"));
            TestBase.clearLoggerSection(ini.section("accesslog"));
            ini.section("server").sections().remove("free-space-signals");
            ini.section("target").sections().remove("https");
            IniConfig consumerSection = ini.sectionOrNull("consumer");
            if (consumerSection != null) {
                for (IniConfig consumer: consumerSection.sections().values()) {
                    consumer.section("target").sections().remove("https");
                }
            }

            ImmutableLogbroker2ConsumerConfig config =
                new Logbroker2ConsumerConfigBuilder(
                    new Logbroker2ConsumerConfigBuilder(ini).build())
                    .build();

            ini.checkUnusedKeys();

            reactor =
                new SharedConnectingIOReactor(config, config.dnsConfig());
            chain.get().add(reactor);

            ImmutableLogbroker2SingleConsumerConfig consumerConfig =
                config.consumers().get(consumerName);
            client = new AsyncClient(reactor, consumerConfig.targetConfig());
            chain.get().add(client);

            messageSender = new MessageSender(
                client,
                consumerConfig,
                (x, y) -> {},
                testBase.logger());

            topicContext =
                new LBTopicContext(
                    "key",
                    "host",
                    new ConsumerLockMessage("some-topic", 1, 2L, 3L, 4L),
                    x -> {},
                    sendBatchSize,
                    testBase.logger());


            reset(chain.release());
        }
    }

    public void start() throws Exception {
        if (targetServer != null) {
            targetServer.start();
        }

        if (anotherServer != null) {
            anotherServer.start();
        }

        reactor.start();
        client.start();
    }

    public StaticServer targetServer() {
        return targetServer;
    }

    public StaticServer anotherServer() {
        return anotherServer;
    }

    public MessageSender messageSender() {
        return messageSender;
    }

    public LBTopicContext topicContext() {
        return topicContext;
    }

    public IntPair<Void> sendMessages(final String... messages)
        throws Exception
    {
        for (int i = 0; i < messages.length; ++i) {
            topicContext.offer(
                new LBMessage(
                    new MessageData(
                        messages[i].getBytes(StandardCharsets.UTF_8),
                        i,
                        new MessageMeta(
                            SOURCE_ID,
                            i,
                            1234567890000L + i,
                            1334567890000L + i,
                            "127.0.0.1",
                            CompressionCodec.RAW,
                            Collections.emptyMap())),
                    () -> {}));
        }
        return messageSender.sendMessages(topicContext).get();
    }
}

