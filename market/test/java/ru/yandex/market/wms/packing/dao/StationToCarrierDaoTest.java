package ru.yandex.market.wms.packing.dao;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationToCarrierDaoTest extends IntegrationTest {

    @Autowired
    private StationToCarrierDao stationToCarrierDao;

    @Test
    @DatabaseSetup("/db/dao/station-to-carrier/db.xml")
    void isDroppingEnabledForAnyStation() {
        assertTrue(stationToCarrierDao.isDroppingEnabledForAnyStation(Set.of("S1")));
        assertTrue(stationToCarrierDao.isDroppingEnabledForAnyStation(Set.of("S1", "S2", "S96")));
        assertFalse(stationToCarrierDao.isDroppingEnabledForAnyStation(Set.of("S2", "S96")));
        assertFalse(stationToCarrierDao.isDroppingEnabledForAnyStation(Set.of("S96")));
    }
}
