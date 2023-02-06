package ru.yandex.market.health.configs.common.versionedconfig;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import ru.yandex.market.health.configs.common.config_history.VersionHistoryDao;
import ru.yandex.market.health.configs.common.project.ProjectDao;

public abstract class VersionedConfigDaoBaseTest {
    protected MongoTemplate mongoTemplate;
    protected VersionedConfigDao<TestConfigEntity, TestConfigVersionEntity> dao;
    protected VersionHistoryDao versionHistoryDao = Mockito.mock(VersionHistoryDao.class);

    protected static TestConfigEntity config(String name) {
        return new TestConfigEntity(name, null, null, null);
    }

    protected static TestConfigEntity config(String name, Instant createdTime) {
        return new TestConfigEntity(name, createdTime, null, null);
    }

    protected static TestConfigEntity config(String name, TestConfigVersionEntity currentVersion) {
        return new TestConfigEntity(name, null, null, currentVersion);
    }

    protected static TestConfigEntity config(
        String id,
        Instant createdTime,
        Instant currentVersionActivatedTime,
        TestConfigVersionEntity currentVersion
    ) {
        return new TestConfigEntity(id, createdTime, currentVersionActivatedTime, currentVersion);
    }

    protected static TestConfigVersionEntity version(String name, long version, int data) {
        return version(name, version, VersionedConfigSource.UI, data);
    }

    protected static TestConfigVersionEntity version(
        String name,
        long version,
        VersionedConfigSource source,
        int data
    ) {
        return new TestConfigVersionEntity(versionId(name, version), source, data);
    }

    protected static VersionedConfigEntity.VersionEntity.Id versionId(String name, long version) {
        return new VersionedConfigEntity.VersionEntity.Id(name, version);
    }

    @BeforeEach
    public void setUp() {
        mongoTemplate = Mockito.spy(
            new MongoTemplate(
                new MongoClient(
                    new ServerAddress(
                        new MongoServer(new MemoryBackend()).bind())
                ),
                "db"
            )
        );
        dao = new VersionedConfigDao(
            mongoTemplate,
            versionHistoryDao,
            Mockito.mock(LocalValidatorFactoryBean.class),
            TestConfigEntity.class,
            TestConfigVersionEntity.class,
            Mockito.mock(ProjectDao.class)
        ) {
            @Override
            protected Criteria getKeyValueCriteria(List searchParams) {
                return null;
            }

            @Override
            protected VersionedConfigEntity.VersionEntity getEmptyVersion(VersionedConfigEntity.VersionEntity.Id id) {
                return new TestConfigVersionEntity(id, VersionedConfigSource.UI, -1);
            }
        };
    }

    protected static class TestConfigEntity extends VersionedConfigEntity<TestConfigVersionEntity> {
        public TestConfigEntity(
            String id,
            Instant createdTime,
            Instant currentVersionActivatedTime,
            TestConfigVersionEntity currentVersion
        ) {
            super(id, null, null, null, createdTime, currentVersionActivatedTime, currentVersion, null);
        }
    }

    protected static class TestConfigVersionEntity extends VersionedConfigEntity.VersionEntity {
        final int data;

        @PersistenceConstructor
        TestConfigVersionEntity(Id id, VersionedConfigSource configSource, int data) {
            super(id, configSource, null, null);
            this.data = data;
        }

        public int getData() {
            return data;
        }

        @Override
        public int hashCode() {
            return data;
        }

        @Override
        public boolean equals(Object o) {
            TestConfigVersionEntity that = (TestConfigVersionEntity) o;
            return Objects.equals(getId(), that.getId())
                && data == that.data;
        }
    }
}
