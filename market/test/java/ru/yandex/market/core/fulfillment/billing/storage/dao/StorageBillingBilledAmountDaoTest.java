package ru.yandex.market.core.fulfillment.billing.storage.dao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.fulfillment.billing.storage.model.FinanceReportStorageBilledAmount;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link StorageBillingBilledAmountDao}
 */
class StorageBillingBilledAmountDaoTest extends FunctionalTest {
    private static final long SUPPLIER_ID = 100L;
    private static final LocalDate DATE_2019_01_12 = LocalDate.of(2019, Month.JANUARY, 12);
    private static final LocalDate DATE_2019_01_13 = LocalDate.of(2019, Month.JANUARY, 13);
    private static final LocalDate DATE_2019_01_14 = LocalDate.of(2019, Month.JANUARY, 14);
    private static final LocalDateTime TIME_2019_01_15 = LocalDateTime.of(2019, Month.JANUARY, 15, 0, 0);

    @Autowired
    StorageBillingBilledAmountDao storageBillingBilledAmountDao;

    @Test
    @DbUnitDataSet(before = "StorageBillingBilledAmountDaoTest.before.csv")
    void test_getFinanceReport() {
        List<FinanceReportStorageBilledAmount> actualItems = storageBillingBilledAmountDao
                .getFinanceReport(SUPPLIER_ID, DATE_2019_01_12, DATE_2019_01_14);

        ReflectionAssert.assertReflectionEquals(getExpectedFinanceReportItems(), actualItems);
    }

    @Test
    @DbUnitDataSet(before = "StorageBillingBilledAmountDaoTest.before.csv",
            after = "StorageBillingBilledAmountDaoTest.delete.one.after.csv")
    void test_deleteBilledOne() {
        storageBillingBilledAmountDao.updateStorageBilling(SUPPLIER_ID, DATE_2019_01_13);
    }

    @Test
    @DbUnitDataSet(before = "StorageBillingBilledAmountDaoTest.before.csv",
            after = "StorageBillingBilledAmountDaoTest.delete.all.after.csv")
    void test_deleteBilledAll() {
        storageBillingBilledAmountDao.updateStorageBilling(DATE_2019_01_13);
    }

    @Test
    @DbUnitDataSet(before = "StorageBillingBilledAmountDaoTest.before.csv")
    void test_getModifiedDates() {
        List<LocalDate> dates = storageBillingBilledAmountDao.getModifiedDates(TIME_2019_01_15);
        assertThat(dates).hasSize(2);
        assertThat(dates.get(0)).isEqualTo(DATE_2019_01_13);
        assertThat(dates.get(1)).isEqualTo(DATE_2019_01_14);
    }

    private List<FinanceReportStorageBilledAmount> getExpectedFinanceReportItems() {
        return Arrays.asList(
                new FinanceReportStorageBilledAmount.Builder()
                        .setShopSku("sku_1")
                        .setBillingDate(DATE_2019_01_13)
                        .setBilledCount(7)
                        .setTariff(new BigDecimal("0.4"))
                        .setAmount(new BigDecimal("2.8"))
                        .build(),
                new FinanceReportStorageBilledAmount.Builder()
                        .setShopSku("sku_1")
                        .setBillingDate(DATE_2019_01_12)
                        .setBilledCount(2)
                        .setTariff(new BigDecimal("0.4"))
                        .setAmount(new BigDecimal("0.8"))
                        .build()
        );
    }
}
