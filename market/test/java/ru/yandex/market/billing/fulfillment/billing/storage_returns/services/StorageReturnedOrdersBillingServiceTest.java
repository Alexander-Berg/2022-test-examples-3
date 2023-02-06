package ru.yandex.market.billing.fulfillment.billing.storage_returns.services;

import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.calendar.DatePeriod;

/**
 * Тесты для {@link StorageReturnedOrdersBillingService}
 */
@DbUnitDataSet
public class StorageReturnedOrdersBillingServiceTest extends FunctionalTest {

    private static final LocalDate BILLING_DATE = LocalDate.of(2020, 8, 10);

    @Autowired
    private StorageReturnedOrdersBillingService storageReturnedOrdersBillingService;

    @Test
    @DbUnitDataSet(
            before = "StorageReturnedOrdersBillingServiceTest.before.csv",
            after = "StorageReturnedOrdersBillingServiceTest.after.csv"
    )
    void testBill() {
        storageReturnedOrdersBillingService.process(BILLING_DATE);
    }

    @Test
    @DbUnitDataSet(
            before = "StorageReturnedOrdersBillingServiceTest.before.csv",
            after = "StorageReturnedOrdersBillingServiceTest.testBillNextMonth.after.csv"
    )
    void testBillNextMonth() {
        storageReturnedOrdersBillingService.process(LocalDate.of(2020, 9, 10));
    }

    @Test
    @DbUnitDataSet(
            before = "StorageReturnedOrdersBillingServiceTest.testIgnoredIds.before.csv",
            after = "StorageReturnedOrdersBillingServiceTest.testIgnoredIds.after.csv"
    )
    void testIgnoredIds() {
        storageReturnedOrdersBillingService.process(BILLING_DATE);
    }

    @Test
    @DbUnitDataSet(
            before = "StorageReturnedOrdersBillingServiceTest.before.csv",
            after = "StorageReturnedOrdersBillingServiceTest.testBillNovember.after.csv"
    )
    void testBillNovember() {
        storageReturnedOrdersBillingService.process(LocalDate.of(2020, 9, 10));
        storageReturnedOrdersBillingService.process(LocalDate.of(2020, 11, 10));
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "StorageReturnedOrdersBillingServiceTest.before.csv",
                    "StorageReturnedOrdersBillingServiceTest.testBillSupplierWithPromo.before.csv"
            },
            after = "StorageReturnedOrdersBillingServiceTest.testBillSupplierWithPromo.after.csv"
    )
    void testBillSupplierWithPromoTariff() {
        storageReturnedOrdersBillingService.process(BILLING_DATE);
    }

    @Test
    @DbUnitDataSet(
            before = "StorageReturnedOrdersBillingServiceTest.before.csv",
            after = "StorageReturnedOrdersBillingServiceTest.testBillDecember.after.csv"

    )
    void testBillDecember() {
        storageReturnedOrdersBillingService.process(LocalDate.of(2020, 12, 9));
        storageReturnedOrdersBillingService.process(LocalDate.of(2020, 12, 10));
        storageReturnedOrdersBillingService.process(LocalDate.of(2021, 1, 10));
    }

    @Test
    @DbUnitDataSet(
            before = "StorageReturnedOrdersBillingServiceTest.before.csv",
            after = "StorageReturnedOrdersBillingServiceTest.testBillNovember2021.after.csv"
    )
    void testBillNovember2021() {
        storageReturnedOrdersBillingService.process(LocalDate.of(2021, 11, 9));
        storageReturnedOrdersBillingService.process(LocalDate.of(2021, 11, 10));
        storageReturnedOrdersBillingService.process(LocalDate.of(2021, 12, 9));
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "StorageReturnedOrdersBillingServiceTest.before.csv",
                    "StorageReturnedOrdersBillingServiceTest.testBillJanuary.before.csv",
            },
            after = "StorageReturnedOrdersBillingServiceTest.testBillJanuary.after.csv"
    )
    void testBillJanuary() {
        storageReturnedOrdersBillingService.process(LocalDate.of(2021, 1, 9));
        storageReturnedOrdersBillingService.process(LocalDate.of(2021, 1, 10));
        storageReturnedOrdersBillingService.process(LocalDate.of(2021, 2, 10));
    }

    @Test
    @DbUnitDataSet(
            before = "StorageReturnedOrdersBillingServiceTest.testWithClearObsoleteData.before.csv",
            after = "StorageReturnedOrdersBillingServiceTest.testWithClearObsoleteData.after.csv"
    )
    void testWithClearObsoleteData() {
        storageReturnedOrdersBillingService.process(LocalDate.of(2021, Month.MARCH, 23));
    }

    @Test
    @DbUnitDataSet(
            before = "StorageReturnedOrdersBillingServiceTest.testWithUpsert.before.csv",
            after = "StorageReturnedOrdersBillingServiceTest.testWithUpsert.after.csv"
    )
    void testWithUpsertToday() {
        storageReturnedOrdersBillingService.process(LocalDate.of(2021, Month.MARCH, 25));
    }

    @Test
    @DbUnitDataSet(
            before = "StorageReturnedOrdersBillingServiceTest.testLostCheckpoint.before.csv",
            after = "StorageReturnedOrdersBillingServiceTest.testLostCheckpoint.after.csv"
    )
    void testLostCheckpoint() {
        storageReturnedOrdersBillingService.process(LocalDate.of(2021, Month.MARCH, 25));
    }

    @Test
    @DbUnitDataSet(
            before = "StorageReturnedOrdersBillingServiceTest.importDate.lower.CheckpointDate.before.csv",
            after = "StorageReturnedOrdersBillingServiceTest.importDate.lower.CheckpointDate.after.csv"
    )
    @DisplayName("import_date < checkpoint_date")
    void testWithImportDateLowerCheckpointDate() {
        DatePeriod.of(LocalDate.of(2021, Month.MARCH, 10), LocalDate.of(2021, Month.MARCH, 24))
                .forEach(storageReturnedOrdersBillingService::process);
    }

    @Test
    @DbUnitDataSet(
            before = "StorageReturnedOrdersBillingServiceTest.testClearingToday.before.csv",
            after = "StorageReturnedOrdersBillingServiceTest.testClearingToday.after.csv"
    )
    @DisplayName("ClearingToday")
    void testClearingToday() {
        storageReturnedOrdersBillingService.process(LocalDate.of(2021, Month.APRIL, 19));
    }

    @Test
    @DbUnitDataSet(
            before = "StorageReturnedOrdersBillingServiceTest.testNewStartBillingCheckpoint.before.csv",
            after = "StorageReturnedOrdersBillingServiceTest.testNewStartBillingCheckpoint.after.csv"
    )
    @DisplayName("testNewStartBillingCheckpoint")
    void testNewStartBillingCheckpoint() {
        DatePeriod.of(LocalDate.of(2021, Month.MARCH, 1), LocalDate.of(2021, Month.MARCH, 22))
                .forEach(storageReturnedOrdersBillingService::process);
    }

    @Test
    @DbUnitDataSet(
            before = "StorageReturnedOrdersBillingServiceTest.testBillDefinedPartners.before.csv",
            after = "StorageReturnedOrdersBillingServiceTest.testBillDefinedPartners.after.csv"
    )
    @DisplayName("testBillDefinedPartners")
    void testBillDefinedPartners() {
        storageReturnedOrdersBillingService.billDefinedPartners(
                LocalDate.of(2020, Month.SEPTEMBER, 10),
                Set.of(1L, 12346L),
                Collections.emptySet()
        );
    }
}
