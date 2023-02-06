package ru.yandex.market.rg.asyncreport.statistics.supplier.xls;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.fulfillment.report.excel.ExcelTestUtils;
import ru.yandex.market.rg.asyncreport.statistics.supplier.PartnerSalesStatisticsReportParams;
import ru.yandex.market.rg.asyncreport.statistics.supplier.enums.GranularityLevel;
import ru.yandex.market.rg.asyncreport.statistics.supplier.enums.GroupingBy;
import ru.yandex.market.rg.asyncreport.statistics.supplier.enums.ReportMetric;
import ru.yandex.market.rg.config.ClickhouseFunctionalTest;

/**
 * Тест для {@link SalesStatisticExcelReportGeneratorService}.
 */
@DbUnitDataSet(dataSource = "clickHouseDataSource", before = "data/SalesStatisticExcelReportGeneratorServiceTest.dictionaries.csv")
class SalesStatisticExcelReportGeneratorServiceTest extends ClickhouseFunctionalTest {

    @Autowired
    private SalesStatisticExcelReportGeneratorService salesStatisticExcelReportGeneratorService;

    @ParameterizedTest
    @MethodSource("args")
    @DbUnitDataSet(dataSource = "clickHouseDataSource", before = "data/SalesStatisticExcelReportGeneratorServiceTest.before.csv")
    void testXlsReport(String description,
                       PartnerSalesStatisticsReportParams reportParams,
                       String expectedFilePath) throws Exception {
        Path tempFilePath = Files.createTempFile("SalesStatisticReportGeneratorTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());

        try (OutputStream output = new FileOutputStream(reportFile)) {
            salesStatisticExcelReportGeneratorService.generate(
                    reportParams,
                    output
            );
        }

        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        XSSFWorkbook expected = new XSSFWorkbook(getClass().getResourceAsStream(expectedFilePath));

        ExcelTestUtils.assertEquals(
                expected,
                actual,
                new HashSet<>()
        );
    }


    private static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(
                        "Группировка по sku, детализация по дням, с минимальной информацией",
                        PartnerSalesStatisticsReportParams.builder()
                                .setEntityId(101)
                                .setDateFrom(LocalDate.of(2015, 1, 1))
                                .setDateTo(LocalDate.of(2021, 12, 31))
                                .setDetalization(GranularityLevel.DAY)
                                .setGrouping(GroupingBy.OFFERS)
                                .build(),
                        "SalesStatisticExcelReportGeneratorTest.expected.1.xlsx"
                ),
                Arguments.of(
                        "Группировка по sku, детализация по дням, с полной информацией",
                        PartnerSalesStatisticsReportParams.builder()
                                .setEntityId(101)
                                .setDateFrom(LocalDate.of(2015, 1, 1))
                                .setDateTo(LocalDate.of(2020, 12, 31))
                                .setDetalization(GranularityLevel.DAY)
                                .setGrouping(GroupingBy.OFFERS)
                                .setBrandIds(List.of(1007L)) //Apple
                                .setReportMetrics(Arrays.asList(ReportMetric.values()))
                                .build(),
                        "SalesStatisticExcelReportGeneratorTest.expected.2.xlsx"
                ),
                Arguments.of(
                        "Группировка по sku, детализация по дням, с частичной информацией",
                        PartnerSalesStatisticsReportParams.builder()
                                .setEntityId(101)
                                .setDateFrom(LocalDate.of(2015, 1, 1))
                                .setDateTo(LocalDate.of(2021, 12, 31))
                                .setDetalization(GranularityLevel.DAY)
                                .setGrouping(GroupingBy.OFFERS)
                                .setCategoryIds(List.of(90764L)) //Детские товары
                                .setReportMetrics(List.of(ReportMetric.CHECKOUT_CONVERSION))
                                .build(),
                        "SalesStatisticExcelReportGeneratorTest.expected.3.xlsx"
                ),
                Arguments.of(
                        "Группировка по sku, детализация по неделям, с полной информацией",
                        PartnerSalesStatisticsReportParams.builder()
                                .setEntityId(101)
                                .setDateFrom(LocalDate.of(2015, 1, 1))
                                .setDateTo(LocalDate.of(2021, 12, 31))
                                .setDetalization(GranularityLevel.WEEK)
                                .setSkus(List.of("cau3y8"))
                                .setGrouping(GroupingBy.OFFERS)
                                .setReportMetrics(Arrays.asList(ReportMetric.values()))
                                .build(),
                        "SalesStatisticExcelReportGeneratorTest.expected.4.xlsx"
                ),
                Arguments.of(
                        "Группировка по sku, детализация по месяцам, с полной информацией",
                        PartnerSalesStatisticsReportParams.builder()
                                .setEntityId(101)
                                .setDateFrom(LocalDate.of(2015, 1, 1))
                                .setDateTo(LocalDate.of(2021, 12, 31))
                                .setDetalization(GranularityLevel.MONTH)
                                .setSkus(List.of("cau3y8"))
                                .setGrouping(GroupingBy.OFFERS)
                                .setReportMetrics(Arrays.asList(ReportMetric.values()))
                                .build(),
                        "SalesStatisticExcelReportGeneratorTest.expected.5.xlsx"
                ),
                Arguments.of(
                        "Группировка по sku, детализация по кварталам, с полной информацией",
                        PartnerSalesStatisticsReportParams.builder()
                                .setEntityId(101)
                                .setDateFrom(LocalDate.of(2015, 1, 1))
                                .setDateTo(LocalDate.of(2021, 12, 31))
                                .setDetalization(GranularityLevel.QUARTER)
                                .setSkus(List.of("cau3y8"))
                                .setGrouping(GroupingBy.OFFERS)
                                .setReportMetrics(Arrays.asList(ReportMetric.values()))
                                .build(),
                        "SalesStatisticExcelReportGeneratorTest.expected.6.xlsx"
                ),
                Arguments.of(
                        "Группировка по бренду, детализация по годам, с полной информацией",
                        PartnerSalesStatisticsReportParams.builder()
                                .setEntityId(101)
                                .setDateFrom(LocalDate.of(2015, 1, 1))
                                .setDateTo(LocalDate.of(2021, 12, 31))
                                .setDetalization(GranularityLevel.YEAR)
                                .setGrouping(GroupingBy.BRANDS)
                                .setBrandIds(List.of(1003L)) //AMD
                                .setReportMetrics(Arrays.asList(ReportMetric.values()))
                                .build(),
                        "SalesStatisticExcelReportGeneratorTest.expected.7.xlsx"
                ),
                Arguments.of(
                        "Группировка по категории, детализация по годам, с полной информацией",
                        PartnerSalesStatisticsReportParams.builder()
                                .setEntityId(101)
                                .setDateFrom(LocalDate.of(2015, 1, 1))
                                .setDateTo(LocalDate.of(2021, 12, 31))
                                .setDetalization(GranularityLevel.YEAR)
                                .setGrouping(GroupingBy.CATEGORIES)
                                .setCategoryIds(List.of(91019L)) //Процессоры
                                .setReportMetrics(Arrays.asList(ReportMetric.values()))
                                .build(),
                        "SalesStatisticExcelReportGeneratorTest.expected.8.xlsx"
                )
        );
    }
}
