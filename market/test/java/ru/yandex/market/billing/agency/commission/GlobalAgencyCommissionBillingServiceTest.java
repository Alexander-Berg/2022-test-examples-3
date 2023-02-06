package ru.yandex.market.billing.agency.commission;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Tests for {@link GlobalAgencyCommissionBillingService}
 */
public class GlobalAgencyCommissionBillingServiceTest extends FunctionalTest {

    private static final LocalDate DATE_2021_12_01 = LocalDate.of(2021, 12, 1);

    @Autowired
    private GlobalAgencyCommissionBillingService globalAgencyCommissionBillingService;

    @Test
    @DisplayName("Не падаем, когда нечего биллить.")
    @DbUnitDataSet(before = "GlobalAgencyCommissionTest.before.csv",
            after = "GlobalAgencyCommissionTest.after.csv")
    void testNoBillingData() {
        globalAgencyCommissionBillingService.process(DATE_2021_12_01);
    }

    @Test
    @DisplayName("Биллим только указанную дату.")
    @DbUnitDataSet(before = "GlobalAgencyCommissionTest_bill_specific_date.before.csv",
            after = "GlobalAgencyCommissionTest_bill_specific_date.after.csv")
    void shouldBillSpecificDateWhenBillingDataGiven() {
        globalAgencyCommissionBillingService.process(DATE_2021_12_01);
    }

    @Test
    @DisplayName("Биллим только указанную дату и партнера.")
    @DbUnitDataSet(before = "GlobalAgencyCommissionTest_bill_specific_date_partner.before.csv",
            after = "GlobalAgencyCommissionTest_bill_specific_date_partner.after.csv")
    void shouldBillSpecificDateWhenBillingDateAndPartnerIdsGiven() {
        globalAgencyCommissionBillingService.billForPartners(
                DATE_2021_12_01,
                Set.of(12L));
    }

    @Test
    @DisplayName("Пересчитываем данные, когда были уже обилленые записи.")
    @DbUnitDataSet(before = "GlobalAgencyCommissionTest_rebill_correct.before.csv",
            after = "GlobalAgencyCommissionTest_rebill_correct.after.csv")
    void shouldRebillSpecificDateWhenBillingDataGiven() {
        globalAgencyCommissionBillingService.process(DATE_2021_12_01);
    }
}
