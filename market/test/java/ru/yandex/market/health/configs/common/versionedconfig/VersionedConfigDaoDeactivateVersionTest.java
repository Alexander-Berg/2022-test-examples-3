package ru.yandex.market.health.configs.common.versionedconfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

public class VersionedConfigDaoDeactivateVersionTest extends VersionedConfigDaoBaseTest {
    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        dao.createConfig(config("name1"));
        dao.createValidVersion(version("name1", -1, 456), null);
        dao.createValidVersion(version("name1", -1, 567), null);
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldDoNothing_whenThereIsNoCurrentVersion() {
        dao.deactivate("name1");

        assertThat(dao.getConfig("name1").getCurrentVersion()).isNull();

        Mockito.verify(versionHistoryDao, Mockito.times(0)).recordDeactivation(any(), any());
    }

    @Test
    @SuppressWarnings("MethodName")
    public void shouldDeactivate_whenThereIsCurrentVersion() {
        VersionedConfigEntity.VersionEntity.Id versionId = versionId("name1", 1);
        dao.activateVersion(versionId, null);

        dao.deactivate("name1");

        assertThat(dao.getConfig("name1").getCurrentVersion()).isNull();
        assertThat(dao.getOptionalConfigVersion(versionId)).isNotEmpty();

        Mockito.verify(versionHistoryDao, Mockito.times(1)).recordDeactivation(versionId, null);
    }
}
