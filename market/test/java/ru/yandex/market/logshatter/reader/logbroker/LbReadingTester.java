package ru.yandex.market.logshatter.reader.logbroker;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Service;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
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
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinition;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.health.HealthMetaDao;
import ru.yandex.market.health.KeyValueMetricSupplier;
import ru.yandex.market.health.configs.clickhouse.exception.ClusterSaveException;
import ru.yandex.market.health.configs.logshatter.config.ConfigValidationException;
import ru.yandex.market.health.configs.logshatter.config.LogShatterConfig;
import ru.yandex.market.health.configs.logshatter.config.LogSource;
import ru.yandex.market.health.configs.logshatter.config.TableDescriptionUtils;
import ru.yandex.market.health.configs.logshatter.useragent.FakeUserAgentDetector;
import ru.yandex.market.logbroker.pull.LogBrokerClient;
import ru.yandex.market.logbroker.pull.LogBrokerOffset;
import ru.yandex.market.logshatter.ClickHouseClusterSaveService;
import ru.yandex.market.logshatter.LogShatterExternalOutputWorker;
import ru.yandex.market.logshatter.LogShatterExternalParserWorker;
import ru.yandex.market.logshatter.LogShatterMonitoring;
import ru.yandex.market.logshatter.LogShatterOutputWorker;
import ru.yandex.market.logshatter.LogShatterParserWorker;
import ru.yandex.market.logshatter.LogShatterService;
import ru.yandex.market.logshatter.config.ConfigurationService;
import ru.yandex.market.logshatter.logging.BatchErrorLoggerFactory;
import ru.yandex.market.logshatter.logging.ErrorBoosterLogger;
import ru.yandex.market.logshatter.logging.LogSamplingPropertiesService;
import ru.yandex.market.logshatter.logging.StuffFileInfoLogger;
import ru.yandex.market.logshatter.meta.LogshatterMetaDao;
import ru.yandex.market.logshatter.meta.SourceMeta;
import ru.yandex.market.logshatter.parser.LogParser;
import ru.yandex.market.logshatter.parser.LogParserProvider;
import ru.yandex.market.logshatter.parser.ParserContext;
import ru.yandex.market.logshatter.parser.TableDescription;
import ru.yandex.market.logshatter.reader.ReadSemaphore;
import ru.yandex.market.logshatter.reader.logbroker.common.TopicId;
import ru.yandex.market.logshatter.reader.logbroker.dc.LbDataCenterReaderServiceFactory;
import ru.yandex.market.logshatter.reader.logbroker.dc.LbLogshatterDataCenters;
import ru.yandex.market.logshatter.reader.logbroker.topic.LbApiStreamConsumerFactory;
import ru.yandex.market.logshatter.reader.logbroker.topic.LbTopicReaderServiceFactory;
import ru.yandex.market.logshatter.sharding.LogShatterShardingService;
import ru.yandex.market.monitoring.MonitoringStatus;

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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * <br>
 * Date: 01.02.2019
 */
class LbReadingTester {
    private static final String SERVER1 = "SERVER1";
    private static final String DB_NAME = "test";
    private static final String DEFAULT_TABLE_NAME = "test";

    private final LogBrokerClient logBrokerClient = mock(LogBrokerClient.class);

    private final TestSingleThreadExecutorServiceFactory executorServiceFactory =
        new TestSingleThreadExecutorServiceFactory();

    private final Map<String, LogbrokerClientFactory> dataCenterToLogBrokerClientFactoryMap;

    private final MongoClient mongoClient =
        new MongoClient(new ServerAddress(new MongoServer(new MemoryBackend()).bind()));
    private final LogshatterMetaDao logshatterMetaDao = new LogshatterMetaDao(mongoClient.getDatabase(
        "logshatterMetaDao"));
    private final HealthMetaDao healthMetaDao = new HealthMetaDao(mongoClient.getDatabase("healthMetaDao"));

    private final ClickHouseConnection clickHouseConnection = mock(ClickHouseConnection.class, Mockito.RETURNS_MOCKS);

    private final LogShatterMonitoring logShatterMonitoring = new LogShatterMonitoring();
    private final ReadSemaphore readSemaphore = mock(ReadSemaphore.class);
    private final ConfigurationService configurationService = new ConfigurationService();
    private final LogShatterService logShatterService = spy(new LogShatterService());

    private final Map<String, Session> topicIdToSessionMap = new HashMap<>();

    private LogBrokerReaderService sut;

    private ReadSemaphore.QueuesCounter queuesCounter = mock(ReadSemaphore.QueuesCounter.class);
    private ClickHouseClusterSaveService clickHouseClusterSaveService = mock(ClickHouseClusterSaveService.class);
    private StreamListener.ReadResponder readResponderMock;

