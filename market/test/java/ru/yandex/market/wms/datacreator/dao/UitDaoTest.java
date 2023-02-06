package ru.yandex.market.wms.datacreator.dao;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.datacreator.config.DataCreatorIntegrationTest;

public class UitDaoTest extends DataCreatorIntegrationTest {

    @Autowired
    private UitDao uitDao;

    @Test
    @DatabaseSetup(value = "/dao/uit/before.xml", connection = "wmwhse1Connection")
    void getUitByLocAndLotTest() {
        String uit = uitDao.getUitByLocAndLot("loc1", "1111");
        Assertions.assertEquals("3333", uit);
    }
}
