package ru.yandex.market.vendor.stats.modelbids.banners;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.vendor.statistic.report.impl.xlsx.XlsxReportContext;
import ru.yandex.vendor.statistic.report.impl.xlsx.XlsxReportGenerator;
import ru.yandex.vendor.statistic.report.model.Report;
import ru.yandex.vendor.stats.StatisticsScaleType;
import ru.yandex.vendor.stats.modelbidbanner.model.MarketModelBidsBannerMetric;
import ru.yandex.vendor.stats.modelbidbanner.model.MarketModelBidsBannersContext;
import ru.yandex.vendor.stats.modelbidbanner.report.impl.MarketModelBidsBannerReportGenerator;

import static ru.yandex.market.vendor.stats.StatsTestUtils.verifyGeneratedReport;
import static ru.yandex.vendor.stats.PlatformType.DESKTOP;
import static ru.yandex.vendor.stats.modelbidbanner.model.MarketModelBidsBannerMetric.CLICKS;
import static ru.yandex.vendor.stats.modelbidbanner.model.MarketModelBidsBannerMetric.SHOWS;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/stats/modelbids/banners/MarketModelBidsBannerReportGeneratorTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/stats/modelbids/banners/MarketModelBidsBannerReportGeneratorTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
public class MarketModelBidsBannerReportGeneratorTest extends AbstractVendorPartnerFunctionalTest {

    private static final String REPORT_NAME =
            "market_modelbids_banners_report.xlsx";

    private final MarketModelBidsBannerReportGenerator modelBidsBannerReportGenerator;
    private final XlsxReportGenerator xlsxReportGenerator;
    private final Clock clock;

    @Autowired
    public MarketModelBidsBannerReportGeneratorTest(
            MarketModelBidsBannerReportGenerator modelBidsBannerReportGenerator,
            XlsxReportGenerator xlsxReportGenerator,
            Clock clock
    ) {
        this.modelBidsBannerReportGenerator = modelBidsBannerReportGenerator;
        this.xlsxReportGenerator = xlsxReportGenerator;
        this.clock = clock;
    }

    @Test
    void testGenerateReportWeekAllMetrics(@TempDir Path dir) throws Exception {

        Path actualReportPath = dir.resolve(REPORT_NAME);

        LocalDate now = LocalDate.of(2021, 1, 1);
        LocalDate to = LocalDate.of(2021, 12, 1);

        MarketModelBidsBannersContext context = MarketModelBidsBannersContext.builder()
                .withFrom(now)
                .withTo(to)
                .withScaleType(StatisticsScaleType.WEEK)
                .withVendorId(321)
                .withMetrics(MarketModelBidsBannerMetric.REPORT_METRICS)
                .build();

        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            Pair<Report, XlsxReportContext> report =
                    modelBidsBannerReportGenerator.generate(context, os);
            XlsxReportContext reportContext = report.getValue();
            reportContext.setOs(os);
            xlsxReportGenerator.generate(report.getKey(), report.getValue());
        }

