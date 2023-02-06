package ru.yandex.market.core.environment;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.model.ResourceFileDescriptor;
import ru.yandex.market.common.mds.s3.client.model.ResourceHistoryStrategy;
import ru.yandex.market.common.mds.s3.spring.db.ResourceConfigurationDao;
import ru.yandex.market.common.mds.s3.spring.db.impl.ResourceConfigurationDaoImpl;
import ru.yandex.market.common.mds.s3.spring.model.ResourceConfigurationStatus;
import ru.yandex.market.core.FunctionalTest;

class ResourceConfigurationDaoImplTest extends FunctionalTest {
    @Autowired
    JdbcTemplate jdbcTemplate;

    ResourceConfigurationDao dao;

    @BeforeEach
    void setUp() {
        dao = new ResourceConfigurationDaoImpl(jdbcTemplate, "mbi_core.mds_s3_resource_config");
    }

    @Test
    void smokeTest() {
        var config = ResourceConfiguration.create(
                "bucket",
                ResourceHistoryStrategy.LAST_ONLY,
                ResourceFileDescriptor.create("descriptor"),
                null
        );
        dao.merge(
                "module",
                config
        );
        dao.getByStatus(ResourceConfigurationStatus.EXISTS);
        dao.setStatusFor(ResourceConfigurationStatus.EXISTS, List.of(config));
        dao.setStatusNotFor("module", ResourceConfigurationStatus.EXISTS, List.of(config));
        dao.delete(List.of(config));
    }
}
