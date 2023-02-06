package ru.yandex.market.vendor.stats.clickhouse;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.StringUtils;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.vendor.stats.clickhouse.BrandzoneBannerStatisticsContext;
import ru.yandex.vendor.stats.clickhouse.ClickHouseStatService;
import ru.yandex.vendor.stats.clickhouse.XlsxClickhouseReportGenerator;
import ru.yandex.vendor.stats.clickhouse.model.report.BrandzoneBannerStatisticsReportData;
import ru.yandex.vendor.stats.clickhouse.model.report.BrandzoneBannerStatisticsReportDataBody;
import ru.yandex.vendor.stats.clickhouse.model.report.BrandzoneBannerStatisticsReportDataHeader;
import ru.yandex.vendor.stats.clickhouse.model.report.BrandzoneBannerStatisticsReportDataRow;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;

public class ClickHouseStatServiceFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    private static final String BRANDZONE_BANNER_STATISTICS_TEMP_REPORT_NAME = "bz_banner_statistics_temp_report.xlsx";
    private static final List<String> BRANDZONE_BANNER_STATISTICS_TABLE_HEADER
            = List.of("", "", "Было показов", "Осталось показов", "Было кликов", "CTR");

    private static final int REPORT_NAME_ROW = 0;
    private static final int TABLE_HEADER_ROW_OFFSET = 1;
    private static final int TABLE_BODY_ROW_OFFSET = 2;

    private final ClickHouseStatService clickHouseStatService;
    private final XlsxClickhouseReportGenerator xlsxClickhouseReportGenerator;
    private final WireMockServer csBillingApiMock;

    @Autowired
    public ClickHouseStatServiceFunctionalTest(ClickHouseStatService clickHouseStatService,
                                               XlsxClickhouseReportGenerator xlsxClickhouseReportGenerator,
                                               WireMockServer csBillingApiMock) {
        this.clickHouseStatService = clickHouseStatService;
        this.xlsxClickhouseReportGenerator = xlsxClickhouseReportGenerator;
        this.csBillingApiMock = csBillingApiMock;
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/clickhouse/ClickHouseStatServiceFunctionalTest/testBrandzoneBannerStatisticsReportGeneration/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/stats/clickhouse/ClickHouseStatServiceFunctionalTest/testBrandzoneBannerStatisticsReportGeneration/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DisplayName("Отчет за июль, август, сентябрь. Данные есть за август и сентябрь: за август только по тач, сентябрь - тач и десктоп")
    @Test
    void testBrandzoneBannerStatisticsReportGeneration(@TempDir Path tempDir) throws IOException {
        Path reportPath = tempDir.resolve(BRANDZONE_BANNER_STATISTICS_TEMP_REPORT_NAME);

        csBillingApiMock.stubFor(WireMock.get(WireMock.urlMatching("/service/132/datasource/tariffs\\?datasourceId=[0-9]+&datasourceId=[0-9]+"))
                .willReturn(okJson(getStringResource("/testBrandzoneBannerStatisticsReportGeneration/campaignTariffsResponse.json"))));

        csBillingApiMock.stubFor(WireMock.get(WireMock.urlMatching("/service/132/datasource/activity\\?datasourceId=[0-9]+&datasourceId=[0-9]+"))
                .willReturn(okJson(getStringResource("/testBrandzoneBannerStatisticsReportGeneration/activityResponse.json"))));

        BrandzoneBannerStatisticsContext context = new BrandzoneBannerStatisticsContext(
                100,
                10,
                List.of(1593550800000L, 1596229200000L, 1598907600000L)
        );
        var brandzoneBannerStatistics = clickHouseStatService.getBrandzoneBannerStatistics(context);
        var reportData = BrandzoneBannerStatisticsReportData.of(context, brandzoneBannerStatistics);
        try (OutputStream os = new FileOutputStream(reportPath.toFile())) {
            xlsxClickhouseReportGenerator.generateBrandzoneBannerStatisticsReport(os, reportData);
        }
        verifyGeneratedReport(reportPath, reportData);
    }

    private void verifyGeneratedReport(Path reportPath, BrandzoneBannerStatisticsReportData reportData)
            throws IOException {
        try (InputStream is = new FileInputStream(reportPath.toFile());
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(workbook.getNumberOfSheets() - 1);
            validateHeader(sheet, reportData.getHeader());
            validateTable(sheet, reportData.getBody());
        }
    }

    private void validateHeader(Sheet sheet, BrandzoneBannerStatisticsReportDataHeader header) {
        Assertions.assertEquals(
                "Статистика продвижения бренд-зоны",
                sheet.getRow(REPORT_NAME_ROW).getCell(0).getStringCellValue()
        );
    }

    private void validateTable(Sheet sheet, BrandzoneBannerStatisticsReportDataBody body) {
        validateTableHeader(sheet);
        validateTableBody(sheet, body);
    }

    private void validateTableHeader(Sheet sheet) {
        Row tableHeader = sheet.getRow(TABLE_HEADER_ROW_OFFSET);
        Stream.iterate(0, i -> ++i)
                .limit(BRANDZONE_BANNER_STATISTICS_TABLE_HEADER.size())
                .forEach(i -> Assertions.assertEquals(
                        tableHeader.getCell(i).getStringCellValue(),
                        BRANDZONE_BANNER_STATISTICS_TABLE_HEADER.get(i))
                );
    }

    private void validateTableBody(Sheet sheet, BrandzoneBannerStatisticsReportDataBody body) {
        List<BrandzoneBannerStatisticsReportDataRow> rows = body.getRows();
        Stream.iterate(0, i -> ++i)
                .limit(rows.size())
                .forEach(i -> validateTableBodyRow(sheet.getRow(i + TABLE_BODY_ROW_OFFSET), rows.get(i)));
    }

    private void validateTableBodyRow(Row row, BrandzoneBannerStatisticsReportDataRow reportDataRow) {
        Assertions.assertEquals(
                reportDataRow.getPeriod(),
                StringUtils.nullIfEmpty(row.getCell(0).getStringCellValue()),
                "Период"
        );
        Assertions.assertEquals(
                reportDataRow.getRowHeader(),
                StringUtils.nullIfEmpty(row.getCell(1).getStringCellValue()),
                "Заголовок строки"
        );
        double actualShows = row.getCell(2).getNumericCellValue();
        Assertions.assertEquals(
                reportDataRow.getActualShows(),
                reportDataRow.getActualShows() == null ? nullIfZeroCast(actualShows) : (Long) ((long) actualShows),
                "Реальные показы"
        );
        double actualShowsLeft = row.getCell(3).getNumericCellValue();
        Assertions.assertEquals(
                reportDataRow.getShowsLeft(),
                reportDataRow.getShowsLeft() == null ? nullIfZeroCast(actualShowsLeft) : (Long) ((long) actualShowsLeft),
                "Оставшиеся показы"
        );
        double actualClicks = row.getCell(4).getNumericCellValue();
        Assertions.assertEquals(
                reportDataRow.getClicks(),
                reportDataRow.getClicks() == null ? nullIfZeroCast(actualClicks) : (Long) ((long) actualClicks),
                "Клики"
        );
        double actualCtr = row.getCell(5).getNumericCellValue();
        Assertions.assertEquals(
                reportDataRow.getCtr(),
                reportDataRow.getCtr() == null ? nullIfZero(actualCtr) : (Double) actualCtr,
                "CTR"
        );
    }

    private Long nullIfZeroCast(Double value) {
        Double convertedValue = nullIfZero(value);
        return convertedValue == null ? null : convertedValue.longValue();
    }

    private Double nullIfZero(double value) {
        return value == 0.0 ? null : value;
    }
}
