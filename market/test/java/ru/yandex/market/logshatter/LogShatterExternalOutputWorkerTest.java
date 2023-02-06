package ru.yandex.market.logshatter;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinition;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.health.configs.logshatter.LogBatch;
import ru.yandex.market.health.configs.logshatter.config.ConfigValidationException;
import ru.yandex.market.health.configs.logshatter.config.LogShatterConfig;
import ru.yandex.market.logbroker.pull.LogBrokerSourceKey;
import ru.yandex.market.logshatter.logging.BatchErrorLoggerFactory;
import ru.yandex.market.logshatter.output.ConfigOutputQueue;
import ru.yandex.market.logshatter.output.OutputQueue;
import ru.yandex.market.logshatter.parser.LogParser;
import ru.yandex.market.logshatter.parser.LogParserProvider;
import ru.yandex.market.logshatter.parser.ParserContext;
import ru.yandex.market.logshatter.parser.TableDescription;
import ru.yandex.market.logshatter.reader.ReadSemaphore;
import ru.yandex.market.logshatter.reader.SourceContext;
import ru.yandex.market.logshatter.reader.logbroker.common.LogbrokerSourceContext;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LogShatterExternalOutputWorkerTest {
    @Mock
    private LogShatterService logShatterService;
    @Mock
    private LogBrokerSourceKey logBrokerSourceKey;
    @Mock
    private BatchErrorLoggerFactory errorLoggerFactory;
    private LogShatterExternalOutputWorker externalOutputWorker;
    private OutputQueue outputQueue;

    @Before
    public void setUp() throws ConfigValidationException {
        outputQueue = new OutputQueue();
        externalOutputWorker = Mockito.spy(new LogShatterExternalOutputWorker(logShatterService, 11));
        when(externalOutputWorker.getLogShatterService()).thenReturn(logShatterService);
        when(logShatterService.getExternalOutputQueue()).thenReturn(outputQueue);
    }

    @Test
    public void getConfigOutputQueueWhenFailedClusterFieldIsNotSet()
        throws InterruptedException, ConfigValidationException {
        ConfigOutputQueue expectedConfigQueue = createConfigOutputQueue("test_config_id_1", "test_cluster_1");
        SourceContext sourceContext = createSourceContext("test_config_id_1", "test_cluster_1");
        outputQueue.add(sourceContext);

        ConfigOutputQueue actualOutputQueue = externalOutputWorker.getConfigOutputQueue();

        compareConfigQueues(expectedConfigQueue, actualOutputQueue);

        Assert.assertEquals(1, outputQueue.queueSize());
    }

    @Test
    public void getConfigOutputQueueWhenFailedClusterFieldIsSet()
        throws InterruptedException, ConfigValidationException {
        externalOutputWorker.setFailedClusterId("test_cluster_1", 1, TimeUnit.MINUTES);
        ConfigOutputQueue expectedConfigQueue = createConfigOutputQueue("test_config_id_2", "test_cluster_2");
        createQueueWithDifferentSourceContexts();

        ConfigOutputQueue actualOutputQueue = externalOutputWorker.getConfigOutputQueue();

        compareConfigQueues(expectedConfigQueue, actualOutputQueue);

        Assert.assertEquals(4, outputQueue.queueSize());
    }

    @Test
    public void getConfigOutputQueueWhenFailedClusterFieldIsSetAndQueueContainsOnlyOneConfigData()
        throws InterruptedException, ConfigValidationException {
        externalOutputWorker.setFailedClusterId("test_cluster_1", 1, TimeUnit.MINUTES);
        ConfigOutputQueue expectedConfigQueue = createConfigOutputQueueWithSameSourceContexts(
            "test_config_id_1", "test_cluster_1");
        createQueueWithSameSourceContexts();

        ConfigOutputQueue actualOutputQueue = externalOutputWorker.getConfigOutputQueue();

        compareConfigQueues(expectedConfigQueue, actualOutputQueue);

        Assert.assertEquals(10, outputQueue.queueSize());
    }

    @Test
    public void getConfigOutputQueueWhenFailedClusterFieldIsSetAndTimeExpired()
        throws InterruptedException, ConfigValidationException {
        externalOutputWorker.setFailedClusterId("test_cluster_1", 3, TimeUnit.SECONDS);
        ConfigOutputQueue expectedConfigQueue = createConfigOutputQueue("test_config_id_1", "test_cluster_1");
        createQueueWithDifferentSourceContexts();

        Thread.sleep(3000);
        ConfigOutputQueue actualOutputQueue = externalOutputWorker.getConfigOutputQueue();

        compareConfigQueues(expectedConfigQueue, actualOutputQueue);

        Assert.assertEquals(4, outputQueue.queueSize());
    }

    private void compareConfigQueues(ConfigOutputQueue expectedConfigQueue, ConfigOutputQueue actualOutputQueue) {
        compareLogShatterConfigs(expectedConfigQueue.getLogShatterConfig(), actualOutputQueue.getLogShatterConfig());
        Assert.assertEquals(expectedConfigQueue.queueSize(), actualOutputQueue.queueSize());
        Assert.assertEquals(expectedConfigQueue.lockedSize(), actualOutputQueue.lockedSize());
    }

    private void compareLogShatterConfigs(LogShatterConfig expectedConfig, LogShatterConfig actualConfig) {
        boolean areEqual = expectedConfig.getConfigId().equals(actualConfig.getConfigId()) &&
            expectedConfig.getClickHouseClusterId().equals(actualConfig.getClickHouseClusterId()) &&
            expectedConfig.getDataTableName().equals(actualConfig.getDataTableName());
        Assert.assertTrue(areEqual);
    }

    private SourceContext createSourceContext(String configId, String clusterId) throws ConfigValidationException {
        LogShatterConfig logShatterConfig = createLogShatterConfig(configId, clusterId);

        SourceContext sourceContext = new LogbrokerSourceContext(
            Paths.get("/dev/null"),
            logShatterConfig,
            logBrokerSourceKey,
            errorLoggerFactory,
            0,
            new ReadSemaphore().getEmptyQueuesCounter()
        );

        LogBatch batch = new LogBatch(
            Stream.empty(), 10, 1, 1, Duration.ofMillis(0),
            sourceContext.getLogParser().getTableDescription().getColumns(),
            "sourceName",
            "sourceHost",
            null
        );

        sourceContext.getOutputQueue().add(batch);

        return sourceContext;
    }

    private LogShatterConfig createLogShatterConfig(String configId, String clusterId)
        throws ConfigValidationException {
        String databaseName = "health";
        String tableName = "test";
        TableDescription tableDescription = TableDescription.createDefault(
            Arrays.asList(
                new Column("host", ColumnType.String),
                new Column("test1", ColumnType.String),
                new Column("test2", ColumnType.Int32)
            ));
        ClickHouseTableDefinition notDistributedTableDefinition = new ClickHouseTableDefinitionImpl(
            databaseName,
            tableName,
            tableDescription.getColumns(),
            tableDescription.getEngine()
        );

        return LogShatterConfig.newBuilder()
            .setConfigId(configId)
            .setDataClickHouseTable(notDistributedTableDefinition)
            .setParserProvider(new LogParserProvider(TestParser.class.getName(), null, null))
            .setClickHouseClusterId(clusterId)
            .build();
    }

    private ConfigOutputQueue createConfigOutputQueue(String configId, String clusterId)
        throws ConfigValidationException {
        ConfigOutputQueue configQueue = new ConfigOutputQueue(createLogShatterConfig(configId, clusterId));
        configQueue.add(createSourceContext(configId, clusterId));
        return configQueue;
    }

    private ConfigOutputQueue createConfigOutputQueueWithSameSourceContexts(String configId, String clusterId)
        throws ConfigValidationException {
        ConfigOutputQueue configQueue = new ConfigOutputQueue(createLogShatterConfig(configId, clusterId));
        for (int i = 0; i < 10; i++) {
            configQueue.add(createSourceContext(configId, clusterId));
        }
        return configQueue;
    }

    private void createQueueWithDifferentSourceContexts() throws ConfigValidationException {
        SourceContext firstSourceContext = createSourceContext("test_config_id_1", "test_cluster_1");
        SourceContext secondSourceContext = createSourceContext("test_config_id_2", "test_cluster_2");
        SourceContext thirdSourceContext = createSourceContext("test_config_id_3", "test_cluster_3");
        SourceContext fourthSourceContext = createSourceContext("test_config_id_3", "test_cluster_4");
        outputQueue.add(firstSourceContext);
        outputQueue.add(secondSourceContext);
        outputQueue.add(thirdSourceContext);
        outputQueue.add(fourthSourceContext);
    }

    private void createQueueWithSameSourceContexts() throws ConfigValidationException {
        for (int i = 0; i < 10; i++) {
            SourceContext sourceContext = createSourceContext("test_config_id_1", "test_cluster_1");
            outputQueue.add(sourceContext);
        }
    }

    public static class TestParser implements LogParser {
        private int parsedLines = 0;

        TableDescription tableDescription = TableDescription.createDefault(
            Arrays.asList(
                new Column("host", ColumnType.String),
                new Column("test1", ColumnType.String),
                new Column("test2", ColumnType.Int32)
            ));

        @Override
        public TableDescription getTableDescription() {
            return tableDescription;
        }

        @Override
        public void parse(String line, ParserContext context) throws Exception {
            parsedLines++;
        }
    }

}
