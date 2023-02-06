package ru.yandex.market.vendor.stats.modelbidsincut;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.vendor.asyncreport.model.AsyncReportTask;
import ru.yandex.vendor.statistic.report.impl.xlsx.XlsxReportContext;
import ru.yandex.vendor.statistic.report.impl.xlsx.XlsxReportGenerator;
import ru.yandex.vendor.stats.PlatformType;
import ru.yandex.vendor.stats.StatisticsScaleType;
import ru.yandex.vendor.stats.modelbidsincut.ModelBidsIncutGroupingType;
import ru.yandex.vendor.stats.modelbidsincut.ModelBidsIncutMetricType;
import ru.yandex.vendor.stats.modelbidsincut.ModelBidsIncutStatsContext;
import ru.yandex.vendor.stats.modelbidsincut.report.ModelBidsIncutStatsReport;
import ru.yandex.vendor.stats.modelbidsincut.report.ModelBidsIncutStatsReportXlsxGenerator;
import ru.yandex.vendor.stats.modelbidsincut.service.ModelBidsIncutStatsService;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static ru.yandex.market.vendor.stats.StatsTestUtils.verifyGeneratedReport;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/stats/modelbidsincut/ModelBidsIncutStatsReportGeneratorTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/stats/modelbidsincut/ModelBidsIncutStatsReportGeneratorTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
public class ModelBidsIncutStatsReportGeneratorTest extends AbstractVendorPartnerFunctionalTest {

    private static final String MODELBIDS_INCUT_STATS_TEMP_REPORT_NAME =
            "modelbids_incut_stats_temp_report_name.xlsx";
    private static final long VENDOR_ID = 321;
    private static final long UID = 88005553535L;

    private final ModelBidsIncutStatsService modelBidsIncutStatsService;
    private final ModelBidsIncutStatsReportXlsxGenerator<XlsxReportContext> modelBidsIncutStatsReportXlsxGenerator;
    private final XlsxReportGenerator xlsxReportGenerator;
    private final WireMockServer advIncutMock;

    @Autowired
    public ModelBidsIncutStatsReportGeneratorTest(
            ModelBidsIncutStatsService modelBidsIncutStatsService,
            ModelBidsIncutStatsReportXlsxGenerator<XlsxReportContext> modelBidsIncutStatsReportXlsxGenerator,
            XlsxReportGenerator xlsxReportGenerator, WireMockServer advIncutMock) {
        this.modelBidsIncutStatsService = modelBidsIncutStatsService;
        this.modelBidsIncutStatsReportXlsxGenerator = modelBidsIncutStatsReportXlsxGenerator;
        this.xlsxReportGenerator = xlsxReportGenerator;
        this.advIncutMock = advIncutMock;
    }

