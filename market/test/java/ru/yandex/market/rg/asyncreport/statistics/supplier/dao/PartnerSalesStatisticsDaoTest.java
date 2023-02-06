package ru.yandex.market.rg.asyncreport.statistics.supplier.dao;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.rg.asyncreport.statistics.supplier.PartnerSalesStatisticsReportParams;
import ru.yandex.market.rg.asyncreport.statistics.supplier.builder.PartnerSalesStatisticsQueryBuilder;
import ru.yandex.market.rg.asyncreport.statistics.supplier.enums.GranularityLevel;
import ru.yandex.market.rg.asyncreport.statistics.supplier.enums.GroupingBy;
import ru.yandex.market.rg.asyncreport.statistics.supplier.enums.ReportMetric;
import ru.yandex.market.rg.asyncreport.statistics.supplier.xls.PartnerSalesReportRow;
import ru.yandex.market.rg.config.ClickhouseFunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Локально поднять CH со схемой можно с помощью скрипта stub/init.sh.
 * Требуется установленный докер
 */
@DbUnitDataSet(dataSource = "clickHouseDataSource", before = "PartnerSalesStatisticsDaoTest.dictionaries.csv")
public class PartnerSalesStatisticsDaoTest extends ClickhouseFunctionalTest {

    private static final String CLICKHOUSE_DATASOURCE = "clickHouseDataSource";
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private PartnerSalesStatisticsDao partnerSalesStatisticsDao;

    @ParameterizedTest(name = "{0}")
    @MethodSource("args")
    @DbUnitDataSet(dataSource = CLICKHOUSE_DATASOURCE, before = "PartnerSalesStatisticsDaoTest.before.csv")
    void fetchDataTest(String decription, PartnerSalesStatisticsReportParams params, String pathToExpectedData) throws IOException {
        var builder = PartnerSalesStatisticsQueryBuilder
                .fromReportParams(params)
                .withDictSchemaPrefix("default");
        var expected = getExpectedData(pathToExpectedData);
        var resultSize = partnerSalesStatisticsDao.fetchRowCount(builder);
        var result = partnerSalesStatisticsDao.fetchReportData(builder);
        assertThat(result).hasSize(resultSize).hasSize(expected.size());
        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    @DbUnitDataSet(dataSource = CLICKHOUSE_DATASOURCE, before = "PartnerSalesStatisticsDaoTest.before.csv")
    void fetchDataBatchedTest() throws IOException {
        var builder = PartnerSalesStatisticsQueryBuilder
                .fromReportParams(PartnerSalesStatisticsReportParams.builder()
                        .setEntityId(101)
                        .setDateFrom(LocalDate.of(2015, 1, 1))
                        .setDateTo(LocalDate.of(2021, 12, 31))
                        .setDetalization(GranularityLevel.DAY)
                        .setGrouping(GroupingBy.OFFERS)
                        .build())
                .withDictSchemaPrefix("default");
        var expected = getExpectedData("data/groupBySku-daily-1.json");
        var resultSize = partnerSalesStatisticsDao.fetchRowCount(builder);
        assertThat(resultSize).isEqualTo(expected.size());
        var accumulatedResult = new ArrayList<PartnerSalesReportRow>();
        var testBatchSize = 5;

        for (int i = 0; i <= 3; i++) {
            var result = partnerSalesStatisticsDao.fetchReportData(
                    builder.buildQuery(testBatchSize, i * testBatchSize),
                    builder.getRowMapper()
            );
            assertThat(result).hasSize(i == 3 ? 2 : testBatchSize);
            accumulatedResult.addAll(result);
        }

        assertThat(accumulatedResult).containsExactlyElementsOf(expected);
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
                        "data/groupBySku-daily-1.json"
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
                        "data/groupBySku-daily-2.json"
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
                        "data/groupBySku-daily-3.json"
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
                        "data/groupBySku-weekly.json"
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
                        "data/groupBySku-monthly.json"
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
                        "data/groupBySku-quarterly.json"
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
                        "data/groupByBrand-yearly.json"
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
                        "data/groupByCategory-yearly.json"
                )

        );
    }

    private List<PartnerSalesReportRow> getExpectedData(String fileName) throws IOException {
        return MAPPER.readValue(StringTestUtil.getString(getClass(), fileName),
                new TypeReference<List<PartnerSalesReportRow>>() {});
    }

}
