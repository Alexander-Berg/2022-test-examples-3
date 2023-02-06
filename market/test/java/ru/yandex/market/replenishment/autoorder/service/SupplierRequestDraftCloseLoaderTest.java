package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDateTime;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
public class SupplierRequestDraftCloseLoaderTest extends FunctionalTest {

    @Autowired
    SupplierRequestDraftCloseLoader supplierRequestDraftCloseLoader;

    @Test
    @DbUnitDataSet(before = "SupplierRequestDraftCloseLoaderTest.before.csv",
            after = "SupplierRequestDraftCloseLoaderTest.after.csv")
    public void testDraftsClose() {
        LocalDateTime mockedDateTime = LocalDateTime.of(2021, 5, 12, 10, 47);
        setTestTime(mockedDateTime);
        supplierRequestDraftCloseLoader.load();
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestDraftCloseLoaderTest_withSimilarDrafts.before.csv",
            after = "SupplierRequestDraftCloseLoaderTest_withSimilarDrafts.after.csv")
    public void testDraftsClose_withSimilarDrafts() {
        LocalDateTime mockedDateTime = LocalDateTime.of(2021, 5, 12, 10, 47);
        setTestTime(mockedDateTime);
        supplierRequestDraftCloseLoader.load();
    }
}
