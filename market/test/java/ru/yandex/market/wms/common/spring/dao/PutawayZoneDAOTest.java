package ru.yandex.market.wms.common.spring.dao;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.PutawayZone;
import ru.yandex.market.wms.common.spring.dao.implementation.PutawayZoneDAO;
import ru.yandex.market.wms.common.spring.enums.PutawayZoneType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static ru.yandex.market.wms.common.model.enums.DatabaseSchema.WMWHSE1;

public class PutawayZoneDAOTest extends IntegrationTest {

    @Autowired
    private PutawayZoneDAO putawayZoneDao;

    @Test
    @DatabaseSetup(value = "/db/dao/putaway-zone/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/putaway-zone/after-create.xml", assertionMode = NON_STRICT_UNORDERED,
            connection = "wmwhseConnection")
    public void createNew() {
        PutawayZone zone = PutawayZone.builder()
                .putawayZone("Floor 1")
                .addwho("AD_TEST2")
                .editwho("AD_TEST2")
                .type(PutawayZoneType.BBXD_SORTER)
                .build();

        putawayZoneDao.create(zone, WMWHSE1);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/putaway-zone/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/putaway-zone/before.xml", assertionMode = NON_STRICT,
            connection = "wmwhseConnection")
    public void isExists() {
        PutawayZone existing = PutawayZone.builder()
                .putawayZone("SECTOR A")
                .build();

        PutawayZone existingBbxd = PutawayZone.builder()
                .putawayZone("SECTOR B")
                .type(PutawayZoneType.BBXD_SORTER)
                .build();

        PutawayZone nonExisting = PutawayZone.builder()
                .putawayZone("UNKNONW")
                .build();

        assertions.assertThat(putawayZoneDao.isExists(existing, WMWHSE1)).isTrue();
        assertions.assertThat(putawayZoneDao.isExists(existingBbxd, WMWHSE1)).isTrue();
        assertions.assertThat(putawayZoneDao.isExists(nonExisting, WMWHSE1)).isFalse();
    }
}
