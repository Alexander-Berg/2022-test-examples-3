package ru.yandex.market.core.warehouse.dao;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

class WarehouseLinkDaoTest extends FunctionalTest {
    @Autowired
    private WarehouseLinkDao warehouseLinkDao;

    @Test
    @DbUnitDataSet(before = "warehouseLink.before.csv")
    void testShouldReturnDonorWhId() {
        final Long donorWarehouseId = warehouseLinkDao.getDonorWarehouseId(2);
        Assertions.assertEquals(1L, donorWarehouseId);
    }
}
