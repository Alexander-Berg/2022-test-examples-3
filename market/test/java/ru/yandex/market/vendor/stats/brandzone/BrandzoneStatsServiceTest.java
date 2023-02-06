package ru.yandex.market.vendor.stats.brandzone;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.testcontainers.shaded.com.google.common.collect.Sets;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.vendor.asyncreport.model.AsyncReportTask;
import ru.yandex.vendor.asyncreport.model.AsyncReportType;
import ru.yandex.vendor.asyncreport.service.VendorAsyncReportsService;
import ru.yandex.vendor.statistic.report.impl.xlsx.XlsxReportContext;
import ru.yandex.vendor.statistic.report.impl.xlsx.XlsxReportGenerator;
import ru.yandex.vendor.statistic.report.model.Report;
import ru.yandex.vendor.stats.PlatformType;
import ru.yandex.vendor.stats.brandzone.BrandzoneMetricType;
import ru.yandex.vendor.stats.brandzone.model.BrandzoneStatsContext;
import ru.yandex.vendor.stats.brandzone.model.TrafficGroup;
import ru.yandex.vendor.stats.brandzone.report.BrandzoneReportGenerator;

import static ru.yandex.common.util.db.SortingOrder.DESC;
import static ru.yandex.market.vendor.stats.StatsTestUtils.verifyGeneratedReport;
import static ru.yandex.vendor.stats.PlatformType.DESKTOP;
import static ru.yandex.vendor.stats.StatisticsScaleType.DAY;
import static ru.yandex.vendor.stats.StatisticsScaleType.MONTH;
import static ru.yandex.vendor.stats.StatisticsScaleType.WEEK;
import static ru.yandex.vendor.stats.brandzone.BrandzoneMetricType.AUDIENCE;
import static ru.yandex.vendor.stats.brandzone.BrandzoneMetricType.SHOWS;
import static ru.yandex.vendor.stats.brandzone.model.TrafficGroup.AD;
import static ru.yandex.vendor.stats.brandzone.model.TrafficGroup.ORGANIC;

public class BrandzoneStatsServiceTest extends AbstractVendorPartnerFunctionalTest {

    private static final String BRANDZONE_STATS_TEMP_REPORT_NAME =
            "brandzone_stats_temp_report.xlsx";
    private static final long VENDOR_ID = 101;
    private static final long BRAND_ID = 999997;
    private static final long UID = 102;

    private final BrandzoneReportGenerator<XlsxReportContext> brandzoneReportGenerator;
    private final VendorAsyncReportsService vendorAsyncReportsService;
    private final XlsxReportGenerator xlsxReportGenerator;
    private final Clock clock;

