package ru.yandex.market.core.feed.supplier.db;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.supplier.summary.SupplierMappingSummaryInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест вставки/чтения сводок по маппингам для {@link SupplierSummaryDao}.
 */
class SupplierSummaryDaoTest extends FunctionalTest {

    @Autowired
    private SupplierSummaryDao supplierSummaryDao;


    @DbUnitDataSet(before = "SupplierSummaryDaoTest.before.csv")
    @Test
    void testGetSupplierMappingSummaryInfo() {
        SupplierMappingSummaryInfo summary = supplierSummaryDao.getSupplierMappingSummaryInfo(102L);
        Assertions.assertEquals(new SupplierMappingSummaryInfo(102L, 1, 2, 3), summary);
    }

    @DbUnitDataSet(before = "SupplierSummaryDaoTest.before.csv"
            , after = "SupplierSummaryDaoTest.insert.after.csv")
    @Test
    void testInsertSupplierMappingSummaryInfo() {
        supplierSummaryDao.upsertSupplierMappingSummary(
                Collections.singletonList(new SupplierMappingSummaryInfo(101L, 10, 20, 30)));
    }

    @DbUnitDataSet( before = "SupplierSummaryDaoTest.before.csv",
            after = "SupplierSummaryDaoTest.clean.after.csv")
    @Test
    void testDeleteSupplierMappingSummaryInfo() {
        supplierSummaryDao.cleanSummaryInfoForPartner(102L);
    }

    @DbUnitDataSet(before = "SupplierSummaryDaoTest.before.csv",
            after = "SupplierSummaryDaoTest.update.after.csv")
    @Test
    void testUpdateSupplierMappingSummaryInfo() {
        supplierSummaryDao.upsertSupplierMappingSummary(
                Collections.singletonList(new SupplierMappingSummaryInfo(102L, 10, 20, 30)));
    }

    @DbUnitDataSet(before = "SupplierSummaryDaoTest.before.csv")
    @Test
    void testEmptyGetMappingSummaryInfo() {
        SupplierMappingSummaryInfo summary = supplierSummaryDao.getSupplierMappingSummaryInfo(404L);
        Assertions.assertEquals(new SupplierMappingSummaryInfo(404L, 0, 0, 0), summary);
    }

    @DbUnitDataSet(before = "SupplierSummaryDaoTest.before.csv",
            after = "SupplierSummaryDaoTest.moderation.after.csv")
    @Test
    void testGetSupplierWithModerationCompleted() {
        Assertions.assertEquals(2, supplierSummaryDao.updateSuppliersWithModerationCompleted());
    }

    @DbUnitDataSet(before = "SupplierSummaryDaoTest.updateDatacampPriceFlags.before.csv",
            after = "SupplierSummaryDaoTest.updateDatacampPriceFlags.after.csv")
    @Test
    void testUpdateDatacampPriceFlags() {
        supplierSummaryDao.setHasDatacampPrices(List.of(
                102L,
                202L,
                204L
        ));
    }

    @DbUnitDataSet(before = "SupplierSummaryDaoTest.updateDatacampStockFlags.before.csv",
            after = "SupplierSummaryDaoTest.updateDatacampStockFlags.after.csv")
    @Test
    void testUpdateDatacampStockFlags() {
        supplierSummaryDao.setHasDatacampStocks(List.of(
                201L,
                203L
        ));
    }

    @DbUnitDataSet(before = "SupplierSummaryDaoTest.updateAllDatacampFlags.before.csv",
            after = "SupplierSummaryDaoTest.updateAllDatacampFlags.after.csv")
    @Test
    void testUpdateAllDatacampFlags() {
        supplierSummaryDao.setAllDatacampFlags(List.of(
                102L,
                202L,
                204L
        ));
    }

    @DbUnitDataSet(before = "SupplierSummaryDaoTest.getPartnersWithoutPricesOrStocks.before.csv")
    @Test
    void testGetPartnersWithoutPriceOrStock() {
        List<Long> suppliers = supplierSummaryDao.getPartnersWithoutPrice();
        assertThat(suppliers).containsExactlyInAnyOrder(
                102L,
                201L,
                203L
        );
    }

    @DbUnitDataSet(before = "SupplierSummaryDaoTest.testUpdateAllDatacampFlagsPartner.before.csv",
            after = "SupplierSummaryDaoTest.testUpdateAllDatacampFlagsPartner.after.csv")
    @Test
    void testUpdateAllDatacampFlagsPartner() {
        supplierSummaryDao.updateAllDatacampFlagsForPartner(
                102L, true, true, true);
        supplierSummaryDao.updateAllDatacampFlagsForPartner(
                202L, true, false, false);
        supplierSummaryDao.updateAllDatacampFlagsForPartner(
                204L, false, true, false);
    }

}
