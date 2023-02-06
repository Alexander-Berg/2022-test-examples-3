package ru.yandex.market.billing.marketing;

import java.time.LocalDate;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@ParametersAreNonnullByDefault
class PartnerMarketingCompensationBillingServiceTest extends FunctionalTest {

    @Autowired
    PartnerMarketingCompensationBillingService partnerMarketingCompensationBillingService;

    @Test
    @DisplayName("Обиливание первых промозаказов по кампаниям")
    @DbUnitDataSet(
            before = "PartnerMarketingCompensationBillingServiceTest.common.csv",
            after = "PartnerMarketingCompensationBillingServiceTest.first.after.csv"
    )
    void processWithoutBilledItemsTest() {
        partnerMarketingCompensationBillingService.process(LocalDate.parse("2021-06-15"));
    }

    @Test
    @DisplayName("Обиливание промозаказов по кампаниям, по которым уже есть обиленные данные")
    @DbUnitDataSet(
            before = {
                    "PartnerMarketingCompensationBillingServiceTest.common.csv",
                    "PartnerMarketingCompensationBillingServiceTest.withitems.before.csv"
            },
            after = "PartnerMarketingCompensationBillingServiceTest.withitems.after.csv"
    )
    void processWithBilledItemsTest() {
        partnerMarketingCompensationBillingService.process(LocalDate.parse("2021-06-16"));
    }

    @Test
    @DisplayName("Обиливание промозаказов по кампаниям c превышением сумм")
    @DbUnitDataSet(
            before = {
                    "PartnerMarketingCompensationBillingServiceTest.common.csv",
                    "PartnerMarketingCompensationBillingServiceTest.over.before.csv"
            },
            after = "PartnerMarketingCompensationBillingServiceTest.over.after.csv"
    )
    void processWithOverspendingTest() {
        partnerMarketingCompensationBillingService.process(LocalDate.parse("2021-06-17"));
    }

    @Test
    @DisplayName("Переобиливание промозаказов")
    @DbUnitDataSet(
            before = "PartnerMarketingCompensationBillingServiceTest.reimported.before.csv",
            after = "PartnerMarketingCompensationBillingServiceTest.reimported.after.csv"
    )
    void reimportProcessTest() {
        partnerMarketingCompensationBillingService.process(LocalDate.parse("2021-06-15"));
    }

    @Test
    @DisplayName("Переобилливание промозаказов после переимпорта с игнором отдельных заказов")
    @DbUnitDataSet(
            before = "PartnerMarketingCompensationBillingServiceTest.rebillAfterReimport.before.csv",
            after = "PartnerMarketingCompensationBillingServiceTest.rebillAfterReimport.after.csv"
    )
    void rebillingAfterReimportWithIgnoringOrders() {
        partnerMarketingCompensationBillingService.process(LocalDate.parse("2021-06-15"));
    }

    @Test
    @DisplayName("Обиливание промозаказов по кампаниям с игнорирование некоторых кампаний")
    @DbUnitDataSet(
            before = {
                    "PartnerMarketingCompensationBillingServiceTest.common.csv",
                    "PartnerMarketingCompensationBillingServiceTest.ignoredcampaigns.before.csv"
            },
            after = "PartnerMarketingCompensationBillingServiceTest.ignoredcampaigns.after.csv"
    )
    void processWithIgnoredCampaignsTest() {
        partnerMarketingCompensationBillingService.process(LocalDate.parse("2021-06-15"));
    }

    @Test
    @DisplayName("Обиливание промозаказов по кампаниям с игнорирование некоторых item")
    @DbUnitDataSet(
            before = {
                    "PartnerMarketingCompensationBillingServiceTest.common.csv",
                    "PartnerMarketingCompensationBillingServiceTest.ignored.before.csv"
            },
            after = "PartnerMarketingCompensationBillingServiceTest.ignored.after.csv"
    )
    void processWithIgnoredItemsTest() {
        partnerMarketingCompensationBillingService.process(LocalDate.parse("2021-06-15"));
    }

    @Test
    @DisplayName("Обиливание промозаказов по кампаниям c превышением сумм с учетом корректировок")
    @DbUnitDataSet(
            before = {
                    "PartnerMarketingCompensationBillingServiceTest.common.csv",
                    "PartnerMarketingCompensationBillingServiceTest.over.before.csv",
                    "PartnerMarketingCompensationBillingServiceTest.overWithCorrections.before.csv"
            },
            after = "PartnerMarketingCompensationBillingServiceTest.overWithCorrections.after.csv"
    )
    void processWithOverspendingAndCorrectionsTest() {
        partnerMarketingCompensationBillingService.process(LocalDate.parse("2021-06-17"));
    }

    @Test
    @DisplayName("Переобиливание промозаказов c превышением сумм с учетом корректировок")
    @DbUnitDataSet(
            before = {
                    "PartnerMarketingCompensationBillingServiceTest.reimported.before.csv",
                    "PartnerMarketingCompensationBillingServiceTest.reimportedWithCorrections.before.csv"
            },
            after = "PartnerMarketingCompensationBillingServiceTest.reimportedWithCorrections.after.csv"
    )
    void rebillingWithOverspendingAndCorrectionsTest() {
        partnerMarketingCompensationBillingService.process(LocalDate.parse("2021-06-15"));
    }
}
