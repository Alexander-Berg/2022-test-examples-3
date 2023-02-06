package ru.yandex.market.wms.packing.dao;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.packing.pojo.LocSorter;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LocSorterDaoTest extends IntegrationTest {

    @Autowired
    private LocSorterDao locSorterDao;

    @Test
    @DatabaseSetup("/db/dao/loc-sorter/db_setup.xml")
    public void isAdjacentSorter() {
        LocSorter locSorter = locSorterDao.getAdjacentSorterLoc("STAGE01").orElseThrow(IllegalArgumentException::new);

        assertAll(
                () -> assertEquals(locSorter.getLoc(), "STAGE02"),
                () -> assertEquals(locSorter.getPutawayzone(), "RACK2")
        );
    }

    @Test
    @DatabaseSetup("/db/dao/loc-sorter/db_setup.xml")
    public void notAdjacentSorter() {
        assertTrue(locSorterDao.getAdjacentSorterLoc("STAGE03").isEmpty());
    }
}
