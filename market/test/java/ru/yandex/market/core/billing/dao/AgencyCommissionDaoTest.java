package ru.yandex.market.core.billing.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.billing.model.AgencyCommissionBillingInfo;
import ru.yandex.market.core.billing.model.AgencyCommissionReportItem;
import ru.yandex.market.core.campaign.model.CampaignType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

@ParametersAreNonnullByDefault
class AgencyCommissionDaoTest extends FunctionalTest {

    @Autowired
    private AgencyCommissionDao agencyCommissionDao;

    @Test
    @DisplayName("Пустой список заклиреных платежей.")
    void test_getClearedReceiptItems_shouldNotFail_whenEmpty() {
        List<AgencyCommissionBillingInfo> clearedReceiptItems =
                agencyCommissionDao.getAgencyCommissionBillingRecords(Set.of(1L, 2L, 3L));

        assertThat(clearedReceiptItems, hasSize(0));
    }

    @Test
    @DisplayName("Получить записи прямых платежей чека")
    @DbUnitDataSet(before = "AgencyCommissionDaoTest.getClearedReceiptItems.before.csv", after = "")
    void test_getClearedReceiptItems_shouldGetCorrectItems_whenHaveRecords() {
        List<AgencyCommissionBillingInfo> clearedReceiptItems =
                agencyCommissionDao.getAgencyCommissionBillingRecords(Set.of(1L, 2L, 3L));

        assertThat(clearedReceiptItems, hasSize(3));
    }

    @Test
    @DisplayName("Данные для отчёта для поставщика (SUPPLIER_ID).")
    @DbUnitDataSet(before = "AgencyCommissionDaoTest.getReportItems.before.csv")
    void test_getAgencyCommissionReportItems_whenGivenSupplierId() {
        List<AgencyCommissionReportItem> agencyCommissionReportItems = agencyCommissionDao.getAgencyCommissionReportItems(
                1L,
                CampaignType.SUPPLIER,
                100L,
                LocalDate.of(2021, 1, 1),
                LocalDate.of(2021, 5, 31));
        assertThat(agencyCommissionReportItems, hasSize(1));
    }

    @Test
    @DisplayName("Данные для отчёта для магазина (DBS SHOP_ID).")
    @DbUnitDataSet(before = "AgencyCommissionDaoTest.getReportItems.before.csv")
    void test_getAgencyCommissionReportItems_whenGivenShopIdDbs() {
        List<AgencyCommissionReportItem> agencyCommissionReportItems = agencyCommissionDao.getAgencyCommissionReportItems(
                774L,
                CampaignType.SHOP,
                100L,
                LocalDate.of(2021, 1, 1),
                LocalDate.of(2021, 5, 31));
        assertThat(agencyCommissionReportItems, hasSize(1));
    }

    @Test
    @DisplayName("Данные для отчёта по всем партнерам.")
    @DbUnitDataSet(before = "AgencyCommissionDaoTest.getReportItems.before.csv")
    void test_getAgencyCommissionReportItems() {
        List<AgencyCommissionReportItem> agencyCommissionReportItems = agencyCommissionDao.getAgencyCommissionReportItems(
                null,
                null,
                null,
                LocalDate.of(2021, 1, 1),
                LocalDate.of(2021, 5, 31));
        assertThat(agencyCommissionReportItems, hasSize(5));
    }
}
