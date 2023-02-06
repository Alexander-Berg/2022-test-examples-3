package ru.yandex.market.logshatter;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;

import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinition;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.health.configs.logshatter.LogBatch;
import ru.yandex.market.health.configs.logshatter.config.ConfigValidationException;
import ru.yandex.market.health.configs.logshatter.config.LogShatterConfig;
import ru.yandex.market.logbroker.pull.LogBrokerSourceKey;
import ru.yandex.market.logshatter.config.pipeline.LogShatterPipelineConfig;
import ru.yandex.market.logshatter.config.pipeline.LogShatterPipelineConfigDao;
import ru.yandex.market.logshatter.config.pipeline.common.LogShatterCommonPipelineConfig;
import ru.yandex.market.logshatter.config.pipeline.common.LogShatterCommonPipelineConfigDao;
import ru.yandex.market.logshatter.config.pipeline.common_topics.LogShatterCommonTopicsPipelineConfig;
import ru.yandex.market.logshatter.config.pipeline.common_topics.LogShatterCommonTopicsPipelineConfigDao;
import ru.yandex.market.logshatter.logging.BatchErrorLoggerFactory;
import ru.yandex.market.logshatter.logging.ErrorBoosterLogger;
import ru.yandex.market.logshatter.logging.LogSamplingPropertiesService;
import ru.yandex.market.logshatter.parser.LogParserProvider;
import ru.yandex.market.logshatter.parser.TableDescription;
import ru.yandex.market.logshatter.pipeline.LogShatterPipeline;
import ru.yandex.market.logshatter.reader.QueuesLimits;
import ru.yandex.market.logshatter.reader.ReadSemaphore;
import ru.yandex.market.logshatter.reader.SourceContext;
import ru.yandex.market.logshatter.reader.logbroker.common.LogbrokerSourceContext;

import static org.assertj.core.api.Assertions.assertThat;

public class LogShatterServiceConfigTest {
    private LogShatterCommonTopicsPipelineConfigDao commonTopicsPipelineConfigDao;
    private LogShatterCommonPipelineConfigDao commonPipelineConfigDao;
    private LogShatterPipelineConfigDao pipelineConfigDao;
    private LogShatterService logShatterService;
    private LogShatterConfig logShatterConfig;
    private LogBrokerSourceKey logBrokerSourceKey;
    private BatchErrorLoggerFactory errorLoggerFactory;
    private ReadSemaphore readSemaphore;


    @BeforeEach
    public void setUp() throws ConfigValidationException {

        // Init mongo
        MemoryBackend memoryBackend = new MemoryBackend();
        MongoServer mongoServer = new MongoServer(memoryBackend);
        ServerAddress serverAddress = new ServerAddress(mongoServer.bind());
        MongoClient mongoClient = new MongoClient(serverAddress);
        String databaseName = "db";
        SimpleMongoDbFactory mongoDbFactory = new SimpleMongoDbFactory(mongoClient, databaseName);
        MongoTemplate mongoTemplate = Mockito.spy(new MongoTemplate(mongoDbFactory));

        String configId = "test_config_id_1";
        String clusterId = "test_cluster_1";
        String chDatabaseName = "health";
        String tableName = "test";
        TableDescription tableDescription = TableDescription.createDefault(
            Arrays.asList(
                new Column("host", ColumnType.String),
                new Column("test1", ColumnType.String),
                new Column("test2", ColumnType.Int32)
            ));
        ClickHouseTableDefinition notDistributedTableDefinition = new ClickHouseTableDefinitionImpl(
            chDatabaseName,
            tableName,
            tableDescription.getColumns(),
            tableDescription.getEngine()
        );

        ErrorBoosterLogger errorBoosterLogger = new ErrorBoosterLogger(false, "test");
        LogSamplingPropertiesService logSamplingPropertiesService = new LogSamplingPropertiesService(1.0f,
            Collections.emptyMap());

        logShatterConfig = LogShatterConfig.newBuilder()
            .setConfigId(configId)
            .setDataClickHouseTable(notDistributedTableDefinition)
            .setParserProvider(new LogParserProvider(LogShatterExternalOutputWorkerTest.TestParser.class.getName(),
                null, null))
            .setClickHouseClusterId(clusterId)
            .build();

        commonPipelineConfigDao = Mockito.spy(
            new LogShatterCommonPipelineConfigDao(mongoTemplate, LogShatterCommonPipelineConfig.COMMON_ID));
        commonTopicsPipelineConfigDao = Mockito.spy(
            new LogShatterCommonTopicsPipelineConfigDao(mongoTemplate,
                LogShatterCommonTopicsPipelineConfig.COMMON_TOPICS_ID));
        pipelineConfigDao = Mockito.spy(new LogShatterPipelineConfigDao(mongoTemplate));
        logBrokerSourceKey = Mockito.spy(new LogBrokerSourceKey(null, null, null, null, null, null, null));
        logShatterService = Mockito.spy(new LogShatterService());
        errorLoggerFactory = Mockito.spy(
            new BatchErrorLoggerFactory(1, 1, errorBoosterLogger, logSamplingPropertiesService));
        readSemaphore = Mockito.spy(new ReadSemaphore());
        logShatterService.setCommonPipelineConfigDao(commonPipelineConfigDao);
        logShatterService.setCommonTopicsPipelineConfigDao(commonTopicsPipelineConfigDao);
        logShatterService.setPipelineConfigDao(pipelineConfigDao);
    }


