package ru.yandex.market.health.configs.common;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigDao;
import ru.yandex.market.health.configs.common.versionedconfig.VersionedConfigEntity;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
public class ConfigDaoTestBase<T extends VersionedConfigEntity<U>, U extends VersionedConfigEntity.VersionEntity> {
    @Autowired
    protected VersionedConfigDao<T, U> dao;

    protected String searchingId = "test_id";
    protected String notSearchingId = "wrong_id";
    protected String searchingTitle = "test_title";
    protected String searchingTable = "searchingTable";

    protected void setUp(T searchingEntity, U searchingVersion, T notSearchingEntity, U notSearchingVersion) {
        dao.createConfig(searchingEntity);
        dao.createValidVersion(searchingVersion, "user42");
        dao.publishVersion(new VersionedConfigEntity.VersionEntity.Id(searchingId, 0L));
        dao.activateVersion(new VersionedConfigEntity.VersionEntity.Id(searchingId, 0L), null);

        dao.createConfig(notSearchingEntity);
        dao.createValidVersion(notSearchingVersion, "user42");
        dao.publishVersion(new VersionedConfigEntity.VersionEntity.Id(notSearchingId, 0L));
        dao.activateVersion(new VersionedConfigEntity.VersionEntity.Id(notSearchingId, 0L), null);
    }

    @Test
    public void findById() {
        testFindConfigs("idFilter", searchingId);
    }

    @Test
    public void findByTitle() {
        testFindConfigs("title", searchingTitle);
    }

    @Test
    public void findByTable() {
        testFindConfigs("table", searchingTable);
    }

    protected void testFindConfigs(String key, String value) {
        testFindConfigs(key, value, searchingId);
    }

    protected void testFindConfigs(String key, String value, String expectedId) {
        List<VersionedConfigDao.SearchCriteria> searchParams =
            Collections.singletonList(new VersionedConfigDao.SearchCriteria(key, value));
        Page<T> configs = dao.findConfigs(
            null, searchParams, 1, null, -1, null
        );
        assertThat(configs.getContent().size()).isEqualTo(1);
        assertThat(configs.getContent().get(0).getId()).isEqualTo(expectedId);
    }
}
