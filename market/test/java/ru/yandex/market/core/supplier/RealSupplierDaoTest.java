package ru.yandex.market.core.supplier;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.supplier.model.RealSupplierInfo;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class RealSupplierDaoTest extends FunctionalTest {

    @Autowired
    private RealSupplierDao realSupplierDao;

    @Test
    @DbUnitDataSet(
            before = "RealSupplierDaoTest.testSaveRealSupplier.before.csv",
            after = "RealSupplierDaoTest.testSaveRealSupplier.after.csv")
    void saveRealSupplier() {

        realSupplierDao.saveRealSupplier(new RealSupplierInfo.Builder()
                .setName("тест1")
                .setRealSupplierId("TST1")
                .setUpdatedAt(LocalDateTime.of(2018, 6, 5, 16, 0).atZone(ZoneId.systemDefault()).toInstant())
                .build());
        realSupplierDao.saveRealSupplier(new RealSupplierInfo.Builder()
                .setName("тест2")
                .setRealSupplierId("TST2")
                .setUpdatedAt(LocalDateTime.of(2018, 6, 5, 16, 15).atZone(ZoneId.systemDefault()).toInstant())
                .build());
    }

    @Test
    @DbUnitDataSet(
            before = "RealSupplierDaoTest.testSaveRealSupplier.before.csv"
    )
    void getRealSupplierByRsId() {

        var result = realSupplierDao.getByRsId("TST1");

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(result.get().getId(), 10L);
    }
}