    @Test
    void testAllMetricsAllPlatformsDayScale(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(MODELBIDS_INCUT_STATS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2020, 12, 7);
        LocalDate to = LocalDate.of(2020, 12, 12);

        Set<ModelBidsIncutMetricType> metrics = Set.of(ModelBidsIncutMetricType.values());

        Set<PlatformType> platformTypes = Set.of(PlatformType.DESKTOP, PlatformType.TOUCH, PlatformType.APPLICATION);
        ModelBidsIncutStatsContext context =
                ModelBidsIncutStatsContext.newBuilder()
                        .setVendorId(VENDOR_ID)
                        .setUid(UID)
                        .setMetrics(metrics)
                        .setFrom(from)
                        .setTo(to)
                        .setScaleType(StatisticsScaleType.DAY)
                        .setIncuts(Set.of(1L, 2L))
                        .setPlatforms(platformTypes)
                        .setGroupingType(ModelBidsIncutGroupingType.NO_GROUPING)
                        .build();

        AsyncReportTask<ModelBidsIncutStatsContext> task =
                modelBidsIncutStatsService.startAsyncXlsxReportGeneration(context, context.getUid());

        stubAdvIncutsList();

        ModelBidsIncutStatsReport<XlsxReportContext> report =
                modelBidsIncutStatsReportXlsxGenerator.generate(task.getReportParams());
        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            XlsxReportContext reportContext = report.getContext();
            reportContext.setOs(os);
            xlsxReportGenerator.generate(report.getReport(), report.getContext());
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testAllMetricsAllPlatformsDayScale/expected.xlsx"
        );
        verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
    }

    @Test
    void testAllMetricsAllPlatformsDayScaleShowCategoryGrouping(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(MODELBIDS_INCUT_STATS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2020, 12, 7);
        LocalDate to = LocalDate.of(2020, 12, 12);

        Set<ModelBidsIncutMetricType> metrics = Set.of(ModelBidsIncutMetricType.values());

        Set<PlatformType> platformTypes = Set.of(PlatformType.DESKTOP, PlatformType.TOUCH, PlatformType.APPLICATION);
        ModelBidsIncutStatsContext context =
                ModelBidsIncutStatsContext.newBuilder()
                        .setVendorId(VENDOR_ID)
                        .setUid(UID)
                        .setMetrics(metrics)
                        .setFrom(from)
                        .setTo(to)
                        .setScaleType(StatisticsScaleType.DAY)
                        .setIncuts(Set.of(1L, 2L))
                        .setPlatforms(platformTypes)
                        .setGroupingType(ModelBidsIncutGroupingType.SHOW_CATEGORY)
                        .build();

        AsyncReportTask<ModelBidsIncutStatsContext> task =
                modelBidsIncutStatsService.startAsyncXlsxReportGeneration(context, context.getUid());

        stubAdvIncutsList();

        ModelBidsIncutStatsReport<XlsxReportContext> report =
                modelBidsIncutStatsReportXlsxGenerator.generate(task.getReportParams());
        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            XlsxReportContext reportContext = report.getContext();
            reportContext.setOs(os);
            xlsxReportGenerator.generate(report.getReport(), report.getContext());
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testAllMetricsAllPlatformsDayScaleShowCategoryGrouping/expected.xlsx"
        );
        verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
    }

    @Test
    void testAllMetricsAllPlatformsMonthScale(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(MODELBIDS_INCUT_STATS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2020, 12, 7);
        LocalDate to = LocalDate.of(2021, 1, 3);

        Set<ModelBidsIncutMetricType> metrics = Set.of(ModelBidsIncutMetricType.values());

        Set<PlatformType> platformTypes = Set.of(PlatformType.DESKTOP, PlatformType.TOUCH, PlatformType.APPLICATION);
        ModelBidsIncutStatsContext context =
                ModelBidsIncutStatsContext.newBuilder()
                        .setVendorId(VENDOR_ID)
                        .setUid(UID)
                        .setMetrics(metrics)
                        .setFrom(from)
                        .setTo(to)
                        .setScaleType(StatisticsScaleType.MONTH)
                        .setIncuts(Set.of(1L, 2L))
                        .setPlatforms(platformTypes)
                        .setGroupingType(ModelBidsIncutGroupingType.NO_GROUPING)
                        .build();

        AsyncReportTask<ModelBidsIncutStatsContext> task =
                modelBidsIncutStatsService.startAsyncXlsxReportGeneration(context, context.getUid());

        stubAdvIncutsList();

        ModelBidsIncutStatsReport<XlsxReportContext> report =
                modelBidsIncutStatsReportXlsxGenerator.generate(task.getReportParams());
        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            XlsxReportContext reportContext = report.getContext();
            reportContext.setOs(os);
            xlsxReportGenerator.generate(report.getReport(), report.getContext());
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testAllMetricsAllPlatformsMonthScale/expected.xlsx"
        );
        verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
    }

    @Test
    void testAllMetricsAllPlatformsWeekScale(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(MODELBIDS_INCUT_STATS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2020, 12, 7);
        LocalDate to = LocalDate.of(2021, 1, 3);

        Set<ModelBidsIncutMetricType> metrics = Set.of(ModelBidsIncutMetricType.values());

        Set<PlatformType> platformTypes = Set.of(PlatformType.DESKTOP, PlatformType.TOUCH, PlatformType.APPLICATION);
        ModelBidsIncutStatsContext context =
                ModelBidsIncutStatsContext.newBuilder()
                        .setVendorId(VENDOR_ID)
                        .setUid(UID)
                        .setMetrics(metrics)
                        .setFrom(from)
                        .setTo(to)
                        .setScaleType(StatisticsScaleType.WEEK)
                        .setIncuts(Set.of(1L, 2L))
                        .setPlatforms(platformTypes)
                        .setGroupingType(ModelBidsIncutGroupingType.NO_GROUPING)
                        .build();

        AsyncReportTask<ModelBidsIncutStatsContext> task =
                modelBidsIncutStatsService.startAsyncXlsxReportGeneration(context, context.getUid());

        stubAdvIncutsList();

        ModelBidsIncutStatsReport<XlsxReportContext> report =
                modelBidsIncutStatsReportXlsxGenerator.generate(task.getReportParams());
        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            XlsxReportContext reportContext = report.getContext();
            reportContext.setOs(os);
            xlsxReportGenerator.generate(report.getReport(), report.getContext());
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testAllMetricsAllPlatformsWeekScale/expected.xlsx"
        );
        verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
    }

    @Test
    void testEmptyReport(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(MODELBIDS_INCUT_STATS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2010, 12, 7);
        LocalDate to = LocalDate.of(2010, 12, 12);

        Set<ModelBidsIncutMetricType> metrics = Set.of(ModelBidsIncutMetricType.values());

        Set<PlatformType> platformTypes = Set.of(PlatformType.DESKTOP, PlatformType.TOUCH, PlatformType.APPLICATION);
        ModelBidsIncutStatsContext context =
                ModelBidsIncutStatsContext.newBuilder()
                        .setVendorId(VENDOR_ID)
                        .setUid(UID)
                        .setMetrics(metrics)
                        .setFrom(from)
                        .setTo(to)
                        .setScaleType(StatisticsScaleType.DAY)
                        .setIncuts(Set.of(1L, 2L))
                        .setPlatforms(platformTypes)
                        .setGroupingType(ModelBidsIncutGroupingType.NO_GROUPING)
                        .build();

        AsyncReportTask<ModelBidsIncutStatsContext> task =
                modelBidsIncutStatsService.startAsyncXlsxReportGeneration(context, context.getUid());

        stubAdvIncutsList();

        ModelBidsIncutStatsReport<XlsxReportContext> report =
                modelBidsIncutStatsReportXlsxGenerator.generate(task.getReportParams());
        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            XlsxReportContext reportContext = report.getContext();
            reportContext.setOs(os);
            xlsxReportGenerator.generate(report.getReport(), report.getContext());
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testEmptyReport/expected.xlsx"
        );
        verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
    }

    @Test
    void testEmptyReportShowCategoryGrouping(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(MODELBIDS_INCUT_STATS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2010, 12, 7);
        LocalDate to = LocalDate.of(2010, 12, 12);

        Set<ModelBidsIncutMetricType> metrics = Set.of(ModelBidsIncutMetricType.values());

        Set<PlatformType> platformTypes = Set.of(PlatformType.DESKTOP, PlatformType.TOUCH, PlatformType.APPLICATION);
        ModelBidsIncutStatsContext context =
                ModelBidsIncutStatsContext.newBuilder()
                        .setVendorId(VENDOR_ID)
                        .setUid(UID)
                        .setMetrics(metrics)
                        .setFrom(from)
                        .setTo(to)
                        .setScaleType(StatisticsScaleType.DAY)
                        .setIncuts(Set.of(1L, 2L))
                        .setPlatforms(platformTypes)
                        .setGroupingType(ModelBidsIncutGroupingType.SHOW_CATEGORY)
                        .build();

        AsyncReportTask<ModelBidsIncutStatsContext> task =
                modelBidsIncutStatsService.startAsyncXlsxReportGeneration(context, context.getUid());

        stubAdvIncutsList();

        ModelBidsIncutStatsReport<XlsxReportContext> report =
                modelBidsIncutStatsReportXlsxGenerator.generate(task.getReportParams());
        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            XlsxReportContext reportContext = report.getContext();
            reportContext.setOs(os);
            xlsxReportGenerator.generate(report.getReport(), report.getContext());
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testEmptyReportShowCategoryGrouping/expected.xlsx"
        );
        verifyGeneratedReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
    }

    private void stubAdvIncutsList() {
        advIncutMock.stubFor(WireMock.get("/api/v1/incuts/list?vendorId=321&datasourceId=1321&uid=88005553535")
                .willReturn(aResponse().withBody(getStringResource("/incutsListResponse.json"))));
    }

}
