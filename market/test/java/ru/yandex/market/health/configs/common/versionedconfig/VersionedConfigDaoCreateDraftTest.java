package ru.yandex.market.health.configs.common.versionedconfig;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionedConfigDaoCreateDraftTest extends VersionedConfigDaoBaseTest {
    @Test
    public void shouldInsertWithSequentialVersionNumbers() {
        dao.createConfig(config("name1"));
        dao.createConfig(config("name2"));
        dao.createDraft("name1", null);
        dao.createDraft("name2", null);
        dao.createDraft("name2", null);
        dao.createDraft("name2", null);
        assertThat(dao.getConfigVersions("name1"))
            .extracting(VersionedConfigEntity.VersionEntity::getId, TestConfigVersionEntity::getData)
            .containsExactly(
                Assertions.tuple(versionId("name1", 0), -1)
            );
        assertThat(dao.getConfigVersions("name2"))
            .extracting(VersionedConfigEntity.VersionEntity::getId, TestConfigVersionEntity::getData)
            .containsExactly(
                Assertions.tuple(versionId("name2", 0), -1),
                Assertions.tuple(versionId("name2", 1), -1),
                Assertions.tuple(versionId("name2", 2), -1)
            );
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldThrow_WhenConfigDoesNotExist() {
        Assertions.assertThatThrownBy(() -> dao.createDraft("name", null))
            .isInstanceOf(ConfigNotFoundException.class)
            .hasMessageContaining("name");
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldRetryAndReturnNewId_WhenVersionAlreadyExists() {
        dao.createConfig(config("name1"));

        // Мокаем mongoTemplate так, чтобы при попытке вставить версию всё выглядело так, как будто эту версию с таким
        // id уже кто-то вставил. Метод createDraft должен вычислять новый id и ретраить в таких случаях.
        Mockito.doAnswer(new InsertTwiceAnswer(3))
            .when(mongoTemplate)
            .insert(Matchers.any(TestConfigVersionEntity.class));

        VersionedConfigEntity.VersionEntity.Id id = dao.createDraft("name1", null);
        assertThat(id.getVersionNumber()).isEqualTo(3);
        assertThat(dao.getOptionalConfigVersion(versionId("name1", 3))).isPresent();
        assertThat(dao.getConfigVersions("name1")).hasSize(4);
    }

    @Test
    public void shouldGiveUpAfterTooManyRetries() {
        dao.createConfig(config("name1"));

        // Мокаем mongoTemplate так, чтобы при попытке вставить версию всё выглядело так, как будто эту версию с таким
        // id уже кто-то вставил. Метод createDraft должен вычислять новый id и ретраить в таких случаях.
        Mockito.doAnswer(new InsertTwiceAnswer(1000))
            .when(mongoTemplate)
            .insert(Matchers.any(TestConfigVersionEntity.class));

        Assertions.assertThatThrownBy(() -> dao.createDraft("name1", null))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void shouldCopyPreviousVersion() {
        dao.createConfig(config("name1"));
        dao.createValidVersion(version("name1", -1, 456), null);
        dao.createDraft("name1", null);

        assertThat(dao.getConfigVersions("name1"))
            .extracting(VersionedConfigEntity.VersionEntity::getId, TestConfigVersionEntity::getData)
            .containsExactly(
                Assertions.tuple(versionId("name1", 0), 456),
                Assertions.tuple(versionId("name1", 1), 456)
            );
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