    @Autowired
    public BrandzoneStatsServiceTest(BrandzoneReportGenerator<XlsxReportContext> brandzoneReportGenerator,
                                     VendorAsyncReportsService vendorAsyncReportsService,
                                     XlsxReportGenerator xlsxReportGenerator,
                                     Clock clock) {
        this.brandzoneReportGenerator = brandzoneReportGenerator;
        this.vendorAsyncReportsService = vendorAsyncReportsService;
        this.xlsxReportGenerator = xlsxReportGenerator;
        this.clock = clock;
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brandzone/BrandzoneStatsServiceTest/testShowsDayScaleOrganicDesktop/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brandzone/BrandzoneStatsServiceTest/testShowsDayScaleOrganicDesktop/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testShowsDayScaleOrganicDesktop(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(BRANDZONE_STATS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2020, 12, 7);
        LocalDate to = LocalDate.of(2020, 12, 18);

        TreeSet<BrandzoneMetricType> metricTypes = Stream.of(SHOWS)
                .collect(Collectors.toCollection(Sets::newTreeSet));

        BrandzoneStatsContext context = BrandzoneStatsContext.builder()
                .setBrandId(BRAND_ID)
                .setUid(UID)
                .setScaleType(DAY)
                .setTrafficGroups(Set.of(ORGANIC))
                .setFrom(from)
                .setTo(to)
                .setReport(true)
                .setPlatforms(Set.of(DESKTOP))
                .setSortOrder(DESC)
                .setMetrics(metricTypes)
                .build();

        AsyncReportTask<BrandzoneStatsContext> task = vendorAsyncReportsService.createAsyncReportTask(
                VENDOR_ID,
                AsyncReportType.BRANDZONE_STATS,
                context,
                UID
        );

        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            Pair<Report, XlsxReportContext> report = brandzoneReportGenerator.generate(task.getReportParams(), os);
            XlsxReportContext reportContext = report.getValue();
            reportContext.setOs(os);
            xlsxReportGenerator.generate(report.getKey(), report.getValue());
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testShowsDayScaleOrganicDesktop/expected.xlsx"
        );
        verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brandzone/BrandzoneStatsServiceTest/testShowsAudienceWeekAllTrafficAndPlatforms/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brandzone/BrandzoneStatsServiceTest/testShowsAudienceWeekAllTrafficAndPlatforms/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testShowsAudienceWeekAllTrafficAndPlatforms(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(BRANDZONE_STATS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2020, 12, 7);
        LocalDate to = LocalDate.of(2020, 12, 18);

        TreeSet<BrandzoneMetricType> metricTypes = Stream.of(SHOWS, AUDIENCE)
                .collect(Collectors.toCollection(Sets::newTreeSet));

        BrandzoneStatsContext context = BrandzoneStatsContext.builder()
                .setBrandId(BRAND_ID)
                .setUid(UID)
                .setScaleType(WEEK)
                .setTrafficGroups(Set.of(TrafficGroup.values()))
                .setFrom(from)
                .setTo(to)
                .setReport(true)
                .setPlatforms(Set.of(PlatformType.values()))
                .setSortOrder(DESC)
                .setMetrics(metricTypes)
                .build();

        AsyncReportTask<BrandzoneStatsContext> task = vendorAsyncReportsService.createAsyncReportTask(
                VENDOR_ID,
                AsyncReportType.BRANDZONE_STATS,
                context,
                UID
        );

        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            Pair<Report, XlsxReportContext> report = brandzoneReportGenerator.generate(task.getReportParams(), os);
            XlsxReportContext reportContext = report.getValue();
            reportContext.setOs(os);
            xlsxReportGenerator.generate(report.getKey(), report.getValue());
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testShowsAudienceWeekAllTrafficAndPlatforms/expected.xlsx"
        );
        verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brandzone/BrandzoneStatsServiceTest/testAllMetricsMonthDesktopAd/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brandzone/BrandzoneStatsServiceTest/testAllMetricsMonthDesktopAd/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testAllMetricsMonthDesktopAd(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(BRANDZONE_STATS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2020, 12, 1);
        LocalDate to = LocalDate.of(2020, 12, 31);

        BrandzoneStatsContext context = BrandzoneStatsContext.builder()
                .setBrandId(BRAND_ID)
                .setUid(UID)
                .setScaleType(MONTH)
                .setTrafficGroups(Set.of(AD))
                .setFrom(from)
                .setTo(to)
                .setReport(true)
                .setPlatforms(Set.of(DESKTOP))
                .setSortOrder(DESC)
                .setMetrics(BrandzoneMetricType.getMetricsAsSet())
                .build();

        AsyncReportTask<BrandzoneStatsContext> task = vendorAsyncReportsService.createAsyncReportTask(
                VENDOR_ID,
                AsyncReportType.BRANDZONE_STATS,
                context,
                UID
        );

        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            Pair<Report, XlsxReportContext> report = brandzoneReportGenerator.generate(task.getReportParams(), os);
            XlsxReportContext reportContext = report.getValue();
            reportContext.setOs(os);
            xlsxReportGenerator.generate(report.getKey(), report.getValue());
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testAllMetricsMonthDesktopAd/expected.xlsx"
        );
        verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brandzone/BrandzoneStatsServiceTest/testDefaultMetricsWithoutTrafficGroup/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brandzone/BrandzoneStatsServiceTest/testDefaultMetricsWithoutTrafficGroup/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testDefaultMetricsWithoutTrafficGroup(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(BRANDZONE_STATS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2020, 12, 1);
        LocalDate to = LocalDate.of(2020, 12, 31);

        BrandzoneStatsContext context = BrandzoneStatsContext.builder()
                .setBrandId(BRAND_ID)
                .setUid(UID)
                .setScaleType(DAY)
                .setFrom(from)
                .setTo(to)
                .setReport(true)
                .setPlatforms(Set.of(DESKTOP))
                .build();

        AsyncReportTask<BrandzoneStatsContext> task = vendorAsyncReportsService.createAsyncReportTask(
                VENDOR_ID,
                AsyncReportType.BRANDZONE_STATS,
                context,
                UID
        );

        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            Pair<Report, XlsxReportContext> report = brandzoneReportGenerator.generate(task.getReportParams(), os);
            XlsxReportContext reportContext = report.getValue();
            reportContext.setOs(os);
            xlsxReportGenerator.generate(report.getKey(), report.getValue());
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testDefaultMetricsWithoutTrafficGroup/expected.xlsx"
        );
        verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
    }

}
