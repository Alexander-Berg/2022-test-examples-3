package ru.yandex.market.health.configs.logshatter;

import java.time.Instant;
import java.util.Collections;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.clickhouse.ddl.ClickHouseDdlService;
import ru.yandex.market.health.configs.common.ConfigDaoTestBase;
import ru.yandex.market.health.configs.common.TableEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionStatus;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigEntity;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigSource;
import ru.yandex.market.health.configs.logshatter.mongo.DataSourceLogBrokerEntity;
import ru.yandex.market.health.configs.logshatter.mongo.DataSourceTrackerEntity;
import ru.yandex.market.health.configs.logshatter.mongo.DataSourcesEntity;
import ru.yandex.market.health.configs.logshatter.mongo.JavaParserEntity;
import ru.yandex.market.health.configs.logshatter.mongo.LogshatterConfigEntity;
import ru.yandex.market.health.configs.logshatter.mongo.LogshatterConfigVersionEntity;
import ru.yandex.market.health.configs.logshatter.mongo.ParserEntity;
import ru.yandex.market.health.configs.logshatter.spring.HealthConfigUtilsLogshatterInternalSpringConfig;
import ru.yandex.market.logshatter.parser.LogParser;
import ru.yandex.market.logshatter.parser.ParserContext;
import ru.yandex.market.logshatter.parser.TableDescription;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(classes = {
    HealthConfigUtilsLogshatterInternalSpringConfig.class,
    LogshatterConfigDaoTest.SpringConfiguration.class
})
class LogshatterConfigDaoTest extends ConfigDaoTestBase<LogshatterConfigEntity, LogshatterConfigVersionEntity> {
    private String searchingTopic = "searching--topic";
    private String searchingHost = "searchingHost";
    private String searchingPath = "searchingPath";
    private String searchingQueue = "searhcingQueue";
    private String searchingClassName = "ru.yandex.market.health.configs.logshatter" +
        ".LogshatterConfigDaoTest$SearchingParser";
    private String searchingTable = "searchingTable";

    private LogshatterConfigEntity searchingEntity = new LogshatterConfigEntity(
        searchingId,
        null,
        searchingTitle,
        null,
        Instant.now(),
        Instant.now(),
        null,
        null
    );

    private LogshatterConfigVersionEntity searchingVersion = new LogshatterConfigVersionEntity(
        new VersionedConfigEntity.VersionEntity.Id(searchingId, 0L),
        VersionedConfigSource.UI,
        VersionStatus.PUBLIC,
        new DataSourcesEntity(
            new DataSourceLogBrokerEntity(
                Collections.singletonList(searchingTopic),
                searchingHost,
                searchingPath,
                null
            ),
            Collections.singletonList(
                new DataSourceTrackerEntity(searchingQueue)
            ),
            null
        ),
        new ParserEntity(
            new JavaParserEntity(searchingClassName),
            null,
            null,
            null
        ),
        null,
        new TableEntity(
            "db", searchingTable
        ),
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    private LogshatterConfigEntity notSearchingEntity = new LogshatterConfigEntity(
        notSearchingId,
        null,
        "wrongTitle",
        null,
        Instant.now(),
        Instant.now(),
        null,
        null
    );

    private LogshatterConfigVersionEntity notSearchingVersion = new LogshatterConfigVersionEntity(
        new VersionedConfigEntity.VersionEntity.Id(notSearchingId, 0L),
        VersionedConfigSource.UI,
        VersionStatus.PUBLIC,
        new DataSourcesEntity(
            new DataSourceLogBrokerEntity(
                null,
                "wrongHost",
                "wrongPath",
                null
            ),
            Collections.singletonList(
                new DataSourceTrackerEntity("wrongQueue")
            ),
            null
        ),
        new ParserEntity(
            new JavaParserEntity("ru.yandex.market.health.configs.logshatter" +
                ".LogshatterConfigDaoTest$NotSearchingParser"),
            null,
            null,
            null
        ),
        null,
        new TableEntity(
            "db", "wrongTable"
        ),
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    @BeforeAll
    public void setUp() {
        setUp(searchingEntity, searchingVersion, notSearchingEntity, notSearchingVersion);
    }

    @Test
    public void findByParser() {
        testFindConfigs("parserClass", searchingClassName);
    }

    @Test
    public void findByHost() {
        testFindConfigs("logHost", searchingHost);
    }

    @Test
    public void findByPath() {
        testFindConfigs("logPath", searchingPath);
    }

    @Test
    public void findByQueue() {
        testFindConfigs("startrekQueue", searchingQueue);
    }

    @Test
    public void findByTopic() {
        testFindConfigs("topic", searchingTopic);
    }

    @Test
    public void findByDefaultTopic() {
        testFindConfigs("topic", "market-health-testing--other", notSearchingId);
    }

    @PropertySource("classpath:/test.properties")
    static class SpringConfiguration {
        @Bean
        public MongoTemplate logshatterConfigsMongoTemplate() {
            return new MongoTemplate(
                new MongoClient(
                    new ServerAddress(
                        new MongoServer(new MemoryBackend()).bind())
                ),
                "db"
            );
        }

        @Bean
        public ClickHouseDdlService clickHouseDdlService() {
            return Mockito.mock(ClickHouseDdlService.class);
        }
    }

    public static class SearchingParser implements LogParser {

        @Override
        public TableDescription getTableDescription() {
            return null;
        }

        @Override
        public void parse(String line, ParserContext context) throws Exception {

        }
    }

    public static class NotSearchingParser implements LogParser {

        @Override
        public TableDescription getTableDescription() {
            return null;
        }

        @Override
        public void parse(String line, ParserContext context) throws Exception {

        }
    }
}
