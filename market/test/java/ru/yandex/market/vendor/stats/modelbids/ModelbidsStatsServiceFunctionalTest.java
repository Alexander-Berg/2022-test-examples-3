package ru.yandex.market.vendor.stats.modelbids;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.vendor.stats.modelbids.ClickPaymentType;
import ru.yandex.vendor.stats.modelbids.ModelbidsStatsContext;
import ru.yandex.vendor.stats.modelbids.ModelbidsStatsService;
import ru.yandex.vendor.stats.modelbids.PlatformPlacementGroupType;

import static ru.yandex.market.vendor.stats.StatsTestUtils.verifyGeneratedReport;
import static ru.yandex.vendor.stats.PlatformType.TOUCH;
import static ru.yandex.vendor.stats.StatisticsScaleType.DAY;
import static ru.yandex.vendor.stats.StatisticsScaleType.WEEK;
import static ru.yandex.vendor.stats.modelbids.ModelbidsMetricType.AVG_BID;
import static ru.yandex.vendor.stats.modelbids.ModelbidsMetricType.AVG_CLICK_PRICE;
import static ru.yandex.vendor.stats.modelbids.ModelbidsMetricType.CHARGES;
import static ru.yandex.vendor.stats.modelbids.ModelbidsMetricType.CLICKS_PAID;
import static ru.yandex.vendor.stats.modelbids.ModelbidsMetricType.CLICKS_TOTAL;
import static ru.yandex.vendor.stats.modelbids.ModelbidsMetricType.CPO;
import static ru.yandex.vendor.stats.modelbids.ModelbidsMetricType.CR;
import static ru.yandex.vendor.stats.modelbids.ModelbidsMetricType.CTR_NEW;
import static ru.yandex.vendor.stats.modelbids.ModelbidsMetricType.ORDERS;
import static ru.yandex.vendor.stats.modelbids.ModelbidsMetricType.SHOWS;
import static ru.yandex.vendor.stats.modelbids.ModelbidsMetricType.TARGET_ACTIONS;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/stats/modelbids/ModelbidsStatsServiceFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/stats/modelbids/ModelbidsStatsServiceFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
public class ModelbidsStatsServiceFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    private static final String MODELBIDS_STATISTICS_TEMP_REPORT_NAME =
            "modelbids_statistics_temp_report.xlsx";
    private static final long VENDOR_ID = 321;
    private static final long BRAND_ID = 110321;
    private static final long UID = 88005553535L;

    private final ModelbidsStatsService modelbidsStatsService;

    @Autowired
    public ModelbidsStatsServiceFunctionalTest(ModelbidsStatsService modelbidsStatsService) {
        this.modelbidsStatsService = modelbidsStatsService;
    }

    @Test
    void testTouchDayShowsClickTotal(@TempDir Path tempDir) throws Exception {

        Path actualReportPath = tempDir.resolve(MODELBIDS_STATISTICS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2020, 12, 7);
        LocalDate to = LocalDate.of(2020, 12, 9);

        ModelbidsStatsContext context = ModelbidsStatsContext.newBuilder()
                .setUid(UID)
                .setVendorId(VENDOR_ID)
                .setFrom(TimeUtil.toMillis(from))
                .setTo(TimeUtil.toMillis(to))
                .setScaleType(DAY)
                .setMetrics(List.of(SHOWS, CLICKS_TOTAL))
                .setDetailedByModel(false)
                .setDetailedByCategory(false)
                .setClickPaymentType(ClickPaymentType.FREE)
                .setPlatform(Collections.singleton(TOUCH))
                .setDetailedByPlacementGroupType(true)
                .build();

        try(OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            modelbidsStatsService.generateXlsxReport(context, os);
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testTouchDayShowsClickTotal/expected.xlsx"
        );
        verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
    }

    @Test
    void testTouchDayShowsClickTotalLastDayAbsent(@TempDir Path tempDir) throws Exception {

        Path actualReportPath = tempDir.resolve(MODELBIDS_STATISTICS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2020, 12, 7);
        LocalDate to = LocalDate.of(2020, 12, 13);

        ModelbidsStatsContext context = ModelbidsStatsContext.newBuilder()
                .setUid(UID)
                .setVendorId(VENDOR_ID)
                .setFrom(TimeUtil.toMillis(from))
                .setTo(TimeUtil.toMillis(to))
                .setScaleType(DAY)
                .setMetrics(List.of(SHOWS, CLICKS_TOTAL))
                .setDetailedByModel(false)
                .setDetailedByCategory(false)
                .setClickPaymentType(ClickPaymentType.FREE)
                .setPlatform(Collections.singleton(TOUCH))
                .setDetailedByPlacementGroupType(true)
                .build();

        try(OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            modelbidsStatsService.generateXlsxReport(context, os);
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testTouchDayShowsClickTotalLastDayAbsent/expected.xlsx"
        );
        verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
    }

    @Test
    void testDetailedByProduct(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(MODELBIDS_STATISTICS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2020, 12, 3);
        LocalDate to = LocalDate.of(2020, 12, 15);

        ModelbidsStatsContext context = ModelbidsStatsContext.newBuilder()
                .setUid(UID)
                .setVendorId(VENDOR_ID)
                .setFrom(TimeUtil.toMillis(from))
                .setTo(TimeUtil.toMillis(to))
                .setScaleType(DAY)
                .setMetrics(List.of(TARGET_ACTIONS, ORDERS, CTR_NEW, CPO, CR))
                .setDetailedByModel(true)
                .setDetailedByCategory(false)
                .setPlatformPlacementGroupTypes(PlatformPlacementGroupType.allValues())
                .setClickPaymentType(ClickPaymentType.ANY)
                .build();

        try(OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            modelbidsStatsService.generateXlsxReport(context, os);
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testDetailedByProduct/expected.xlsx"
        );
        verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));

    }

    @Test
    void testDetailedByCategory(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(MODELBIDS_STATISTICS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2020, 12, 3);
        LocalDate to = LocalDate.of(2020, 12, 15);

        ModelbidsStatsContext context = ModelbidsStatsContext.newBuilder()
                .setUid(UID)
                .setVendorId(VENDOR_ID)
                .setFrom(TimeUtil.toMillis(from))
                .setTo(TimeUtil.toMillis(to))
                .setScaleType(DAY)
                .setMetrics(List.of(TARGET_ACTIONS, ORDERS, CTR_NEW, CPO, CR))
                .setDetailedByModel(false)
                .setDetailedByCategory(true)
                .setPlatform(Set.of(TOUCH))
                .setPlatformPlacementGroupTypes(PlatformPlacementGroupType.allValues())
                .setClickPaymentType(ClickPaymentType.ANY)
                .build();

        try(OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            modelbidsStatsService.generateXlsxReport(context, os);
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testDetailedByCategory/expected.xlsx"
        );
        verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
    }

    @Test
    void testGetAllMetricsWeekScale(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(MODELBIDS_STATISTICS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2020, 12, 3);
        LocalDate to = LocalDate.of(2020, 12, 15);

        ModelbidsStatsContext context = ModelbidsStatsContext.newBuilder()
                .setUid(UID)
                .setVendorId(VENDOR_ID)
                .setFrom(TimeUtil.toMillis(from))
                .setTo(TimeUtil.toMillis(to))
                .setScaleType(WEEK)
                .setMetrics(List.of(
                        SHOWS,
                        CLICKS_TOTAL,
                        CLICKS_PAID,
                        CTR_NEW,
                        CHARGES,
                        AVG_BID,
                        AVG_CLICK_PRICE,
                        TARGET_ACTIONS,
                        CR,
                        ORDERS,
                        CPO
                ))
                .setDetailedByModel(false)
                .setDetailedByCategory(true)
                .setPlatformPlacementGroupTypes(PlatformPlacementGroupType.allValues())
                .setClickPaymentType(ClickPaymentType.ANY)
                .build();

        try(OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            modelbidsStatsService.generateXlsxReport(context, os);
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testGetAllMetricsWeekScale/expected.xlsx"
        );
        verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/modelbids/ModelbidsStatsServiceFunctionalTest/testHideOrdersAndCpo/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testHideOrdersAndCpo(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(MODELBIDS_STATISTICS_TEMP_REPORT_NAME);
        LocalDate from = LocalDate.of(2021, 9, 3);
        LocalDate to = LocalDate.of(2021, 9, 15);

        ModelbidsStatsContext context = ModelbidsStatsContext.newBuilder()
                .setUid(UID)
                .setVendorId(VENDOR_ID)
                .setFrom(TimeUtil.toMillis(from))
                .setTo(TimeUtil.toMillis(to))
                .setScaleType(DAY)
                .setMetrics(Set.of(ORDERS, CPO))
                .setDetailedByModel(true)
                .setDetailedByCategory(true)
                .setPlatform(Set.of(TOUCH))
                .setPlatformPlacementGroupTypes(PlatformPlacementGroupType.allValues())
                .setClickPaymentType(ClickPaymentType.ANY)
                .build();

        try(OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            modelbidsStatsService.generateXlsxReport(context, os);
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testHideOrdersAndCpo/expected.xlsx"
        );
        verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
    }

}
