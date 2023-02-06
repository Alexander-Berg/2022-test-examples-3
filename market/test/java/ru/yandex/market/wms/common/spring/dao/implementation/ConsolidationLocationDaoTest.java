package ru.yandex.market.wms.common.spring.dao.implementation;

import java.util.Arrays;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.ConsolidationLocation;
import ru.yandex.market.wms.common.spring.enums.ConsolidationLocationType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsolidationLocationDaoTest extends IntegrationTest {

    @Autowired
    private ConsolidationLocationDao consolidationLocationDao;

    @Test
    @DatabaseSetup("/db/dao/cons-loc/before-1.xml")
    void testSelectNoBuilding() {
        var locations = consolidationLocationDao.getAllConsolidationLocations();
        assertEquals(1, locations.size());
        assertEquals(3, locations.values().stream().flatMap(List::stream).toList().size());

        locations = consolidationLocationDao.getAllConsolidationLocations(null);
        assertEquals(1, locations.size());
        assertEquals(3, locations.values().stream().flatMap(List::stream).toList().size());

        locations = consolidationLocationDao.getAllConsolidationLocations(1);
        var locs = locations.values().iterator().next();
        assertEquals(1, locations.size());
        assertEquals(1, locs.size());
        assertEquals("NS-CONS-1", locs.get(0));

        locations = consolidationLocationDao.getAllConsolidationLocations(2);
        locs = locations.values().iterator().next();
        assertEquals(1, locations.size());
        assertEquals(1, locs.size());
        assertEquals("NS-CONS-2", locs.get(0));
    }

    @Test
    @DatabaseSetup("/db/dao/cons-loc/before-2.xml")
    @ExpectedDatabase(value = "/db/dao/cons-loc/after-1.xml", assertionMode = NON_STRICT)
    void insertOne() {
        consolidationLocationDao.insert(ConsolidationLocation.builder()
                .loc("CONS-3")
                .addWho("test")
                .type(ConsolidationLocationType.OVERSIZE)
                .build());
    }

    @Test
    @DatabaseSetup("/db/dao/cons-loc/before-2.xml")
    @ExpectedDatabase(value = "/db/dao/cons-loc/after-2.xml", assertionMode = NON_STRICT)
    void insertList() {
        consolidationLocationDao.insert(Arrays.asList(
                ConsolidationLocation.builder()
                        .loc("CONS-3")
                        .addWho("test")
                        .type(ConsolidationLocationType.OVERSIZE)
                        .build(),
                ConsolidationLocation.builder()
                        .loc("CONS-3")
                        .addWho("test")
                        .type(ConsolidationLocationType.SINGLES)
                        .build(),
                ConsolidationLocation.builder()
                        .loc("CONS-4")
                        .addWho("test")
                        .type(ConsolidationLocationType.OVERSIZE)
                        .build()));
    }

    @Test
    @DatabaseSetup("/db/dao/cons-loc/before-2.xml")
    @ExpectedDatabase(value = "/db/dao/cons-loc/after-3.xml", assertionMode = NON_STRICT)
    void deleteOne() {
        consolidationLocationDao.delete(ConsolidationLocation.builder()
                .loc("CONS-2")
                .type(ConsolidationLocationType.OVERSIZE)
                .build());
    }

    @Test
    @DatabaseSetup("/db/dao/cons-loc/before-2.xml")
    @ExpectedDatabase(value = "/db/dao/cons-loc/after-4.xml", assertionMode = NON_STRICT)
    void deleteList() {
        consolidationLocationDao.delete(Arrays.asList(
                ConsolidationLocation.builder()
                        .loc("CONS-2")
                        .type(ConsolidationLocationType.OVERSIZE)
                        .build(),
                ConsolidationLocation.builder()
                        .loc("CONS-0")
                        .type(ConsolidationLocationType.SINGLES)
                        .build()));
    }
}
