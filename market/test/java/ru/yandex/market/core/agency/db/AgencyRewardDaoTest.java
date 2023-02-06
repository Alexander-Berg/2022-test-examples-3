package ru.yandex.market.core.agency.db;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.agency.AgencyRewardSummary;
import ru.yandex.market.core.agency.program.quarter.Quarter;
import ru.yandex.market.core.agency.program.quarter.model.ArpAuctionAgency;
import ru.yandex.market.core.agency.program.quarter.model.ArpPromoDiscountAgency;
import ru.yandex.market.core.agency.report.ActivityReportData;
import ru.yandex.market.core.agency.report.AuctionReportData;
import ru.yandex.market.core.agency.report.PromoDiscountReportData;
import ru.yandex.market.core.agency.report.QualityReportData;
import ru.yandex.market.core.util.DateTimes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для {@link AgencyRewardDao}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class AgencyRewardDaoTest extends FunctionalTest {
    private static final Quarter FIRST_QUARTER = Quarter.of(2017, 1);
    private static final Quarter SECOND_QUARTER = Quarter.of(2017, 2);
    private static final Quarter THIRD_QUARTER = Quarter.of(2019, 3);

    @Autowired
    private AgencyRewardDao agencyRewardDao;

    private static Stream<Arguments> getSummaryData() {
        return Stream.of(
                Arguments.of(
                        "агентство с двумя активными клиентами. У каждого клиента по одному магазину. Магазины " +
                                "проходили проверку качества",
                        11L,
                        List.of(
                                new AgencyRewardSummary(11L, FIRST_QUARTER, 0.111, 0, 0.7222222222222222,
                                        0.7710526315789473, null),
                                new AgencyRewardSummary(11L, SECOND_QUARTER, 0.112, 0, 0.9994997498749375,
                                        0.9994984954864594, 3.99)
                        )
                ),
                Arguments.of(
                        "агентство с двумя активными клиентами. У одного из них два магазина. Все проверки пройдены",
                        22L,
                        List.of(
                                new AgencyRewardSummary(22L, FIRST_QUARTER, 0.113, 0, 1., 0.7112359550561798, null),
                                new AgencyRewardSummary(22L, SECOND_QUARTER, 0.114, 0, 0.9996665555185061,
                                        0.9996659986639946, 3.99)
                        )
                ),
                Arguments.of(
                        "агентство с двумя клиентами. Активен только один. Не было проверок",
                        33L,
                        List.of(
                                new AgencyRewardSummary(33L, FIRST_QUARTER, 0.115, 0, 1, 0.42105263157894735, null),
                                new AgencyRewardSummary(33L, SECOND_QUARTER, 0.116, 0, 0.9997499374843711,
                                        0.9997496244366549, 3.99)
                        )
                ),
                Arguments.of(
                        "агентство без активных клиентов. Все проверки провалены",
                        44L,
                        List.of(
                                new AgencyRewardSummary(44L, FIRST_QUARTER, 0.117, 0, 0, 0., null),
                                new AgencyRewardSummary(44L, SECOND_QUARTER, 0.118, 0, 0.9997999599919984,
                                        0.999799759711654, 3.99)
                        )
                ),
                Arguments.of(
                        "агентство с двумя клиентами. Активены оба, но нет оборота. Качество больше порога",
                        55L,
                        List.of(
                                new AgencyRewardSummary(55L, FIRST_QUARTER, 0.119, 0, 0.9, 0., null),
                                new AgencyRewardSummary(55L, SECOND_QUARTER, 0.121, 0, 0.9998333055509252,
                                        0.9998331664998331, 3.99),
                                new AgencyRewardSummary(55L, THIRD_QUARTER, 0.122, 0, 0.9, 0., null)
                        )
                ),
                Arguments.of(
                        "агентство с двумя клиентами. Активены оба, но оборот есть только у одного",
                        66L,
                        List.of(
                                new AgencyRewardSummary(66L, FIRST_QUARTER, 0.123, 0, 0.9285714285714286,
                                        0.6666666666666666, null),
                                new AgencyRewardSummary(66L, SECOND_QUARTER, 0.124, 0, 0.9998571224460637,
                                        0.999857020303117, 3.99)
                        )
                ),
                Arguments.of(
                        "агентство с минимальными значениями качества и активности для премий",
                        77L,
                        List.of(
                                new AgencyRewardSummary(77L, FIRST_QUARTER, 0.125, 0, 0.8, 0.9, 3.4),
                                new AgencyRewardSummary(77L, THIRD_QUARTER, 0.126, 0, 0.75, 0.85, 0.14)
                        )
                ),
                Arguments.of(
                        "агентство с данными из кварталов, у которых разные настройки подсчета",
                        88L,
                        List.of(
                                new AgencyRewardSummary(88L, FIRST_QUARTER, 0, 0, 0.8, 0.9, 3.4),
                                new AgencyRewardSummary(88L, THIRD_QUARTER, 0, 0, 0.8, 0.9, 4.31)
                        )
                ),
                Arguments.of(
                        "Нет данных для агентства",
                        99999L,
                        List.of()
                )
        );
    }

    @Test
    @DisplayName("Получение детализированной информации о качестве для отчета")
    @DbUnitDataSet(before = "csv/AgencyRewardDao.get_quality.before.csv")
    void testGetQualityReport() {
        List<QualityReportData> qualityReportData = agencyRewardDao.getQualityReportData(11L, FIRST_QUARTER);
        assertThat(qualityReportData).containsExactlyInAnyOrder(
                QualityReportData.builder()
                        .agencyId(11L)
                        .stateDate(DateTimes.toInstant(2017, 1, 1))
                        .clientId(15L)
                        .clientFromDate(DateTimes.toInstant(2018, 2, 3))
                        .shopDomain("shop16")
                        .shopPassedChecks(9)
                        .shopTotalChecks(10)
                        .qualityRatio(0.9)
                        .build(),

                QualityReportData.builder()
                        .agencyId(11L)
                        .stateDate(DateTimes.toInstant(2017, 1, 1))
                        .clientId(25L)
                        .clientFromDate(DateTimes.toInstant(2018, 4, 5))
                        .shopDomain("shop26")
                        .shopPassedChecks(4)
                        .shopTotalChecks(9)
                        .qualityRatio(0.4444444444444444)
                        .build(),

                QualityReportData.builder()
                        .agencyId(11L)
                        .stateDate(DateTimes.toInstant(2017, 1, 1))
                        .clientId(25L)
                        .clientFromDate(DateTimes.toInstant(2018, 4, 5))
                        .shopDomain("shop27")
                        .shopPassedChecks(0)
                        .shopTotalChecks(0)
                        .qualityRatio(1)
                        .build()
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getSummaryData")
    @DisplayName("Получение коэффициентов")
    @DbUnitDataSet(before = "csv/AgencyRewardDao.get_activity.before.csv")
    void testGetRatio(String name, long agencyId, List<AgencyRewardSummary> expectedSummary) {
        Map<Quarter, AgencyRewardSummary> actualSummary = agencyRewardDao.getRewardSummary(agencyId);
        for (var entry : actualSummary.entrySet()) {
            assertThat(entry.getValue().getQuarter()).isEqualTo(entry.getKey());
        }
        assertThat(actualSummary.values()).containsExactlyInAnyOrderElementsOf(expectedSummary);
    }

    @Test
    @DisplayName("Получение детализированной информации об активности для отчета")
    @DbUnitDataSet(before = "csv/AgencyRewardDao.get_activity.before.csv")
    void testGetActivityReport() {
        List<ActivityReportData> activityReportData = agencyRewardDao.getActivityReportData(22L, FIRST_QUARTER);
        assertThat(activityReportData).containsExactlyInAnyOrder(
                ActivityReportData.builder()
                        .agencyId(22L)
                        .stateDate(DateTimes.toInstant(2018, 2, 3))
                        .clientId(15L)
                        .clientFromDate(DateTimes.toInstant(2018, 2, 3))
                        .shopDomains("shop16")
                        .activeDays(19)
                        .totalDays(20)
                        .turnoverRate(0.15730337078651685)
                        .activityRatio(0.149438202247191)
                        .build(),

                ActivityReportData.builder()
                        .agencyId(22L)
                        .stateDate(DateTimes.toInstant(2018, 2, 3))
                        .clientId(25L)
                        .clientFromDate(DateTimes.toInstant(2018, 4, 5))
                        .shopDomains("shop26, shop27")
                        .activeDays(10)
                        .totalDays(15)
                        .turnoverRate(0.8426966292134831)
                        .activityRatio(0.5617977528089888)
                        .build()
        );
    }

    @Test
    @DisplayName("Получение детализированной информации о промо и скидках для отчета")
    @DbUnitDataSet(before = "csv/AgencyRewardDao.getPromoDiscountAgency.before.csv")
    void testGetPromoDiscountAgency() {
        List<ArpPromoDiscountAgency> promoDiscountAgency = agencyRewardDao.getPromoDiscountAgency(999L);
        assertThat(promoDiscountAgency).containsExactlyInAnyOrder(
                ArpPromoDiscountAgency.builder()
                        .agencyId(999L)
                        .discountOffers(2)
                        .promoOffers(3)
                        .totalOffers(100)
                        .totalRatio(0.777)
                        .quarter(Quarter.of(2020, 1))
                        .build(),
                ArpPromoDiscountAgency.builder()
                        .agencyId(999L)
                        .discountOffers(21)
                        .promoOffers(31)
                        .totalOffers(1001)
                        .totalRatio(0.888)
                        .quarter(Quarter.of(2020, 2))
                        .build()
        );
    }

    @Test
    @DisplayName("Получение данных для агентской премии за клики по не минимальным ставкам")
    @DbUnitDataSet(before = "csv/AgencyRewardDao.getAuctionAgency.before.csv")
    void testGetAuctionAgency() {
        List<ArpAuctionAgency> auctionAgency = agencyRewardDao.getArpAuctionAgency(999L);
        assertThat(auctionAgency).containsExactlyInAnyOrder(
                ArpAuctionAgency.builder()
                        .agencyId(999L)
                        .auctionClicks(5)
                        .auctionPrice(6)
                        .totalClicks(7)
                        .totalPrice(8)
                        .auctionClicksRatio(0.1)
                        .auctionPriceRatio(0.2)
                        .quarter(Quarter.of(2020, 1))
                        .build(),
                ArpAuctionAgency.builder()
                        .agencyId(999L)
                        .auctionClicks(51)
                        .auctionPrice(61)
                        .totalClicks(71)
                        .totalPrice(81)
                        .auctionClicksRatio(0.11)
                        .auctionPriceRatio(0.21)
                        .quarter(Quarter.of(2020, 2))
                        .build()
        );
    }

    @Test
    @DisplayName("Получение данных по скидкам и промо")
    @DbUnitDataSet(before = "csv/AgencyRewardDao.getPromoDiscountDatasource.before.csv")
    void getPromoDiscount() {
        List<PromoDiscountReportData> actual = agencyRewardDao.getPromoDiscountReportData(22L, Quarter.of(2020, 1));
        assertThat(actual).containsExactlyInAnyOrder(
                PromoDiscountReportData.builder()
                        .stateDate((DateTimes.toInstant(2020, 2, 2)))
                        .agencyId(22L)
                        .shopName("shop101")
                        .clientFromDate((DateTimes.toInstant(2018, 1, 1)))
                        .campaignId(20000001)
                        .clientId(15L)
                        .promoOffers(517)
                        .discountOffers(417)
                        .totalOffers(1000)
                        .promoRatio(0.57)
                        .discountRatio(0.47)
                        .datasourceId(101)
                        .build(),
                PromoDiscountReportData.builder()
                        .stateDate((DateTimes.toInstant(2020, 2, 2)))
                        .agencyId(22L)
                        .shopName("shop102")
                        .clientFromDate((DateTimes.toInstant(2018, 1, 1)))
                        .campaignId(20000002)
                        .clientId(15L)
                        .promoOffers(518)
                        .discountOffers(418)
                        .totalOffers(2000)
                        .promoRatio(0.58)
                        .discountRatio(0.48)
                        .datasourceId(102)
                        .build(),
                PromoDiscountReportData.builder()
                        .stateDate((DateTimes.toInstant(2020, 2, 2)))
                        .agencyId(22L)
                        .shopName("shop103")
                        .clientFromDate((DateTimes.toInstant(2018, 1, 1)))
                        .campaignId(20000003)
                        .clientId(15L)
                        .promoOffers(519)
                        .discountOffers(419)
                        .totalOffers(3000)
                        .promoRatio(0.59)
                        .discountRatio(0.49)
                        .datasourceId(103)
                        .build(),
                PromoDiscountReportData.builder()
                        .stateDate((DateTimes.toInstant(2020, 2, 2)))
                        .agencyId(22L)
                        .shopName("shop104")
                        .clientFromDate((DateTimes.toInstant(2018, 1, 1)))
                        .campaignId(20000004)
                        .clientId(15L)
                        .promoOffers(520)
                        .discountOffers(420)
                        .totalOffers(4000)
                        .promoRatio(0.50)
                        .discountRatio(0.40)
                        .datasourceId(104)
                        .build()
        );
    }

    @Test
    @DisplayName("Получение данных по ставкам")
    @DbUnitDataSet(before = "csv/AgencyRewardDao.getAuctionDatasource.before.csv")
    void getAuctionDatasource() {
        List<AuctionReportData> actual = agencyRewardDao.getAuctionReportData(22L, Quarter.of(2020, 1));
        assertThat(actual).containsExactlyInAnyOrder(
                AuctionReportData.builder()
                        .stateDate((DateTimes.toInstant(2020, 2, 2)))
                        .agencyId(22L)
                        .shopName("shop101")
                        .clientFromDate((DateTimes.toInstant(2018, 1, 1)))
                        .campaignId(20000001)
                        .clientId(15L)
                        .auctionRatio(0.23)
                        .build(),
                AuctionReportData.builder()
                        .stateDate((DateTimes.toInstant(2020, 2, 2)))
                        .agencyId(22L)
                        .shopName("shop102")
                        .clientFromDate((DateTimes.toInstant(2018, 1, 1)))
                        .campaignId(20000002)
                        .clientId(15L)
                        .auctionRatio(0.33)
                        .build(),
                AuctionReportData.builder()
                        .stateDate((DateTimes.toInstant(2020, 2, 2)))
                        .agencyId(22L)
                        .shopName("shop103")
                        .clientFromDate((DateTimes.toInstant(2018, 1, 1)))
                        .campaignId(20000003)
                        .clientId(15L)
                        .auctionRatio(0.43)
                        .build(),
                AuctionReportData.builder()
                        .stateDate((DateTimes.toInstant(2020, 2, 2)))
                        .agencyId(22L)
                        .shopName("shop104")
                        .clientFromDate((DateTimes.toInstant(2018, 1, 1)))
                        .campaignId(20000004)
                        .clientId(15L)
                        .auctionRatio(0.53)
                        .build()
        );
    }

}
