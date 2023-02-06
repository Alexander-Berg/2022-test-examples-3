package ru.yandex.market.logshatter.reader;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;

import ru.yandex.market.logshatter.LogShatterMonitoring;
import ru.yandex.market.logshatter.config.pipeline.common.LogShatterCommonPipelineConfig;
import ru.yandex.market.logshatter.config.pipeline.common.LogShatterCommonPipelineConfigDao;
import ru.yandex.market.logshatter.config.pipeline.common_topics.LogShatterCommonTopicsPipelineConfig;
import ru.yandex.market.logshatter.config.pipeline.common_topics.LogShatterCommonTopicsPipelineConfigDao;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class SemaphoreTest {

    private ReadSemaphore readSemaphore;
    private LogShatterCommonPipelineConfigDao commonPipelineConfigDao;
    private LogShatterCommonTopicsPipelineConfigDao commonTopicsPipelineConfigDao;


    @BeforeEach
    public void setUp() throws Exception {
        // Init mongo
        MemoryBackend memoryBackend = new MemoryBackend();
        MongoServer mongoServer = new MongoServer(memoryBackend);
        ServerAddress serverAddress = new ServerAddress(mongoServer.bind());
        MongoClient mongoClient = new MongoClient(serverAddress);
        String databaseName = "db";
        SimpleMongoDbFactory mongoDbFactory = new SimpleMongoDbFactory(mongoClient, databaseName);
        MongoTemplate mongoTemplate = Mockito.spy(new MongoTemplate(mongoDbFactory));

        // Daos
        commonPipelineConfigDao =
            Mockito.spy(new LogShatterCommonPipelineConfigDao(mongoTemplate,
            LogShatterCommonPipelineConfig.COMMON_ID));
        commonTopicsPipelineConfigDao =
            Mockito.spy(new LogShatterCommonTopicsPipelineConfigDao(mongoTemplate,
            LogShatterCommonTopicsPipelineConfig.COMMON_TOPICS_ID));

        // Monitoring
        LogShatterMonitoring monitoring = Mockito.spy(new LogShatterMonitoring());

        // Semaphore
        readSemaphore = Mockito.spy(new ReadSemaphore());
        readSemaphore.setZookeeperQuorum("blacksmith01ht.market.yandex.net:2181,blacksmith01vt.market.yandex" +
            ".net:2181,blacksmith01et.market.yandex.net:2181");
        readSemaphore.setZookeeperPrefix("logshatter");
        readSemaphore.setCommonPipelineConfigDao(commonPipelineConfigDao);
        readSemaphore.setCommonTopicsPipelineConfigDao(commonTopicsPipelineConfigDao);
        readSemaphore.setMonitoring(monitoring);
    }

    @Test
    public void createCommonTopicsPipelineConfigAndParseOneMessage() {
        // Creating new configs in mongo
        commonPipelineConfigDao.saveOrUpdate(LogShatterCommonPipelineConfig.builder().build());
        commonTopicsPipelineConfigDao.saveOrUpdate(LogShatterCommonTopicsPipelineConfig
            .builder()
            .internalLimits(new QueuesLimits("market-health-.+:1,^marketstat$:2,blabla--testing:3"))
            .externalLimits(new QueuesLimits("market-health-.+:1,^marketstat$:2,blabla--testing:3,^ohmy--other$:4"))
            .build());

        // Enabling mongo configs
        readSemaphore.setUseMongoConfig(true);
        readSemaphore.setUseCommonTopicsConfig(true);

        readSemaphore.addCommonLimits();
        readSemaphore.addCommonTopicsLimits();

        readSemaphore.initCurrentSizes();
        readSemaphore.initLimitMap();

        assertThat(readSemaphore.getAllInternalLimits().size()).isEqualTo(3);
        assertThat(readSemaphore.getAllExternalLimits().size()).isEqualTo(4);

        assertThat(readSemaphore.getQueuesIdToCurrentSizeBytes().size()).isEqualTo(3);
        assertThat(readSemaphore.getExternalQueuesIdToCurrentSizeBytes().size()).isEqualTo(4);

        assertThat(readSemaphore.getQueuesIdToLimitsBytes().size()).isEqualTo(3);
        assertThat(readSemaphore.getExternalQueuesIdToLimitsBytes().size()).isEqualTo(4);
    }
}
