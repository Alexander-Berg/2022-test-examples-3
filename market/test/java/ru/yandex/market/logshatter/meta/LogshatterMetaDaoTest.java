package ru.yandex.market.logshatter.meta;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.health.configs.logshatter.config.ConfigValidationException;
import ru.yandex.market.health.configs.logshatter.config.LogShatterConfig;
import ru.yandex.market.logshatter.logging.BatchErrorLoggerFactory;
import ru.yandex.market.logshatter.logging.ErrorBoosterLogger;
import ru.yandex.market.logshatter.logging.LogSamplingPropertiesService;
import ru.yandex.market.logshatter.parser.LogParserProvider;
import ru.yandex.market.logshatter.parser.internal.LogshatterPerformanceLog;
import ru.yandex.market.logshatter.reader.ReadSemaphore;
import ru.yandex.market.logshatter.reader.SourceContext;

/**
 * @author kukabara
 */
public class LogshatterMetaDaoTest {
    private LogshatterMetaDao mongoMetaDao;
    private MongoDatabase mongoDatabase;

    @Before
    public void init() {
        mongoDatabase = new MongoClient(new ServerAddress(new MongoServer(new MemoryBackend()).bind()))
            .getDatabase("health");
        mongoMetaDao = new LogshatterMetaDao(mongoDatabase);
    }

    @Test
    public void testCleanup() throws Exception {
        mongoMetaDao.getCollection().deleteMany(new Document());

        Date actualDate = new Date(1564585732000L);
        Date oldDate = new Date(actualDate.getTime() - TimeUnit.DAYS.toMillis(6));
        Date cleanupBeforeDate = new Date(actualDate.getTime() - TimeUnit.DAYS.toMillis(4));

        String cleanupOrigin = "cleanupOrigin";
        String otherOrigin = "otherOrigin";


        SourceContext actualSource = createSourceContext(cleanupOrigin, "a", "db.table", "name", 42, 42);
        SourceContext otherOriginSource = createSourceContext(otherOrigin, "b", "db.table", "name", 42, 42);

        mongoMetaDao.save(actualSource, actualDate);
        mongoMetaDao.save(otherOriginSource, oldDate);

        Set<SourceKey> sourcesNotForCleanup = new HashSet<>();
        sourcesNotForCleanup.add(actualSource.getSourceKey());
        sourcesNotForCleanup.add(otherOriginSource.getSourceKey());

        mongoMetaDao.save(createSourceContext(cleanupOrigin, "old1", "db.table", "name", 42, 42), oldDate);
        mongoMetaDao.save(createSourceContext(cleanupOrigin, "old2", "db.table", "name", 42, 42), oldDate);


        Assert.assertEquals(4, mongoMetaDao.getCollection().count());
        mongoMetaDao.cleanupOldSources(cleanupOrigin, cleanupBeforeDate);
        Assert.assertEquals(2, mongoMetaDao.getCollection().count());

        Set<SourceKey> actual = StreamSupport.stream(mongoMetaDao.getCollection().find().spliterator(), false)
            .map(SourceMeta::getKey).collect(Collectors.toSet());


        Assert.assertEquals(sourcesNotForCleanup, actual);

    }

    private static SourceContext createSourceContext(String origin, String id, String table,
                                                     String name, long dataOffset, long fileOffset) throws Exception {
        SourceKey key = new SourceKey(origin, id, table);
        return new SourceContextImpl(
            key,
            new SourceMeta(key, name, dataOffset, fileOffset)
        );
    }

    private static class SourceContextImpl extends SourceContext {

        private SourceMeta sourceMeta;

        SourceContextImpl(SourceKey sourceKey, SourceMeta sourceMeta) throws ConfigValidationException {
            super(
                LogShatterConfig.newBuilder()
                    .setConfigId("")
                    .setDataClickHouseTable(
                        new ClickHouseTableDefinitionImpl(sourceKey.getTable(), Collections.emptyList(), null))
                    .setParserProvider(new LogParserProvider(LogshatterPerformanceLog.class.getName(), null, null))
                    .build(),
                sourceKey,
                new BatchErrorLoggerFactory(
                    500, 1000,
                    new ErrorBoosterLogger(false, "test"),
                    new LogSamplingPropertiesService(1.0f, Collections.emptyMap())
                ),
                new ReadSemaphore().getEmptyQueuesCounter()
            );
            this.sourceMeta = sourceMeta;
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public String getHost() {
            return null;
        }

        @Override
        public Path getPath() {
            return null;
        }

        @Override
        public long getDataOffset() {
            return sourceMeta.getDataOffset();
        }

        @Override
        public void setDataOffset(long dataOffset) {

        }

        @Override
        public long getFileOffset() {
            return sourceMeta.getFileOffset();
        }

        @Override
        public void setFileOffset(long fileOffset) {

        }

        @Override
        public String getName() {
            return sourceMeta.getName();
        }

        @Override
        public int getInstanceId() {
            return 0;
        }
    }

}