    private ExecutorService parseExecutorService;
    private ExecutorService outputExecutorService;


    LbReadingTester() {
        try {
            // Именно ClickHouseStatementImpl, потому что в продакшн-коде каст
            when(clickHouseConnection.createStatement()).thenReturn(mock(ClickHouseStatementImpl.class));

            this.dataCenterToLogBrokerClientFactoryMap = Stream.of("iva", "man", "myt", "sas", "vla", "kafka-bs")
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

                                    return session("rt3." + dataCenter + "--" +
                                        topics.iterator().next()).streamConsumer;
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

        configurationService.setUserAgentDetector(new FakeUserAgentDetector());

        logShatterService.setConfigurationService(configurationService);
        logShatterService.setClickHouseConnection(clickHouseConnection);
        logShatterService.setHealthMetaDao(healthMetaDao);
        logShatterService.setLogshatterMetaDao(logshatterMetaDao);
        logShatterService.setReadSemaphore(readSemaphore);
        logShatterService.setClickHouseClusterSaveService(clickHouseClusterSaveService);

        parseExecutorService = logShatterService.createExecutorService(2, "parser-");
        outputExecutorService = logShatterService.createExecutorService(2, "output-");

        when(queuesCounter.getQueueThatReachedLimit()).thenReturn(null);

        when(readSemaphore.getQueuesCounterForSource(any(), anyBoolean())).thenReturn(queuesCounter);
    }


    void startReaderService() {
        startReaderService(Collections.singletonList("man"));
    }

    void startReaderService(List<String> dataCenters) {
        LogBrokerConfigurationService logBrokerConfigurationService =
            new LogBrokerConfigurationService(
                configurationService,
                new LogShatterShardingService(null, false, null),
                "",
                ""
            );
        sut = new LogBrokerReaderService(
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
                        3,
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE
                    ),
                    readSemaphore,
                    logShatterService,
                    logBrokerConfigurationService,
                    logshatterMetaDao,
                    null,
                    new BatchErrorLoggerFactory(
                        10, 100,
                        new ErrorBoosterLogger(false, "test"),
                        new LogSamplingPropertiesService(1.0f, Collections.emptyMap())
                    ),
                    new StuffFileInfoLogger(),
                    10
                ),
                executorServiceFactory,
                new LbLogshatterDataCenters(dataCenters.get(0), dataCenters),
                logBrokerConfigurationService,
                logShatterMonitoring,
                Arrays.asList("man", "iva", "sas", "vla", "myt", "kafka-bs"),
                new KeyValueMetricSupplier(),
                true
            ),
            dataCenters
        );

        sut.startAsync();

        // Первый runScheduledTasks выполняет LogBrokerReaderService
        runScheduledTasks();

        // Второй runScheduledTasks выполняет LogBrokerReaderService и LbDataCenterReaderService'ы
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
            new LogShatterParserWorker(logShatterService).parseOneSourceContext();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    void runInternalParserAsync() {
        parseExecutorService.execute(() -> {
            try {
                new LogShatterParserWorker(logShatterService).parseOneSourceContext();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        try {
            parseExecutorService.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void runInternalAndExternalParsersAsync() {
        parseExecutorService.execute(() -> {
            try {
                new LogShatterParserWorker(logShatterService).parseOneSourceContext();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        parseExecutorService.execute(() -> {
            try {
                new LogShatterExternalParserWorker(logShatterService).parseOneSourceContext();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        try {
            parseExecutorService.awaitTermination(1500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void runWriters(int outputBatchSize) {
        try {
            logShatterService.setOutputBatchSize(outputBatchSize);
            new LogShatterOutputWorker(logShatterService, 0).outputOnce();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    void runInternalAndExternalWriters() {
        try {
            new LogShatterOutputWorker(logShatterService, 0).outputOnce();
            new LogShatterExternalOutputWorker(logShatterService, 1).outputOnce();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    void runInternalAndExternalWritersAsync() {
        outputExecutorService.execute(() -> {
            try {
                new LogShatterOutputWorker(logShatterService, 0).outputOnce();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        outputExecutorService.execute(() -> {
            try {
                new LogShatterExternalOutputWorker(logShatterService, 1).outputOnce();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        try {
            outputExecutorService.awaitTermination(1500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void givenConfigs(LogShatterConfig... configs) {
        this.configurationService.setConfigs(Arrays.asList(configs));
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
                                .setSources(Collections.singletonList(LogSource.create("logbroker://market-health" +
                                    "-stable--other")))
                                .setDataClickHouseTable(createClickHouseTableDefinition(DEFAULT_TABLE_NAME))
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

    void setClusterSaveServiceFailedBehavior() {
        doThrow(new ClusterSaveException("message", "test")).when(clickHouseClusterSaveService)
            .saveToClickhouseInNativeFormat(anyString(), anyString(), anyList(), anyList());
    }

    LogParserProvider parserCachingInputTo(List<String> inputStore) throws Exception {
        LogParserProvider parserProvider = new LogParserProvider(
            LbReadingTester.TestLogParser.class.getName(), null, null
        );
        LogParserProvider parserProviderSpy = Mockito.spy(parserProvider);

        LbReadingTester.TestLogParser testLogParser = new LbReadingTester.TestLogParser(inputStore);
        Mockito.when(parserProviderSpy.createParser(Mockito.anyMap()))
            .thenReturn(testLogParser);

        return parserProviderSpy;
    }


    void verifyInteractions(InteractionVerifier... interactionVerifiers) {
        Stream.of(interactionVerifiers).forEach(Runnable::run);
        try {
            verify(readSemaphore, atLeast(0)).waitForRead();
            verify(readSemaphore, atLeast(0)).notifyRead();
            verify(readSemaphore, atLeast(0)).getQueuesCounterForSource(any(), anyBoolean());
            verify(readSemaphore, atLeast(0)).incrementTotalInternalQueue(anyLong());
            verify(readSemaphore, atLeast(0)).decrementTotalInternalQueue(anyLong());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        verifyNoMoreInteractions(getAllMocks());
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    void verifyInternalAndExternalInteractionsOnReadData(
        int waitForReadInvocationsNumber,
        int waitForReadExternalInvocations,
        int notifyReadInvocations,
        int getInternalQueuesCounterInvocations,
        int getExternalQueuesCounterInvocations,
        int incrementTotalInternalQueueInvocations,
        int incrementTotalExternalQueueInvocations,
        int commitInvocations
    ) {
        try {
            verify(readSemaphore, atLeast(waitForReadInvocationsNumber)).waitForRead();
            verify(readSemaphore, atLeast(waitForReadExternalInvocations)).waitForReadExternalClusterData();
            verify(readSemaphore, atLeast(notifyReadInvocations)).notifyRead();
            verify(readSemaphore, times(getInternalQueuesCounterInvocations)).getQueuesCounterForSource(any(),
                eq(true));
            verify(readSemaphore, times(getExternalQueuesCounterInvocations)).getQueuesCounterForSource(any(),
                eq(false));
            verify(readSemaphore, times(incrementTotalInternalQueueInvocations)).incrementTotalInternalQueue(anyLong());
            verify(readSemaphore, times(incrementTotalExternalQueueInvocations)).incrementTotalExternalQueue(anyLong());
            verify(readResponderMock, times(commitInvocations)).commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    void verifyInternalAndExternalInteractionsOnParseData(
        int getInternalParseQueueInvocations,
        int getExternalParseQueueInvocations,
        int getInternalOutputQueueInvocations,
        int getExternalOutputQueueInvocations,
        int decrementTotalInternalQueueInvocations,
        int decrementTotalExternalQueueInvocations,
        int decrementInternalQueuesCounterInvocations,
        int decrementExternalQueuesCounterInvocations
    ) {
        try {
            verify(logShatterService, times(getInternalParseQueueInvocations)).getInternalParseQueue();
            verify(logShatterService, times(getExternalParseQueueInvocations)).getExternalParseQueue();
            verify(logShatterService, times(getInternalOutputQueueInvocations)).getInternalOutputQueue();
            verify(logShatterService, times(getExternalOutputQueueInvocations)).getExternalOutputQueue();
            verify(readSemaphore, times(decrementTotalInternalQueueInvocations)).decrementTotalInternalQueue(anyLong());
            verify(readSemaphore, times(decrementTotalExternalQueueInvocations)).decrementTotalExternalQueue(anyLong());
            verify(queuesCounter, times(decrementInternalQueuesCounterInvocations)).decrement(anyLong(), eq(true));
            verify(queuesCounter, times(decrementExternalQueuesCounterInvocations)).decrement(anyLong(), eq(false));
            verify(readResponderMock, times(0)).commit();
            clearInvocations(logShatterService, readSemaphore, queuesCounter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    void verifyInternalAndExternalInteractionsOnWriteData(
        int decrementTotalInternalQueueInvocations,
        int decrementTotalExternalQueueInvocations,
        int decrementInternalQueuesCounterInvocations,
        int decrementExternalQueuesCounterInvocations,
        int getInternalOutputQueueInvocations,
        int getExternalOutputQueueInvocations,
        int saveToClickhouseInvocations,
        int saveToMdbClickhouseInvocations
    ) {
        try {
            verify(readSemaphore, times(decrementTotalInternalQueueInvocations)).decrementTotalInternalQueue(anyLong());
            verify(readSemaphore, times(decrementTotalExternalQueueInvocations)).decrementTotalExternalQueue(anyLong());
            verify(queuesCounter, times(decrementInternalQueuesCounterInvocations)).decrement(anyLong(), eq(true));
            verify(queuesCounter, times(decrementExternalQueuesCounterInvocations)).decrement(anyLong(), eq(false));
            verify(logShatterService, times(getInternalOutputQueueInvocations)).getInternalOutputQueue();
            verify(logShatterService, times(getExternalOutputQueueInvocations)).getExternalOutputQueue();
            verify(logShatterService, times(saveToClickhouseInvocations)).saveToClickHouse(any(),
                any(LogShatterConfig.class));
            verify(clickHouseClusterSaveService, times(saveToMdbClickhouseInvocations))
                .saveToClickhouseInNativeFormat(anyString(), anyString(), anyList(), anyList());
            verify(readResponderMock).commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void verifyNoInteractions() {
        verifyInteractions();
    }

    void verifyQueueSizeIncreasedBy(long amountBytes) {
        verifyQueueSizeIncreasedBy(amountBytes, 0);
    }

    void verifyQueueSizeIncreasedBy(double amountBytes, long acceptableDifference) {
        verify(readSemaphore).incrementTotalInternalQueue(AdditionalMatchers.and(
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
        return LogShatterConfig.newBuilder().setConfigId("/1");
    }

    static LogShatterConfig config(String topic) {
        return config(topic, DEFAULT_TABLE_NAME);
    }

    static LogShatterConfig config(String topic, String table) {
        try {
            return config()
                .setSources(Collections.singletonList(LogSource.create("logbroker://" + topic)))
                .setDataClickHouseTable(
                    createClickHouseTableDefinition(table)
                )
                .build();
        } catch (ConfigValidationException e) {
            throw new RuntimeException(e);
        }
    }

    private static ClickHouseTableDefinition createClickHouseTableDefinition(String tableName) {
        return new ClickHouseTableDefinitionImpl(
            DB_NAME,
            tableName,
            Collections.emptyList(),
            TableDescriptionUtils.DEFAULT_ENGINE
        );
    }

    static String getTableNameWithDb(String tableName) {
        return DB_NAME + "." + tableName;
    }

    static LogShatterConfig.Builder configThatMatchesEverything() {
        try {
            return LogShatterConfig.newBuilder()
                .setConfigId("/2")
                .setLogPath("**")
                .setLogHosts("**")
                .setParams(new HashMap<>())
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

    static MessageData messageData(long seqNo, String sourceId, String fileName) {
        return messageData(seqNo, sourceId, fileName, CompressionCodec.RAW, "1".getBytes());
    }

    static MessageData messageData(long seqNo, String sourceId, CompressionCodec compressionCodec, byte[] data) {
        return messageData(seqNo, sourceId, "/var/log/app1/app1.log", compressionCodec, data);
    }

    static MessageData messageData(
        long seqNo,
        String sourceId,
        String fileName,
        CompressionCodec compressionCodec,
        byte[] data
    ) {
        Map<String, List<String>> extraFields;
        if (fileName != null) {
            extraFields = ImmutableMap.of(
                "server", Collections.singletonList(SERVER1),
                "file", Collections.singletonList(fileName)
            );
        } else {
            extraFields = ImmutableMap.of(
                "server", Collections.singletonList(SERVER1)
            );
        }

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
                extraFields
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
                readResponderMock = mock(StreamListener.ReadResponder.class);
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

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
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

    @SuppressWarnings("checkstyle:redundantmodifier")
    public static class TestLogParser implements LogParser {

        private static final TableDescription TABLE_DESCRIPTION = TableDescription.createDefault(
            new Column("data", ColumnType.Int64)
        );

        private final List<String> resultStore;

        TestLogParser(List<String> resultStore) {
            this.resultStore = resultStore;
        }

        public TestLogParser() {
            this.resultStore = new ArrayList<>();
        }

        @Override
        public TableDescription getTableDescription() {
            return TABLE_DESCRIPTION;
        }

        @Override
        public void parse(String line, ParserContext context) throws Exception {
            resultStore.add(line);
            context.write(new Date(), 1);
        }
    }
}