    @Test
    public void ifUsingMongoConfigFirstTime_createNewMongoConfig() {
        assertThat(commonPipelineConfigDao.findCommonPipeline().isPresent()).isFalse();
        logShatterService.setUseMongoConfig(true);

        logShatterService.initCommonPipelineConfigs();

        assertThat(commonPipelineConfigDao.findCommonPipeline().isPresent()).isTrue();
    }

    @Test
    public void ifUsingMongoCommonTopicsConfigFirstTime_createNewCommonTopicsMongoConfig() {
        assertThat(commonTopicsPipelineConfigDao.findCommonTopicsPipelineConfig().isPresent()).isFalse();
        logShatterService.setUseCommonTopicsConfig(true);

        logShatterService.initCommonTopicsPipelineConfig();

        assertThat(commonTopicsPipelineConfigDao.findCommonTopicsPipelineConfig().isPresent()).isTrue();
    }

    @Test
    public void ifUsingPrivatePipelinesConfig_readConfigFromMongoAndAddMessageToPrivateQueue() {
        // Убеждаемся, что бд пустая
        assertThat(pipelineConfigDao.findAllPrivate()).isNullOrEmpty();

        // Включаем новую фичу
        logShatterService.setUsePrivatePipelinesConfig(true);

        // Отключаем обработку сообщений
        Mockito.when(logShatterService.isRunning()).thenReturn(false);

        // Создаем сообщение из ЛБ
        SourceContext sourceContext = new LogbrokerSourceContext(
            Paths.get("/dev/null"),
            logShatterConfig,
            logBrokerSourceKey,
            errorLoggerFactory,
            0,
            readSemaphore.getEmptyQueuesCounter()
        );
        // Создаем батч для сообщения ЛБ
        LogBatch batch = new LogBatch(
            Stream.empty(), 0, 0, 0, Duration.ofMillis(0),
            Arrays.asList(
                new Column("date", ColumnType.Date),
                new Column("timestamp", ColumnType.UInt32),
                new Column("value", ColumnType.Int32)
            ),
            "sourceName",
            "sourceHost",
            null
        );
        sourceContext.getParseQueue().add(batch);

        // Даем понять, что топик будет специфичный
        Mockito.when(logBrokerSourceKey.getTopic()).thenReturn("test-topic--stable");

        // Создаем новый конфиг и сохраняем его в монгу
        LogShatterPipelineConfig config = LogShatterPipelineConfig
            .builder()
            .id("test-config-for-test-project")
            .internalParseThreadCount(1)
            .internalLimits(new QueuesLimits(ImmutableList.of(new QueuesLimits.QueueLimit("test-topic--.*",
                1024L * 1024L * 100))))
            .internalParseThreadCount(1)
            .build();
        pipelineConfigDao.saveOrUpdate(config);

        // Инициализируем приватные пайплайны
        logShatterService.initPrivatePipelines();

        // Приватный пайплайн должен был создаться
        Map<String, LogShatterPipeline> privatePipelines = logShatterService.getPrivatePipelines();

        assertThat(privatePipelines).isNotEmpty();
        assertThat(privatePipelines.size()).isEqualTo(1);

        LogShatterPipeline privatePipeline = privatePipelines.get(config.getId());

        assertThat(privatePipeline).isNotNull();

        // Отправляем в очередь
        logShatterService.addToParseQueue(sourceContext, false);

        // Проверяем, что в очередь попало
        assertThat(privatePipeline.getInternalParseQueue().isEmpty()).isFalse();

    }

}
