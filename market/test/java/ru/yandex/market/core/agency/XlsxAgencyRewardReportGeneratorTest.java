package ru.yandex.market.core.agency;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.agency.program.ProgramRewardType;
import ru.yandex.market.core.agency.program.quarter.Quarter;
import ru.yandex.market.core.agency.report.ActivityReportData;
import ru.yandex.market.core.agency.report.AgencyRewardReportGeneratorSettings;
import ru.yandex.market.core.agency.report.AuctionReportData;
import ru.yandex.market.core.agency.report.PromoDiscountReportData;
import ru.yandex.market.core.agency.report.QualityReportData;
import ru.yandex.market.core.agency.report.XlsxAgencyRewardReportGenerator;
import ru.yandex.market.core.ds.model.DatasourceInfo;
import ru.yandex.market.core.util.DateTimes;

import static ru.yandex.market.core.agency.AgencyRewardXlsxReportTestUtil.checkXlsx;

/**
 * Тесты для {@link XlsxAgencyRewardReportGenerator}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class XlsxAgencyRewardReportGeneratorTest {

    private final static Quarter FIRST_QUARTER = Quarter.of(2017, 1);
    private final static Quarter SECOND_QUARTER = Quarter.of(2017, 2);
    private final static Quarter THIRD_QUARTER = Quarter.of(2017, 3);
    private final static long AGENCY_ID = 123L;

    @Nonnull
    private static ByteArrayOutputStream getActualDataStream(final XlsxAgencyRewardReportGenerator generator) throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        generator.generate(os);
        return os;
    }

    @Test
    @DisplayName("Проверка шаблона до 2021Q1")
    void defaultTemplate() throws IOException {
        AgencyRewardReportGeneratorSettings reportSettings = AgencyRewardReportGeneratorSettings.builder()
                .setTemplatePath(XlsxAgencyRewardReportGenerator.TEMPLATE_XLSX)
                .setWithPromo(true)
                .build();

        checkReportTemplate(reportSettings, "mock_test.csv");
    }

    @Test
    @DisplayName("Проверка шаблона от 2021Q1")
    void templateFrom2021Q1() throws IOException {
        AgencyRewardReportGeneratorSettings reportSettings = AgencyRewardReportGeneratorSettings.builder()
                .setTemplatePath(XlsxAgencyRewardReportGenerator.TEMPLATE_2021Q1_XLSX)
                .setWithPromo(false)
                .build();

        checkReportTemplate(reportSettings, "mock_test_2021Q1.csv");
    }

    private void checkReportTemplate(AgencyRewardReportGeneratorSettings reportSettings,
                                     String expected) throws IOException {
        final Agency agency = new Agency(AGENCY_ID, "test agency", -2, "email@agency.com");

        final AgencyRewardSummary currentSummary = new AgencyRewardSummary(AGENCY_ID, FIRST_QUARTER, 0.1, 0.2, 0.5, 0.5, 2.);
        final List<AgencyRewardSummary> rewardSummaries = List.of(
                new AgencyRewardSummary(AGENCY_ID, SECOND_QUARTER, 0.3, 0.4, 0.2, 0.1, 0.6),
                new AgencyRewardSummary(AGENCY_ID, THIRD_QUARTER, 0.5, 0.6, 1, 0.3, 2.6)
        );

        final Map<ProgramRewardType, Instant> reportDates = ImmutableMap.of(
                ProgramRewardType.QUARTER_TOTAL, DateTimes.toInstant(2017, 1, 3),
                ProgramRewardType.ACTIVITY_QUALITY, DateTimes.toInstant(2017, 1, 2),
                ProgramRewardType.AUCTION, DateTimes.toInstant(2020, 2, 2),
                ProgramRewardType.PROMO_DISCOUNT, DateTimes.toInstant(2018, 1, 2)
        );

        final DatasourceInfo shop101 = new DatasourceInfo();
        shop101.setId(101L);
        shop101.setInternalName("shop101");

        final DatasourceInfo shop102 = new DatasourceInfo();
        shop102.setId(102L);
        shop102.setInternalName("shop102");

        final List<QualityReportData> qualityReportData = Arrays.asList(
                QualityReportData.builder()
                        .stateDate(DateTimes.toInstant(2017, 1, 2))
                        .agencyId(AGENCY_ID)
                        .clientId(15L)
                        .clientFromDate(DateTimes.toInstant(2018, 2, 3))
                        .shopDomain("shop101")
                        .campaignId(10101L)
                        .shopPassedChecks(5)
                        .shopTotalChecks(10)
                        .qualityRatio(0.5)
                        .build(),
                QualityReportData.builder()
                        .stateDate(DateTimes.toInstant(2017, 1, 2))
                        .agencyId(AGENCY_ID)
                        .clientId(15L)
                        .clientFromDate(DateTimes.toInstant(2018, 2, 3))
                        .shopDomain("shop102")
                        .campaignId(10102)
                        .shopPassedChecks(25)
                        .shopTotalChecks(100)
                        .qualityRatio(0.25)
                        .build()
        );

        final List<ActivityReportData> activityReportData = Arrays.asList(
                ActivityReportData.builder()
                        .stateDate(DateTimes.toInstant(2017, 1, 2))
                        .agencyId(AGENCY_ID)
                        .clientId(15L)
                        .clientFromDate(DateTimes.toInstant(2018, 2, 3))
                        .shopDomains("shop101, shop102")
                        .activeDays(19)
                        .totalDays(20)
                        .turnoverRate(0.4)
                        .activityRatio(0.56)
                        .build(),
                ActivityReportData.builder()
                        .stateDate(DateTimes.toInstant(2017, 1, 2))
                        .agencyId(AGENCY_ID)
                        .clientId(25L)
                        .clientFromDate(DateTimes.toInstant(2018, 1, 11))
                        .shopDomains("shop201")
                        .activeDays(17)
                        .totalDays(17)
                        .turnoverRate(0.6)
                        .activityRatio(0.12)
                        .build()
        );

        final List<PromoDiscountReportData> promoDiscountReportData = Arrays.asList(
                PromoDiscountReportData.builder()
                        .agencyId(AGENCY_ID)
                        .campaignId(2000000L)
                        .stateDate(DateTimes.toInstant(2018, 2, 11))
                        .clientFromDate(DateTimes.toInstant(2018, 1, 11))
                        .clientId(15L)
                        .datasourceId(101L)
                        .discountOffers(100)
                        .promoOffers(100)
                        .discountRatio(0.12)
                        .promoRatio(0.13)
                        .shopName("shop101")
                        .totalOffers(1000)
                        .build(),
                PromoDiscountReportData.builder()
                        .agencyId(AGENCY_ID)
                        .campaignId(2000001L)
                        .stateDate(DateTimes.toInstant(2018, 2, 12))
                        .clientFromDate(DateTimes.toInstant(2018, 1, 12))
                        .clientId(16L)
                        .datasourceId(102L)
                        .discountOffers(101)
                        .promoOffers(102)
                        .discountRatio(0.14)
                        .promoRatio(0.15)
                        .shopName("shop102")
                        .totalOffers(1000)
                        .build(),
                PromoDiscountReportData.builder()
                        .agencyId(AGENCY_ID)
                        .campaignId(2000002L)
                        .stateDate(DateTimes.toInstant(2018, 2, 13))
                        .clientFromDate(DateTimes.toInstant(2018, 1, 13))
                        .clientId(17L)
                        .datasourceId(103L)
                        .discountOffers(100)
                        .promoOffers(105)
                        .discountRatio(0.12)
                        .promoRatio(0.10)
                        .shopName("shop103")
                        .totalOffers(100)
                        .build()
        );

        final List<AuctionReportData> auctionReportData = List.of(
                AuctionReportData.builder()
                        .stateDate((DateTimes.toInstant(2020, 2, 2)))
                        .agencyId(22L)
                        .shopName("shop101")
                        .clientFromDate((DateTimes.toInstant(2018, 1, 1)))
                        .campaignId(2000001)
                        .clientId(15L)
                        .auctionRatio(0.23)
                        .build(),
                AuctionReportData.builder()
                        .stateDate((DateTimes.toInstant(2020, 2, 2)))
                        .agencyId(22L)
                        .shopName("shop102")
                        .clientFromDate((DateTimes.toInstant(2018, 1, 1)))
                        .campaignId(2000002)
                        .clientId(15L)
                        .auctionRatio(0.33)
                        .build(),
                AuctionReportData.builder()
                        .stateDate((DateTimes.toInstant(2020, 2, 2)))
                        .agencyId(22L)
                        .shopName("shop103")
                        .clientFromDate((DateTimes.toInstant(2018, 1, 1)))
                        .campaignId(2000003)
                        .clientId(15L)
                        .auctionRatio(0.43)
                        .build()
        );


        final XlsxAgencyRewardReportGenerator generator = XlsxAgencyRewardReportGenerator.builder()
                .agency(agency)
                .currentRewardSummary(currentSummary)
                .totalRewardSummaries(rewardSummaries)
                .reportDates(reportDates)
                .activityReportData(activityReportData)
                .qualityReportData(qualityReportData)
                .promoDiscountReportData(promoDiscountReportData)
                .auctionReportData(auctionReportData)
                .settings(reportSettings)
                .build();

        final ByteArrayOutputStream actualDataStream = getActualDataStream(generator);
        checkXlsx(actualDataStream, expected, getClass());
    }
}
