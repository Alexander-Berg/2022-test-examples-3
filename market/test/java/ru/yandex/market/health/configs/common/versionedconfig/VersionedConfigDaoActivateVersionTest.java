package ru.yandex.market.health.configs.common.versionedconfig;

import java.time.Instant;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionedConfigDaoActivateVersionTest extends VersionedConfigDaoBaseTest {
    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        dao.createConfig(config("name1"));
        dao.createValidVersion(version("name1", -1, 456), null);
        dao.createValidVersion(version("name1", -1, 567), null);
    }

    @Test
    public void shouldUpdateActivatedTime() {
        Instant testStartInstant = Instant.now();
        VersionedConfigEntity.VersionEntity.Id versionId = versionId("name1", 1);

        dao.activateVersion(versionId, null);

        assertThat(dao.getConfig("name1").getCurrentVersionActivatedTime())
            .isAfterOrEqualTo(testStartInstant);

        Mockito.verify(versionHistoryDao, Mockito.times(1)).recordActivation(versionId, null);
    }

    @Test
    public void shouldUpdateActivatedBy() {
        VersionedConfigEntity.VersionEntity.Id versionId = versionId("name1", 1);

        dao.activateVersion(versionId, "user42");

        assertThat(dao.getConfig("name1").getActivatedBy())
            .isEqualTo("user42");

        Mockito.verify(versionHistoryDao, Mockito.times(1)).recordActivation(versionId, null);
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldActivate_whenVersionIsLast() {
        VersionedConfigEntity.VersionEntity.Id versionId = versionId("name1", 1);
        dao.activateVersion(versionId, null);

        assertThat(dao.getConfig("name1").getCurrentVersion())
            .extracting(VersionedConfigEntity.VersionEntity::getId, TestConfigVersionEntity::getData)
            .containsExactly(versionId, 567);

        Mockito.verify(versionHistoryDao, Mockito.times(1)).recordActivation(versionId, null);
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldActivate_whenVersionIsNotLast() {
        VersionedConfigEntity.VersionEntity.Id versionId = versionId("name1", 0);
        dao.activateVersion(versionId, null);

        assertThat(dao.getConfig("name1").getCurrentVersion())
            .extracting(VersionedConfigEntity.VersionEntity::getId, TestConfigVersionEntity::getData)
            .containsExactly(versionId, 456);

        Mockito.verify(versionHistoryDao, Mockito.times(1)).recordActivation(versionId, null);
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldActivate_whenSomeOtherVersionWasAlreadyActive() {
        VersionedConfigEntity.VersionEntity.Id versionId1 = versionId("name1", 1);
        VersionedConfigEntity.VersionEntity.Id versionId0 = versionId("name1", 0);

        dao.activateVersion(versionId1, null);
        dao.activateVersion(versionId0, null);

        assertThat(dao.getConfig("name1").getCurrentVersion())
            .extracting(VersionedConfigEntity.VersionEntity::getId, TestConfigVersionEntity::getData)
            .containsExactly(versionId0, 456);

        Mockito.verify(versionHistoryDao, Mockito.times(1)).recordActivation(versionId1, null);
        Mockito.verify(versionHistoryDao, Mockito.times(1)).recordActivation(versionId0, null);
    }

    @Test
    public void shouldReturnActivated() {
        assertThat(dao.activateVersion(versionId("name1", 0), null).getCurrentVersion())
            .extracting(VersionedConfigEntity.VersionEntity::getId, TestConfigVersionEntity::getData)
            .containsExactly(versionId("name1", 0), 456);
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldDoNothing_WhenVersionIsAlreadyActive() {
        VersionedConfigEntity.VersionEntity.Id versionId = versionId("name1", 0);

        dao.activateVersion(versionId, null);
        dao.activateVersion(versionId, null);

        assertThat(dao.getConfig("name1").getCurrentVersion())
            .extracting(VersionedConfigEntity.VersionEntity::getId, TestConfigVersionEntity::getData)
            .containsExactly(versionId, 456);

        Mockito.verify(versionHistoryDao, Mockito.times(2)).recordActivation(versionId, null);
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldThrow_WhenConfigDoesNotExist() {
        mongoTemplate.remove(dao.getConfig("name1"));

        VersionedConfigEntity.VersionEntity.Id versionId = versionId("name1", 0);

        Assertions.assertThatThrownBy(() -> dao.activateVersion(versionId, null))
            .isInstanceOf(ConfigNotFoundException.class);

        Mockito.verify(versionHistoryDao, Mockito.times(0)).recordActivation(versionId, null);
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldThrow_WhenConfigVersionDoesNotExist() {
        VersionedConfigEntity.VersionEntity.Id versionId = versionId("name1", 2);

        Assertions.assertThatThrownBy(() -> dao.activateVersion(versionId, null))
            .isInstanceOf(ConfigVersionNotFoundException.class);

        Mockito.verify(versionHistoryDao, Mockito.times(0)).recordActivation(versionId, null);
    }
}
