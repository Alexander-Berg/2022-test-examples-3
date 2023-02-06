package ru.yandex.market.health.configs.common.versionedconfig;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionedConfigDaoCreateVersionTest extends VersionedConfigDaoBaseTest {
    @Test
    public void shouldInsertWithSequentialVersionNumbers() {
        dao.createConfig(config("name1"));
        dao.createConfig(config("name2"));
        dao.createValidVersion(version("name1", -1, 456), null);
        dao.createValidVersion(version("name2", -1, 567), null);
        dao.createValidVersion(version("name2", -1, 678), null);
        dao.createValidVersion(version("name2", -1, 789), null);
        assertThat(dao.getConfigVersions("name1"))
            .extracting(VersionedConfigEntity.VersionEntity::getId, TestConfigVersionEntity::getData)
            .containsExactly(
                Assertions.tuple(versionId("name1", 0), 456)
            );
        assertThat(dao.getConfigVersions("name2"))
            .extracting(VersionedConfigEntity.VersionEntity::getId, TestConfigVersionEntity::getData)
            .containsExactly(
                Assertions.tuple(versionId("name2", 0), 567),
                Assertions.tuple(versionId("name2", 1), 678),
                Assertions.tuple(versionId("name2", 2), 789)
            );
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldThrow_WhenConfigDoesNotExist() {
        Assertions.assertThatThrownBy(() -> dao.createValidVersion(version("name", -1, 678), null))
            .isInstanceOf(ConfigNotFoundException.class)
            .hasMessageContaining("name");
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldRetryAndReturnNewId_WhenVersionAlreadyExists() {
        dao.createConfig(config("name1"));

        // Мокаем mongoTemplate так, чтобы при попытке вставить версию всё выглядело так, как будто эту версию с таким
        // id уже кто-то вставил. Метод createConfig должен вычислять новый id и ретраить в таких случаях.
        Mockito.doAnswer(new InsertTwiceAnswer(3))
            .when(mongoTemplate)
            .insert(Matchers.any(TestConfigVersionEntity.class));

        TestConfigVersionEntity version = version("name1", -1, 678);
        VersionedConfigEntity.VersionEntity.Id id = dao.createValidVersion(version, null);
        assertThat(id.getVersionNumber()).isEqualTo(3);
        assertThat(version.getId().getVersionNumber()).isEqualTo(3);
        assertThat(dao.getOptionalConfigVersion(versionId("name1", 3))).isPresent();
        assertThat(dao.getConfigVersions("name1")).hasSize(4);
    }

    @Test
    public void shouldGiveUpAfterTooManyRetries() {
        dao.createConfig(config("name1"));

        // Мокаем mongoTemplate так, чтобы при попытке вставить версию всё выглядело так, как будто эту версию с таким
        // id уже кто-то вставил. Метод createConfig должен вычислять новый id и ретраить в таких случаях.
        Mockito.doAnswer(new InsertTwiceAnswer(1000))
            .when(mongoTemplate)
            .insert(Matchers.any(TestConfigVersionEntity.class));

        Assertions.assertThatThrownBy(() -> dao.createValidVersion(version("name1", -1, 678), null))
            .isInstanceOf(RuntimeException.class);
    }

    private static class InsertTwiceAnswer implements Answer<Object> {
        private int doubleInsertsLeft;

        InsertTwiceAnswer(int doubleInsertsLeft) {
            this.doubleInsertsLeft = doubleInsertsLeft;
        }

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            if (doubleInsertsLeft > 0) {
                doubleInsertsLeft--;
                invocation.callRealMethod();
            }
            return invocation.callRealMethod();
        }
    }
}
