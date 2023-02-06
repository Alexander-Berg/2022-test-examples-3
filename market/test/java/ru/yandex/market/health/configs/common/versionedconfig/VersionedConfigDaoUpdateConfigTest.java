package ru.yandex.market.health.configs.common.versionedconfig;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionedConfigDaoUpdateConfigTest extends VersionedConfigDaoBaseTest {
    @Test
    public void shouldUpdate() {
        dao.createConfig(config("name1"));

        assertThat(dao.getConfig("name1"))
            .extracting(
                VersionedConfigEntity::getId,
                VersionedConfigEntity::getTitle,
                VersionedConfigEntity::getDescription
            )
            .contains("name1", null, null);

        dao.updateConfig("name1", "testProject", "title1", "description1");

        assertThat(dao.getConfig("name1"))
            .extracting(
                VersionedConfigEntity::getId,
                VersionedConfigEntity::getProjectId,
                VersionedConfigEntity::getTitle,
                VersionedConfigEntity::getDescription
            )
            .contains("name1", "testProject", "title1", "description1");

        dao.updateConfig("name1", "testProject", null, null);

        assertThat(dao.getConfig("name1"))
            .extracting(
                VersionedConfigEntity::getId,
                VersionedConfigEntity::getProjectId,
                VersionedConfigEntity::getTitle,
                VersionedConfigEntity::getDescription
            )
            .contains("name1", "testProject", null, null);
    }
}
