package ru.yandex.market.rg.agency;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.agency.report.AgencyCheckerReportData;
import ru.yandex.market.rg.config.ClickhouseFunctionalTest;

/**
 * Тесты для {@link AgencyCheckerReportDao}.
 */
class AgencyCheckerReportDaoTest extends ClickhouseFunctionalTest {

    private static final String CLICK_HOUSE = "clickHouseDataSource";
    private static final long AGENCY_ID = 2233719L;

    @Autowired
    AgencyCheckerReportDao agencyCheckerReportDao;

    private static AgencyCheckerReportData getTestData() {
        return AgencyCheckerReportData.builder()
                .date(LocalDate.parse("2020-01-01"))
                .agencyId(2233719L)
                .acpPrev(9.179126213592234)
                .acpCurrent(9.331578947368422)
                .acpDelta("1.66%")
                .activityPrev(0.6896551724137931)
                .activityCurrent(0.7096774193548387)
                .activityDelta("2.9%")
                .agencyName("ООО «ТУТ БАЙ МЕДИА»")
                .api("Да")
                .balanceResult(821.1)
                .bidder("PL")
                .budgetPrev(1890.9)
                .budgetCurrent(177.3)
                .budgetDelta("-90.62%")
                .campaignIdStr("11-21400497")
                .changedBid("Да")
                .citiesCount(158L)
                .clickedOffersPrev(14.142857142857142)
                .clickedOffersCurrent(1.3076923076923077)
                .clickedOffersDelta("-90.75%")
                .clickedOffersSharePrev("0.11%")
                .clickedOffersShareCurrent("1.23%")
                .clicksPrev(206.)
                .clicksCurrent(19.)
                .clientId(32432012L)
                .countPrev(1L)
                .countCurrent(0L)
                .deliveryServices(1.)
                .deliveryServicesDomain(1.5)
                .domainClean("diskagruz.by")
                .domain("diskagruz.by")
                .ecom("Нет")
                .experiment("Да")
                .fielddate("2020-03-01")
                .goal("Нет")
                .hasSisInPickupDesc("Нет")
                .hasSisInPickupDescDomain("Да")
                .managerFullname("Амирян Андрей")
                .matchedOffersPrev(11879.896551724138)
                .matchedOffersCurrent(4.935483870967742)
                .matchedOffersDelta("-99.96%")
                .maxCities(49965L)
                .offersPrev(12776.551724137931)
                .offersCurrent(106.)
                .offersDelta("-99.17%")
                .pickupPointDesc("Да")
                .pickupPointDescDomain("Да")
                .ratingPrev(4.)
                .ratingCurrent(4.)
                .ratingDelta(0.)
                .regionalSharePrev("35.44%")
                .regionalShareCurrent("36.84%")
                .regionsDeliveryRuleDesc("Нет")
                .regionsDeliveryRuleDescDomain("Нет")
                .regionsShippersEnabledDesc("Да")
                .regionsShippersEnabledDescDomain("Да")
                .resultPrev("[Открывается, если баланс магазина опускает ниже заданного значения. - 1]")
                .resultCurrent("Нет")
                .shopId(412151L)
                .shopName("diskagruz.by")
                .strategies("Настроены стратегии:стратегия по умолчанию")
                .unmatchedOffersPrev(896.6551724137935)
                .unmatchedOffersCurrent(101.06451612903226)
                .unmatchedOffersDelta("-88.73%")
                .build();
    }

    @Test
    @DbUnitDataSet(dataSource = CLICK_HOUSE, before = "csv/AgencyCheckerReportDaoTest.getDataTest.before.csv")
    @DisplayName("Получение данных из кликхауса по чекеру для конкретного агентсва, определенного месяца")
    void getDataTest() {
        LocalDate month = LocalDate.of(2020, 1, 1);
        List<AgencyCheckerReportData> actual = agencyCheckerReportDao.getReportData(AGENCY_ID, month);
        Assertions.assertEquals(List.of(getTestData()), actual);
    }

    @Test
    @DisplayName("Если данных нет - должен возвращаться пустой список")
    void emptyResultTest() {
        final LocalDate month = LocalDate.of(2020, 1, 1);
        Assertions.assertEquals(List.of(), agencyCheckerReportDao.getReportData(-1, month));
    }
}
