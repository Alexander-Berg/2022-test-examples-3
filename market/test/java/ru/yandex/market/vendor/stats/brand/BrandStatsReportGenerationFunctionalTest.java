package ru.yandex.market.vendor.stats.brand;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.vendor.stats.StatisticsScaleType;
import ru.yandex.vendor.stats.brand.service.BrandStatisticsReportGenerator;
import ru.yandex.vendor.stats.brand.service.model.BrandStatisticsReportParams;

import static ru.yandex.market.vendor.stats.StatsTestUtils.verifyGeneratedMultiSheetReport;

public class BrandStatsReportGenerationFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    private static final String BRAND_STATISTICS_TEMP_REPORT_NAME = "brand_statistics_temp_report.xlsx";
    private static final long VENDOR_ID = 321;
    private static final long CATEGORY_ID = 666;
    private static final long UID = 102;
    private static final Set<Long> CHOSEN_BRAND_IDS = Set.of(110326L);

    private final BrandStatisticsReportGenerator brandStatisticsReportGenerator;

    @Autowired
    public BrandStatsReportGenerationFunctionalTest(BrandStatisticsReportGenerator brandStatisticsReportGenerator) {
        this.brandStatisticsReportGenerator = brandStatisticsReportGenerator;
    }

    @Disabled
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brand/BrandStatsReportGenerationFunctionalTest/testDayScaleNoTopCurrentBrand/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brand/BrandStatsReportGenerationFunctionalTest/testDayScaleNoTopCurrentBrand/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testDayScaleNoTopCurrentBrand(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(BRAND_STATISTICS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2021, 9, 1);
        LocalDate to = LocalDate.of(2021, 9, 10);

        BrandStatisticsReportParams reportParams = BrandStatisticsReportParams.builder()
                .withFrom(from)
                .withTo(to)
                .withTop5(false)
                .withScaleType(StatisticsScaleType.DAY)
                .withYoy(false)
                .withVendorId(VENDOR_ID)
                .withCategoryId(CATEGORY_ID)
                .withUid(UID)
                .build();


        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            brandStatisticsReportGenerator.writeXlsxReport(reportParams, os);
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testDayScaleNoTopCurrentBrand/expected.xlsx"
        );
        verifyGeneratedMultiSheetReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
    }

    @Disabled
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brand/BrandStatsReportGenerationFunctionalTest/testWeekScaleNoTopCurrentBrand/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brand/BrandStatsReportGenerationFunctionalTest/testWeekScaleNoTopCurrentBrand/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testWeekScaleNoTopCurrentBrand(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(BRAND_STATISTICS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2021, 9, 1);
        LocalDate to = LocalDate.of(2021, 9, 10);

        BrandStatisticsReportParams reportParams = BrandStatisticsReportParams.builder()
                .withFrom(from)
                .withTo(to)
                .withTop5(false)
                .withScaleType(StatisticsScaleType.WEEK)
                .withYoy(false)
                .withVendorId(VENDOR_ID)
                .withCategoryId(CATEGORY_ID)
                .withUid(UID)
                .build();


        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            brandStatisticsReportGenerator.writeXlsxReport(reportParams, os);
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testWeekScaleNoTopCurrentBrand/expected.xlsx"
        );
        verifyGeneratedMultiSheetReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
    }

    @Disabled
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brand/BrandStatsReportGenerationFunctionalTest/testDayScaleCurrentBrandInTopWithOtherBrands/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brand/BrandStatsReportGenerationFunctionalTest/testDayScaleCurrentBrandInTopWithOtherBrands/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testDayScaleCurrentBrandInTopWithOtherBrands(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(BRAND_STATISTICS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2021, 9, 1);
        LocalDate to = LocalDate.of(2021, 9, 10);

        BrandStatisticsReportParams reportParams = BrandStatisticsReportParams.builder()
                .withFrom(from)
                .withTo(to)
                .withTop5(true)
                .withScaleType(StatisticsScaleType.DAY)
                .withYoy(false)
                .withVendorId(VENDOR_ID)
                .withCategoryId(CATEGORY_ID)
                .withChosenBrandIds(CHOSEN_BRAND_IDS)
                .withUid(UID)
                .build();


        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            brandStatisticsReportGenerator.writeXlsxReport(reportParams, os);
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testDayScaleCurrentBrandInTopWithOtherBrands/expected.xlsx"
        );
        verifyGeneratedMultiSheetReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));

    }

    @Disabled
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brand/BrandStatsReportGenerationFunctionalTest/testDayScaleCurrentBrandInTopWithOtherBrandsHaveUnknownBrand/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brand/BrandStatsReportGenerationFunctionalTest/testDayScaleCurrentBrandInTopWithOtherBrandsHaveUnknownBrand/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testDayScaleCurrentBrandInTopWithOtherBrandsHaveUnknownBrand(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(BRAND_STATISTICS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2021, 9, 1);
        LocalDate to = LocalDate.of(2021, 9, 10);

        BrandStatisticsReportParams reportParams = BrandStatisticsReportParams.builder()
                .withFrom(from)
                .withTo(to)
                .withTop5(true)
                .withScaleType(StatisticsScaleType.DAY)
                .withYoy(false)
                .withVendorId(VENDOR_ID)
                .withCategoryId(CATEGORY_ID)
                .withChosenBrandIds(CHOSEN_BRAND_IDS)
                .withUid(UID)
                .build();


        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            brandStatisticsReportGenerator.writeXlsxReport(reportParams, os);
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testDayScaleCurrentBrandInTopWithOtherBrandsHaveUnknownBrand/expected.xlsx"
        );
        verifyGeneratedMultiSheetReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));

    }

    @Disabled
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brand/BrandStatsReportGenerationFunctionalTest/testWeekScaleCurrentBrandInTopWithOtherBrands/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brand/BrandStatsReportGenerationFunctionalTest/testWeekScaleCurrentBrandInTopWithOtherBrands/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testWeekScaleCurrentBrandInTopWithOtherBrands(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(BRAND_STATISTICS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2021, 9, 1);
        LocalDate to = LocalDate.of(2021, 9, 10);

        BrandStatisticsReportParams reportParams = BrandStatisticsReportParams.builder()
                .withFrom(from)
                .withTo(to)
                .withTop5(true)
                .withScaleType(StatisticsScaleType.WEEK)
                .withYoy(false)
                .withVendorId(VENDOR_ID)
                .withCategoryId(CATEGORY_ID)
                .withUid(UID)
                .withChosenBrandIds(CHOSEN_BRAND_IDS)
                .build();


        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            brandStatisticsReportGenerator.writeXlsxReport(reportParams, os);
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testWeekScaleCurrentBrandInTopWithOtherBrands/expected.xlsx"
        );
        verifyGeneratedMultiSheetReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
    }

    @Disabled
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brand/BrandStatsReportGenerationFunctionalTest/testMonthScaleCurrentBrandInTopWithOtherBrands/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brand/BrandStatsReportGenerationFunctionalTest/testMonthScaleCurrentBrandInTopWithOtherBrands/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testMonthScaleCurrentBrandInTopWithOtherBrands(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(BRAND_STATISTICS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2021, 9, 1);
        LocalDate to = LocalDate.of(2021, 10, 10);

        BrandStatisticsReportParams reportParams = BrandStatisticsReportParams.builder()
                .withFrom(from)
                .withTo(to)
                .withTop5(true)
                .withScaleType(StatisticsScaleType.MONTH)
                .withYoy(false)
                .withVendorId(VENDOR_ID)
                .withCategoryId(CATEGORY_ID)
                .withUid(UID)
                .withChosenBrandIds(CHOSEN_BRAND_IDS)
                .build();


        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            brandStatisticsReportGenerator.writeXlsxReport(reportParams, os);
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testMonthScaleCurrentBrandInTopWithOtherBrands/expected.xlsx"
        );
        verifyGeneratedMultiSheetReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
    }

    @Disabled
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brand/BrandStatsReportGenerationFunctionalTest/testYearScaleCurrentBrandInTopWithOtherBrands/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brand/BrandStatsReportGenerationFunctionalTest/testYearScaleCurrentBrandInTopWithOtherBrands/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testYearScaleCurrentBrandInTopWithOtherBrands(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(BRAND_STATISTICS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2021, 9, 1);
        LocalDate to = LocalDate.of(2021, 9, 10);

        BrandStatisticsReportParams reportParams = BrandStatisticsReportParams.builder()
                .withFrom(from)
                .withTo(to)
                .withTop5(true)
                .withScaleType(StatisticsScaleType.YEAR)
                .withYoy(false)
                .withVendorId(VENDOR_ID)
                .withCategoryId(CATEGORY_ID)
                .withUid(UID)
                .withChosenBrandIds(CHOSEN_BRAND_IDS)
                .build();


        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            brandStatisticsReportGenerator.writeXlsxReport(reportParams, os);
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testYearScaleCurrentBrandInTopWithOtherBrands/expected.xlsx"
        );
        verifyGeneratedMultiSheetReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));
    }

    @Disabled
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brand/BrandStatsReportGenerationFunctionalTest/testDayScaleCurrentBrandInTopWithOtherBrandsYoy/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brand/BrandStatsReportGenerationFunctionalTest/testDayScaleCurrentBrandInTopWithOtherBrandsYoy/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testDayScaleCurrentBrandInTopWithOtherBrandsYoy(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(BRAND_STATISTICS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2021, 9, 1);
        LocalDate to = LocalDate.of(2021, 9, 10);

        BrandStatisticsReportParams reportParams = BrandStatisticsReportParams.builder()
                .withFrom(from)
                .withTo(to)
                .withTop5(true)
                .withScaleType(StatisticsScaleType.DAY)
                .withYoy(true)
                .withVendorId(VENDOR_ID)
                .withCategoryId(CATEGORY_ID)
                .withChosenBrandIds(CHOSEN_BRAND_IDS)
                .withUid(UID)
                .build();

        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            brandStatisticsReportGenerator.writeXlsxReport(reportParams, os);
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testDayScaleCurrentBrandInTopWithOtherBrandsYoy/expected.xlsx"
        );
        verifyGeneratedMultiSheetReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));

    }

    @Disabled
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brand/BrandStatsReportGenerationFunctionalTest/testWeekScaleCurrentBrandInTopWithOtherBrandsYoy/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brand/BrandStatsReportGenerationFunctionalTest/testWeekScaleCurrentBrandInTopWithOtherBrandsYoy/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testWeekScaleCurrentBrandInTopWithOtherBrandsYoy(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(BRAND_STATISTICS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2021, 9, 1);
        LocalDate to = LocalDate.of(2021, 9, 10);

        BrandStatisticsReportParams reportParams = BrandStatisticsReportParams.builder()
                .withFrom(from)
                .withTo(to)
                .withTop5(true)
                .withScaleType(StatisticsScaleType.WEEK)
                .withYoy(true)
                .withVendorId(VENDOR_ID)
                .withCategoryId(CATEGORY_ID)
                .withChosenBrandIds(CHOSEN_BRAND_IDS)
                .withUid(UID)
                .build();

        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            brandStatisticsReportGenerator.writeXlsxReport(reportParams, os);
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testWeekScaleCurrentBrandInTopWithOtherBrandsYoy/expected.xlsx"
        );
        verifyGeneratedMultiSheetReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));

    }

    @Disabled
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brand/BrandStatsReportGenerationFunctionalTest/testMonthScaleCurrentBrandInTopWithOtherBrandsYoy/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/brand/BrandStatsReportGenerationFunctionalTest/testMonthScaleCurrentBrandInTopWithOtherBrandsYoy/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testMonthScaleCurrentBrandInTopWithOtherBrandsYoy(@TempDir Path tempDir) throws Exception {
        Path actualReportPath = tempDir.resolve(BRAND_STATISTICS_TEMP_REPORT_NAME);

        LocalDate from = LocalDate.of(2021, 9, 1);
        LocalDate to = LocalDate.of(2021, 9, 10);

        BrandStatisticsReportParams reportParams = BrandStatisticsReportParams.builder()
                .withFrom(from)
                .withTo(to)
                .withTop5(true)
                .withScaleType(StatisticsScaleType.MONTH)
                .withYoy(true)
                .withVendorId(VENDOR_ID)
                .withCategoryId(CATEGORY_ID)
                .withChosenBrandIds(CHOSEN_BRAND_IDS)
                .withUid(UID)
                .build();

        try (OutputStream os = new FileOutputStream(actualReportPath.toFile())) {
            brandStatisticsReportGenerator.writeXlsxReport(reportParams, os);
        }
        InputStreamResource expectedReport = getInputStreamResource(
                "/testMonthScaleCurrentBrandInTopWithOtherBrandsYoy/expected.xlsx"
        );
        verifyGeneratedMultiSheetReport(expectedReport.getInputStream(), Files.newInputStream(actualReportPath));

    }

}
