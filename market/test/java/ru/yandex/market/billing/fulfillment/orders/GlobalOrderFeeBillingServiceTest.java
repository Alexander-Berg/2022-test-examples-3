package ru.yandex.market.billing.fulfillment.orders;

import java.security.InvalidParameterException;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Tests for {@link GlobalOrderFeeBillingService}
 */
class GlobalOrderFeeBillingServiceTest extends FunctionalTest {

    private static final LocalDate DATE_2022_01_01 = LocalDate.of(2022, 1, 1);
    private static final LocalDate DATE_2022_04_01 = LocalDate.of(2022, 4, 1);

    @Autowired
    private GlobalOrderFeeBillingService globalOrderFeeBillingService;

    @Test
    @DisplayName("Не падаем, когда нечего биллить.")
    void test_shouldNotFailWhenNoBillingData() {
        globalOrderFeeBillingService.bill(LocalDate.of(2022, 1, 1));
    }

    @Test
    @DisplayName("Биллим указанную дату.")
    @DbUnitDataSet(before = "GlobalOrderFeeBillingServiceTest.before.csv",
            after = "GlobalOrderFeeBillingServiceTest.after.csv")
    void test_shouldBillGivenDateWhenBillingDataGiven() {
        globalOrderFeeBillingService.bill(DATE_2022_01_01);
    }

    @Test
    @DisplayName("Биллим определенных партнеров.")
    @DbUnitDataSet(before = "GlobalOrderFeeBillingServiceTest_specific_partner.before.csv",
            after = "GlobalOrderFeeBillingServiceTest_specific_partner.after.csv")
    void test_shouldBillWhenSpecificPartnerIdsGiven() {
        globalOrderFeeBillingService.billForPartners(DATE_2022_01_01, Set.of(12345L));
    }

    @Test
    @DisplayName("Перебилливаем существующие записи.")
    @DbUnitDataSet(before = "GlobalOrderFeeBillingServiceTest_rebill_existing.before.csv",
            after = "GlobalOrderFeeBillingServiceTest_rebill_existing.after.csv")
    void test_shouldRebillWhenAlreadyBilledRecordsGiven() {
        globalOrderFeeBillingService.billForPartners(DATE_2022_01_01, Set.of(12345L));
    }

    @Test
    @DisplayName("Падаем, если переданы несуществующие партнеры.")
    @DbUnitDataSet(before = "GlobalOrderFeeBillingServiceTest_exception.before.csv")
    void test_shouldFailWhenNonExistingPartnerIdsGiven() {
        Assertions.assertThrows(
                InvalidParameterException.class,
                () -> globalOrderFeeBillingService.billForPartners(DATE_2022_01_01, Set.of(666L))
        );
    }

    @Test
    @DisplayName("Биллим по категориям, с партнерами и с тарифами.")
    @DbUnitDataSet(before = "GlobalOrderFeeBillingServiceTest_specific_partner_specific_tariff.before.csv",
            after = "GlobalOrderFeeBillingServiceTest_specific_partner_specific_tariff.after.csv")
    void test_shouldBillWhenSpecificPartnerIdAndSpecificTariffGiven() {
        globalOrderFeeBillingService.billForPartners(DATE_2022_04_01, Set.of(4270579L, 123456L));
    }

    @Test
    @DisplayName("Биллим неизвестную категорию.")
    @DbUnitDataSet(before = "GlobalOrderFeeBillingServiceTest_unknown_category.before.csv",
            after = "GlobalOrderFeeBillingServiceTest_unknown_category.after.csv")
    void test_shouldBillUnknounKategory() {
        globalOrderFeeBillingService.bill(DATE_2022_04_01);
    }

    @Test
    @DisplayName("Биллим TEVA_ZOL")
    @DbUnitDataSet(before = "GlobalOrderFeeBillingServiceTest_TevaZol.before.csv",
            after = "GlobalOrderFeeBillingServiceTest_TevaZol.after.csv")
    void test_BillTevaZol() {
        globalOrderFeeBillingService.bill(LocalDate.of(2022, 6, 15));
        globalOrderFeeBillingService.bill(LocalDate.of(2022, 6, 16));
        globalOrderFeeBillingService.bill(LocalDate.of(2022, 7, 8));
        globalOrderFeeBillingService.bill(LocalDate.of(2022, 7, 9));
    }

    @Test
    @DisplayName("Биллим июль.")
    @DbUnitDataSet(before = "GlobalOrderFeeBillingServiceTest_july.before.csv",
            after = "GlobalOrderFeeBillingServiceTest_july.after.csv")
    void test_BillJune() {
        globalOrderFeeBillingService.bill(LocalDate.of(2022, 7, 7));
        globalOrderFeeBillingService.bill(LocalDate.of(2022, 7, 8));
        globalOrderFeeBillingService.bill(LocalDate.of(2022, 7, 9));
    }

    @Test
    @DisplayName("Биллим кастомные тарифы за июль.")
    @DbUnitDataSet(before = "GlobalOrderFeeBillingServiceTest_july_cheap_shop.before.csv",
            after = "GlobalOrderFeeBillingServiceTest_july_cheap_shop.after.csv")
    void test_shouldBillJulyWhenCheapShopGiven() {
        globalOrderFeeBillingService.bill(LocalDate.of(2022, 7, 7));
        globalOrderFeeBillingService.bill(LocalDate.of(2022, 7, 8));
        globalOrderFeeBillingService.bill(LocalDate.of(2022, 7, 9));
        globalOrderFeeBillingService.bill(LocalDate.of(2022, 9, 7));
        globalOrderFeeBillingService.bill(LocalDate.of(2022, 9, 8));
    }
}
