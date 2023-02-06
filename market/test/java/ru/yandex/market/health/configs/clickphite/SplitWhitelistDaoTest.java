package ru.yandex.market.health.configs.clickphite;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import ru.yandex.market.health.configs.clickphite.mongo.SplitWhitelistEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SplitWhitelistDaoTest {

    MongoServer mongoServer;
    SplitWhitelistDao splitWhitelistDao;

    @BeforeEach
    void setup() {
        mongoServer = new MongoServer(new MemoryBackend());
        final MongoTemplate mongoTemplate = new MongoTemplate(new MongoClient(new ServerAddress(mongoServer.bind())),
            "db");
        splitWhitelistDao = new SplitWhitelistDao(mongoTemplate);
    }

    @AfterEach
    void teardown() {
        mongoServer.shutdown();
    }

    @Test
    void removeExpired() {
        final Instant now = Instant.now();
        final SplitWhitelistEntity.Id expiredId = createId("expiredId");
        final SplitWhitelistEntity.Id validId = createId("validId");
        final Instant expiredTime = now.minus(1, ChronoUnit.MINUTES);
        final Instant validTime = now.plus(1, ChronoUnit.MINUTES);
        splitWhitelistDao.put(new SplitWhitelistEntity(expiredId, expiredTime,
            Collections.singletonList(new SplitWhitelistEntity.Element("expired", expiredTime, 1))));
        splitWhitelistDao.put(new SplitWhitelistEntity(validId, validTime,
            Collections.singletonList(new SplitWhitelistEntity.Element("valid", validTime, 1))));

        splitWhitelistDao.removeExpired(now);

        assertThat(splitWhitelistDao.get(expiredId)).isNull();
        assertThat(splitWhitelistDao.get(validId)).isNotNull();
    }

    @Test
    void updateModificationTime() {
        //mongo хранит время с точностью до секунд
        Instant originalModTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        final SplitWhitelistEntity originalEntity = new SplitWhitelistEntity(createId("someSplit"), originalModTime,
            Collections.singletonList(new SplitWhitelistEntity.Element("someValue", originalModTime, 1)));
        splitWhitelistDao.put(originalEntity);
        Instant newModTime = originalModTime.plus(1, ChronoUnit.MINUTES);
        splitWhitelistDao.updateModificationTime(originalEntity.getId(), newModTime);
        final SplitWhitelistEntity actualEntity = splitWhitelistDao.get(originalEntity.getId());
        final SplitWhitelistEntity expectedEntity = new SplitWhitelistEntity(
            originalEntity.getId(),
            newModTime,
            originalEntity.getWhitelist()
        );
        assertThat(actualEntity).usingRecursiveComparison().isEqualTo(expectedEntity);
    }

    SplitWhitelistEntity.Id createId(String splitExpression) {
        return new SplitWhitelistEntity.Id(
            null,
            splitExpression,
            null
        );
    }
}