        InputStreamResource expectedReport = getInputStreamResource(
                "/testGenerateReportWeekAllMetrics/expected.xlsx"
        );
        verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));

    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/modelbids/banners/MarketModelBidsBannerReportGeneratorTest/testDayShowsClicksDesktopMultiBrand/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/modelbids/banners/MarketModelBidsBannerReportGeneratorTest/testDayShowsClicksDesktopMultiBrand/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testDayShowsClicksDesktopMultiBrand(@TempDir Path dir) throws Exception {

        Path actualReportPath = dir.resolve(REPORT_NAME);

        LocalDate now = LocalDate.of(2021, 1, 1);
        LocalDate to = LocalDate.of(2021, 12, 1);

        MarketModelBidsBannersContext context = MarketModelBidsBannersContext.builder()
                .withFrom(now)
                .withTo(to)
                .withScaleType(StatisticsScaleType.DAY)
                .withVendorId(321)
                .withBrandVendorsIds(Set.of(321L, 322L, 323L))
                .withMetrics(new TreeSet<>(Set.of(SHOWS, CLICKS)))
                .withPlatforms(new TreeSet<>(Set.of(DESKTOP)))
                .build();

        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            Pair<Report, XlsxReportContext> report =
                    modelBidsBannerReportGenerator.generate(context, os);
            XlsxReportContext reportContext = report.getValue();
            reportContext.setOs(os);
            xlsxReportGenerator.generate(report.getKey(), report.getValue());
        }

        InputStreamResource expectedReport = getInputStreamResource(
                "/testDayShowsClicksDesktopMultiBrand/expected.xlsx"
        );
        verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
    }

    @Test
    void testDayShowsClickDesktop(@TempDir Path dir) throws Exception {

        Path actualReportPath = dir.resolve(REPORT_NAME);

        LocalDate now = LocalDate.of(2021, 1, 1);
        LocalDate to = LocalDate.of(2021, 12, 1);

        MarketModelBidsBannersContext context = MarketModelBidsBannersContext.builder()
                .withFrom(now)
                .withTo(to)
                .withScaleType(StatisticsScaleType.DAY)
                .withVendorId(321)
                .withMetrics(new TreeSet<>(Set.of(SHOWS, CLICKS)))
                .withPlatforms(new TreeSet<>(Set.of(DESKTOP)))
                .build();

        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            Pair<Report, XlsxReportContext> report =
                    modelBidsBannerReportGenerator.generate(context, os);
            XlsxReportContext reportContext = report.getValue();
            reportContext.setOs(os);
            xlsxReportGenerator.generate(report.getKey(), report.getValue());
        }

        InputStreamResource expectedReport = getInputStreamResource(
                "/testDayShowsClickDesktop/expected.xlsx"
        );
        verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));

    }

    @Test
    void testMonthAllMetricsDesktop(@TempDir Path dir) throws Exception {

        Path actualReportPath = dir.resolve(REPORT_NAME);

        LocalDate now = LocalDate.of(2021, 1, 1);
        LocalDate to = LocalDate.of(2021, 12, 1);

        MarketModelBidsBannersContext context = MarketModelBidsBannersContext.builder()
                .withFrom(now)
                .withTo(to)
                .withScaleType(StatisticsScaleType.MONTH)
                .withVendorId(321)
                .withMetrics(MarketModelBidsBannerMetric.REPORT_METRICS)
                .withPlatforms(new TreeSet<>(Set.of(DESKTOP)))
                .build();

        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            Pair<Report, XlsxReportContext> report =
                    modelBidsBannerReportGenerator.generate(context, os);
            XlsxReportContext reportContext = report.getValue();
            reportContext.setOs(os);
            xlsxReportGenerator.generate(report.getKey(), report.getValue());
        }

        InputStreamResource expectedReport = getInputStreamResource(
                "/testMonthAllMetricsDesktop/expected.xlsx"
        );
        verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));

    }

    @Test
    void testReportWithBannerIds(@TempDir Path dir) throws Exception {

        Path actualReportPath = dir.resolve(REPORT_NAME);

        LocalDate now = LocalDate.of(2021, 1, 1);
        LocalDate to = LocalDate.of(2021, 12, 1);

        MarketModelBidsBannersContext context = MarketModelBidsBannersContext.builder()
                .withFrom(now)
                .withTo(to)
                .withVendorId(321)
                .withMetrics(MarketModelBidsBannerMetric.REPORT_METRICS)
                .withBannerIds(Set.of(1L, 2L, 3L))
                .withPlatforms(new TreeSet<>(Set.of(DESKTOP)))
                .build();

        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            Pair<Report, XlsxReportContext> report =
                    modelBidsBannerReportGenerator.generate(context, os);
            XlsxReportContext reportContext = report.getValue();
            reportContext.setOs(os);
            xlsxReportGenerator.generate(report.getKey(), report.getValue());
        }

        InputStreamResource expectedReport = getInputStreamResource(
                "/testReportWithBannerIds/expected.xlsx"
        );
        verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));

    }


}
