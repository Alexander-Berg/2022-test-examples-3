package ru.yandex.market.logshatter.reader.logbroker2;

import com.github.fakemongo.Fongo;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Service;
import org.mockito.AdditionalMatchers;
import org.mockito.Mockito;
import ru.yandex.clickhouse.ClickHouseConnection;
import ru.yandex.clickhouse.ClickHouseStatementImpl;
import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.auth.Credentials;
import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.StreamConsumer;
import ru.yandex.kikimr.persqueue.consumer.StreamListener;
import ru.yandex.kikimr.persqueue.consumer.stream.StreamConsumerConfig;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerInitResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerLockMessage;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReleaseMessage;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.health.HealthMetaDao;
import ru.yandex.market.logbroker.pull.LogBrokerClient;
import ru.yandex.market.logbroker.pull.LogBrokerOffset;
import ru.yandex.market.logshatter.LogShatterMonitoring;
import ru.yandex.market.logshatter.LogShatterService;
import ru.yandex.market.logshatter.config.ConfigValidationException;
import ru.yandex.market.logshatter.config.ConfigurationService;
import ru.yandex.market.logshatter.config.LogShatterConfig;
import ru.yandex.market.logshatter.config.LogSource;
import ru.yandex.market.logshatter.logging.BatchErrorLoggerFactory;
import ru.yandex.market.logshatter.meta.LogshatterMetaDao;
import ru.yandex.market.logshatter.meta.SourceMeta;
import ru.yandex.market.logshatter.parser.LogParserProvider;
import ru.yandex.market.logshatter.reader.ReadSemaphore;
import ru.yandex.market.logshatter.reader.logbroker.LogBrokerConfigurationService;
import ru.yandex.market.logshatter.reader.logbroker.OldApiLogBrokerClients;
import ru.yandex.market.logshatter.reader.logbroker.PartitionDao;
import ru.yandex.market.logshatter.reader.logbroker2.common.PartitionIdUtils;
import ru.yandex.market.logshatter.reader.logbroker2.common.TopicId;
import ru.yandex.market.logshatter.reader.logbroker2.dc.LbDataCenterReaderServiceFactory;
import ru.yandex.market.logshatter.reader.logbroker2.topic.LbApiStreamConsumerFactory;
import ru.yandex.market.logshatter.reader.logbroker2.topic.LbTopicReaderServiceFactory;
import ru.yandex.market.logshatter.useragent.FakeUserAgentDetector;
import ru.yandex.market.monitoring.MonitoringStatus;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.util.concurrent.Service.State.RUNNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.AdditionalMatchers.gt;
import static org.mockito.AdditionalMatchers.lt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 01.02.2019
 */
class LbReadingTester {
    private static final String SERVER1 = "SERVER1";

    private final LogBrokerClient logBrokerClient = mock(LogBrokerClient.class);

    private final TestSingleThreadExecutorServiceFactory executorServiceFactory =
        new TestSingleThreadExecutorServiceFactory();

    private final Map<String, LogbrokerClientFactory> dataCenterToLogBrokerClientFactoryMap;

    private final Fongo fongo = new Fongo("");
    private final PartitionDao partitionDao = new PartitionDao(fongo.getDatabase("partitionDao"));
    private final LogshatterMetaDao logshatterMetaDao = new LogshatterMetaDao(fongo.getDatabase("logshatterMetaDao"));
    private final HealthMetaDao healthMetaDao = new HealthMetaDao(fongo.getDatabase("healthMetaDao"));

    private final ClickHouseConnection clickHouseConnection = mock(ClickHouseConnection.class, Mockito.RETURNS_MOCKS);

    private final LogShatterMonitoring logShatterMonitoring = new LogShatterMonitoring();
    private final ReadSemaphore readSemaphore = mock(ReadSemaphore.class);
    private final LogShatterService logShatterService = new LogShatterService();

    private List<LogShatterConfig> configs;

    private final Map<String, Session> topicIdToSessionMap = new HashMap<>();

    private LogBrokerReaderService2 sut;


