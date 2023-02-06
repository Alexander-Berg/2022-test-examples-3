package ru.yandex.market.health.configs.common.versionedconfig;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionConfigDaoConfigRetrievalTest extends VersionedConfigDaoBaseTest {
    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        dao.createConfig(config("name1"));
        dao.createConfig(config("name2"));
        dao.createConfig(config("name3"));
        dao.createValidVersion(version("name1", -1, 456), null);
        dao.createValidVersion(version("name2", -1, 567), null);
        dao.createValidVersion(version("name2", -1, 678), null);
        dao.createValidVersion(version("name2", -1, 789), null);
        dao.activateVersion(versionId("name2", 0), "user42");
    }

    @Test
    public void getAllConfigs() {
        assertThat(dao.getAllConfigs())
            .extracting(VersionedConfigEntity::getId)
            .containsExactlyInAnyOrder("name1", "name2", "name3");
    }

    @Test
    public void getActiveConfigs() {
        assertThat(dao.getActiveConfigs())
            .extracting(VersionedConfigEntity::getId)
            .containsExactlyInAnyOrder("name2");
    }

    @Test
    public void getConfig() {
        assertThat(dao.getConfig("name1").getId()).isEqualTo("name1");

        Assertions.assertThatThrownBy(() -> dao.getConfig("does_not_exist"))
            .isInstanceOf(ConfigNotFoundException.class)
            .hasMessageContaining("does_not_exist");
    }

    @Test
    public void getOptionalConfig() {
        assertThat(dao.getOptionalConfig("name1"))
            .map(VersionedConfigEntity::getId)
            .contains("name1");

        assertThat(dao.getOptionalConfig("does_not_exist"))
            .isEmpty();
    }

    @Test
    public void getConfigVersions() {
        assertThat(dao.getConfigVersions("name2"))
            .extracting(VersionedConfigEntity.VersionEntity::getId, TestConfigVersionEntity::getData)
            .containsExactlyInAnyOrder(
                Assertions.tuple(versionId("name2", 0), 567),
                Assertions.tuple(versionId("name2", 1), 678),
                Assertions.tuple(versionId("name2", 2), 789)
            );

        assertThat(dao.getConfigVersions("name3"))
            .isEmpty();

        assertThat(dao.getConfigVersions("does_not_exist"))
            .isEmpty();
    }

    @Test
    public void getConfigVersion() {
        assertThat(dao.getConfigVersion(versionId("name1", 0)))
            .extracting(VersionedConfigEntity.VersionEntity::getId, TestConfigVersionEntity::getData)
            .containsExactlyInAnyOrder(versionId("name1", 0), 456);

        Assertions.assertThatThrownBy(() -> dao.getConfigVersion(versionId("does_not_exist", 0)))
            .isInstanceOf(ConfigVersionNotFoundException.class)
            .hasMessageContaining("does_not_exist")
            .hasMessageContaining("0");

        Assertions.assertThatThrownBy(() -> dao.getConfigVersion(versionId("name1", 1)))
            .isInstanceOf(ConfigVersionNotFoundException.class)
            .hasMessageContaining("name1")
            .hasMessageContaining("1");
    }

    @Test
    public void getOptionalConfigVersion() {
        assertThat(dao.getOptionalConfigVersion(versionId("name1", 0)))
            .get()
            .extracting(VersionedConfigEntity.VersionEntity::getId, TestConfigVersionEntity::getData)
            .containsExactlyInAnyOrder(versionId("name1", 0), 456);

        assertThat(dao.getOptionalConfigVersion(versionId("does_not_exist", 0)))
            .isEmpty();

        assertThat(dao.getOptionalConfigVersion(versionId("name1", 1)))
            .isEmpty();
    }
}
