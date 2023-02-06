package ru.yandex.market.netting;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

public class NettingMigrationDaoTest extends FunctionalTest {

    @Autowired
    private NettingMigrationDao nettingMigrationDao;

    @Test
    @DbUnitDataSet(before = "csv/NettingMigrationDaoTest.testUpdate.before.csv",
            after = "csv/NettingMigrationDaoTest.testUpdate.after.csv")
    void testUpdate() {
        nettingMigrationDao.updateNettingStatus(List.of(4444L, 5555L));
    }
}