    LbReadingTester() {
        try {
            // Именно ClickHouseStatementImpl, потому что в продакшн-коде каст
            when(clickHouseConnection.createStatement()).thenReturn(mock(ClickHouseStatementImpl.class));

            this.dataCenterToLogBrokerClientFactoryMap = Stream.of("iva", "man", "myt", "sas", "vla")
                .collect(Collectors.toMap(
                    dataCenter -> dataCenter,
                    dataCenter -> {
                        LogbrokerClientFactory factory = mock(LogbrokerClientFactory.class);
                        try {
                            when(factory.streamConsumer(any()))
                                .thenAnswer(streamConsumerInvocation -> {
                                    StreamConsumerConfig streamConsumerConfig = streamConsumerInvocation.getArgument(0);
                                    Collection<String> topics = streamConsumerConfig.getSessionConfig().getTopics();

                                    assertThat(topics).hasSize(1);

                                    return session("rt3." + dataCenter + "--" + topics.iterator().next()).streamConsumer;
                                });
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return factory;
                    }
                ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setUserAgentDetector(new FakeUserAgentDetector());

        logShatterService.setConfigurationService(configurationService);
        logShatterService.setClickHouseConnection(clickHouseConnection);
        logShatterService.setHealthMetaDao(healthMetaDao);
        logShatterService.setLogshatterMetaDao(logshatterMetaDao);
        logShatterService.setReadSemaphore(readSemaphore);

        ReadSemaphore.QueuesCounter queuesCounter = mock(ReadSemaphore.QueuesCounter.class);
        when(queuesCounter.getQueueThatReachedLimit()).thenReturn(null);

        when(readSemaphore.getQueuesCounterForSource(any())).thenReturn(queuesCounter);
    }


    void startReaderService() {
        startReaderService(Collections.singletonList("man"));
    }

    void startReaderService(List<String> dataCenters) {
        LogBrokerConfigurationService logBrokerConfigurationService = new LogBrokerConfigurationService(configs, "");
        sut = new LogBrokerReaderService2(
            executorServiceFactory,
            new LbDataCenterReaderServiceFactory(
                new LbTopicReaderServiceFactory(
                    executorServiceFactory,
                    new LbApiStreamConsumerFactory(
                        dataCenterToLogBrokerClientFactoryMap,
                        () -> Credentials.oauth("token"),
                        "clientId",
                        1,
                        2,
                        3
                    ),
                    partitionDao,
                    readSemaphore,
                    logShatterService,
                    logBrokerConfigurationService,
                    logshatterMetaDao,
                    null,
                    new BatchErrorLoggerFactory(10, 100)
                ),
                executorServiceFactory,
                new OldApiLogBrokerClients(
                    dataCenters.stream()
                        .collect(Collectors.toMap(
                            dataCenter -> dataCenter,
                            dataCenter -> logBrokerClient
                        ))
                ),
                logBrokerConfigurationService,
                partitionDao,
                logShatterMonitoring,
                1,
                2
            ),
            dataCenters
        );

        sut.startAsync();

        // Первый runScheduledTasks выполняет LogBrokerReaderService2
        runScheduledTasks();

        // Второй runScheduledTasks выполняет LogBrokerReaderService2 и LbDataCenterReaderService'ы
        runScheduledTasks();
    }

    void runScheduledTasks() {
        executorServiceFactory.runScheduledTasks();
    }

    void readerServiceShouldBeInState(Service.State state) {
        assertEquals(state, sut.state());
    }

    public void clusterCriticalMonitoringShouldBe(MonitoringStatus monitoringStatus) {
        assertEquals(monitoringStatus, logShatterMonitoring.getClusterCritical().getResult().getStatus());
    }

    void runParsers() {
        try {
            logShatterService.new ParserWorker().parseOneSourceContext();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    void runWriters(int outputBatchSize) {
        try {
            logShatterService.setOutputBatchSize(outputBatchSize);
            logShatterService.new OutputWorker(0, 0).outputOnce();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    void givenConfigs(LogShatterConfig... configs) {
        this.configs = Arrays.asList(configs);
    }

    void givenTopicsInOldApi(String ident, String logType, String... topics) {
        try {
            when(logBrokerClient.getOffsets(ident, logType))
                .thenReturn(
                    Stream.of(topics)
                        .map(topic -> new LogBrokerOffset(
                            topic + ":0",
                            0,
                            0,
                            0,
                            0,
                            "",
                            TopicId.fromString(topic).getDataCenter()
                        ))
                        .collect(Collectors.toList())
                );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    Session givenStartedSession() {
        return givenStartedSession(config());
    }

    Session givenStartedSession(LogShatterConfig.Builder... configs) {
        givenConfigs(
            Stream.of(configs)
                .map(config -> {
                        try {
                            return config
                                .setSources(Collections.singletonList(LogSource.create("logbroker://market-health-stable--other")))
                                .build();
                        } catch (ConfigValidationException e) {
                            throw new RuntimeException(e);
                        }
                    }
                )
                .toArray(LogShatterConfig[]::new)
        );

        givenTopicsInOldApi("market-health-stable", "other", "rt3.man--market-health-stable--other");

        startReaderService();

        Session session = session("rt3.man--market-health-stable--other");
        session.lbSendsInitMessage();

        readerServiceShouldBeInState(RUNNING);

        clearInvocations(getAllMocks());

        return session;
    }

    Session.Partition givenStartedSessionWithLockedPartition() {
        return givenStartedSessionWithLockedPartition(givenStartedSession());
    }

    Session.Partition givenStartedSessionWithLockedPartition(LogShatterConfig.Builder... configs) {
        return givenStartedSessionWithLockedPartition(givenStartedSession(configs));
    }

    Session.Partition givenStartedSessionWithLockedPartition(Session session) {
        Session.Partition partition = session.partition(1);
        partition.lbSendsLockMessage();

        clearInvocations(getAllMocks());

        return partition;
    }


    void verifyInteractions(InteractionVerifier... interactionVerifiers) {
        Stream.of(interactionVerifiers).forEach(Runnable::run);
        try {
            verify(readSemaphore, atLeast(0)).waitForRead();
            verify(readSemaphore, atLeast(0)).notifyRead();
            verify(readSemaphore, atLeast(0)).getQueuesCounterForSource(any());
            verify(readSemaphore, atLeast(0)).incrementGlobalQueue(anyLong());
            verify(readSemaphore, atLeast(0)).decrementGlobalQueue(anyLong());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        verifyNoMoreInteractions(getAllMocks());
    }

    void verifyNoInteractions() {
        verifyInteractions();
    }

    void verifyQueueSizeIncreasedBy(long amountBytes) {
        verifyQueueSizeIncreasedBy(amountBytes, 0);
    }

    void verifyQueueSizeIncreasedBy(double amountBytes, long acceptableDifference) {
        verify(readSemaphore).incrementGlobalQueue(AdditionalMatchers.and(
            gt((long) amountBytes - acceptableDifference - 1),
            lt((long) amountBytes + acceptableDifference + 1)
        ));
    }

    private Object[] getAllMocks() {  // TODO
        List<Object> mocks = new ArrayList<>();
        mocks.addAll(dataCenterToLogBrokerClientFactoryMap.values());
        mocks.add(readSemaphore);
        mocks.add(clickHouseConnection);
        topicIdToSessionMap.values().stream()
            .flatMap(Session::getMocksStream)
            .forEach(mocks::add);
        return mocks.toArray();
    }


    Session session(String topicId) {
        return topicIdToSessionMap.computeIfAbsent(topicId, Session::new);
    }


    static LogShatterConfig.Builder config() {
        return LogShatterConfig.newBuilder().setConfigFileName("/1");
    }

    static LogShatterConfig config(String topic) {
        try {
            return config()
                .setSources(Collections.singletonList(LogSource.create("logbroker://" + topic)))
                .build();
        } catch (ConfigValidationException e) {
            throw new RuntimeException(e);
        }
    }

    static LogShatterConfig.Builder configThatMatchesEverything() {
        try {
            return LogShatterConfig.newBuilder()
                .setConfigFileName("/2")
                .setLogPath("**")
                .setLogHosts("**")
                .setDataClickHouseTable(new ClickHouseTableDefinitionImpl("db.first", Collections.emptyList(), null))
                .setParserProvider(new LogParserProvider(TestParser.class.getName(), null, null));
        } catch (ConfigValidationException e) {
            throw new RuntimeException(e);
        }
    }

    InteractionVerifier clickHouseReceivedData() {
        return () -> {
            try {
                verify(clickHouseConnection, atLeastOnce()).createStatement();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        };
    }


    static MessageData messageData(long seqNo) {
        return messageData(seqNo, "SOURCE_ID1");
    }

    static MessageData messageData(long seqNo, CompressionCodec compressionCodec, byte[] data) {
        return messageData(seqNo, "SOURCE_ID1", compressionCodec, data);
    }

    static MessageData messageData(long seqNo, String sourceId) {
        return messageData(seqNo, sourceId, CompressionCodec.RAW, "1".getBytes());
    }

    static MessageData messageData(long seqNo, String sourceId, CompressionCodec compressionCodec, byte[] data) {
        return new MessageData(
            data,
            0,
            new MessageMeta(
                sourceId.getBytes(),
                seqNo,
                2,
                3,
                "",
                compressionCodec,
                ImmutableMap.of(
                    "server", Collections.singletonList(SERVER1),
                    "file", Collections.singletonList("/var/log/app1/app1.log")
                )
            )
        );
    }


    interface InteractionVerifier extends Runnable {
    }


    class Session {
        private final TopicId topicId;
        private final Map<Integer, Partition> partitionNumberToPartition = new HashMap<>();
        private final StreamConsumer streamConsumer = mock(StreamConsumer.class);
        private StreamListener streamListener;

        Session(String topicId) {
            this.topicId = TopicId.fromString(topicId);

            doAnswer(startConsumeInvocation -> {
                streamListener = startConsumeInvocation.getArgument(0);
                return null;
            })
                .when(streamConsumer).startConsume(any());
        }

        Stream<Object> getMocksStream() {
            return Stream.concat(
                Stream.of(streamConsumer),
                partitionNumberToPartition.values().stream()
                    .flatMap(Partition::getMocksStream)
            );
        }

        Partition partition(int partition) {
            return partitionNumberToPartition.computeIfAbsent(
                partition,
                partitionNumber -> new Partition(topicId, partitionNumber)
            );
        }

        void lbSendsInitMessage() {
            streamListener.onInit(new ConsumerInitResponse("session_" + topicId.asString()));
        }

        InteractionVerifier lbReceivesStreamConsumerStart(String dataCenter) {
            return () -> {
                try {
                    verify(dataCenterToLogBrokerClientFactoryMap.get(dataCenter), atLeast(1))
                        .streamConsumer(any(StreamConsumerConfig.class));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                verify(streamConsumer).startConsume(any(StreamListener.class));
            };
        }

        InteractionVerifier lbReceivesStreamConsumerStop() {
            return () -> verify(streamConsumer).stopConsume();
        }


        class Partition {
            private final TopicId topicId;
            private final int partitionNumber;

            private final StreamListener.LockResponder lockResponder = mock(StreamListener.LockResponder.class);

            private final Map<Long, StreamListener.ReadResponder> cookieToReadResponderMap = new HashMap<>();

            Partition(TopicId topicId, int partitionNumber) {
                this.topicId = topicId;
                this.partitionNumber = partitionNumber;
            }

            Stream<Object> getMocksStream() {
                return Stream.of(lockResponder);
            }

            void givenOffsetInMongo(long offset) {
                partitionDao.save(new LogBrokerOffset(
                    PartitionIdUtils.toString(topicId.asString(), partitionNumber),
                    offset,
                    offset,
                    offset,
                    0,
                    "",
                    ""
                ));
            }

            void lbSendsLockMessage() {
                streamListener.onLock(
                    new ConsumerLockMessage(
                        topicId.asString(),
                        partitionNumber,
                        0,
                        0,
                        0
                    ),
                    lockResponder
                );
            }

            void lbSendsReleaseMessage() {
                streamListener.onRelease(
                    new ConsumerReleaseMessage(
                        topicId.asString(),
                        partitionNumber,
                        true,
                        0
                    )
                );
            }

            void lbClosesSession() {
                streamListener.onClose();
            }

            void lbSendsError(Throwable throwable) {
                streamListener.onError(throwable);
            }

            InteractionVerifier lbReceivedLockedMessage(long offset) {
                return () -> verify(lockResponder).locked(eq(offset), anyBoolean());
            }

            void lbSendsData(long cookie, MessageData... messageDatas) {
                assertNotEquals(0, messageDatas.length);

                assertFalse(cookieToReadResponderMap.containsKey(cookie));
                StreamListener.ReadResponder readResponderMock = mock(StreamListener.ReadResponder.class);
                cookieToReadResponderMap.put(cookie, readResponderMock);

                streamListener.onRead(
                    new ConsumerReadResponse(
                        Collections.singletonList(
                            new MessageBatch(
                                topicId.asString(),
                                partitionNumber,
                                Arrays.asList(messageDatas)
                            )
                        ),
                        cookie
                    ),
                    readResponderMock
                );
            }

            InteractionVerifier lbReceivedCommit(long cookie) {
                return () -> verify(cookieToReadResponderMap.get(cookie)).commit();
            }

            InteractionVerifier mongoReceivedSeqNo(String sourceId, long seqNo) {
                return () -> assertThat(logshatterMetaDao.getByOrigin(topicId.getIdent()).values())
                    .extracting(SourceMeta::getId, SourceMeta::getFileOffset)
                    .contains(tuple(
                        topicId.asString() + "-" + partitionNumber + "-" + SERVER1 + "-" + sourceId,
                        seqNo
                    ));
            }
        }
    }
}
