package ru.yandex.market.rg.asyncreport.statistics.supplier;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.asyncreport.util.ParamsUtils;
import ru.yandex.market.rg.asyncreport.statistics.supplier.enums.GranularityLevel;
import ru.yandex.market.rg.asyncreport.statistics.supplier.enums.GroupingBy;
import ru.yandex.market.rg.asyncreport.statistics.supplier.enums.ReportMetric;

public class PartnerSalesStatisticsReportGeneratorTest {
    private ObjectMapper objectMapper;
    private DefaultPrettyPrinter printer;

    @BeforeEach
    void init() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        DefaultPrettyPrinter.Indenter indenter =
                new DefaultIndenter("    ", DefaultIndenter.SYS_LF);
        printer = new DefaultPrettyPrinter();
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);
    }

    static Stream<Arguments> paramsArgs() {
        return Stream.of(
                Arguments.of(
                        "Стандартные параметры",
                        "" +
                                "{\n" +
                                "  \"entityId\": 774,\n" +
                                "  \"detalization\": \"MONTH\",\n" +
                                "  \"dateFrom\": \"2018-03-14\",\n" +
                                "  \"dateTo\": \"2018-03-14\",\n" +
                                "  \"regionIds\": [\n" +
                                "    1,\n" +
                                "    2\n" +
                                "  ],\n" +
                                "  \"brandIds\": [\n" +
                                "    \"3\",\n" +
                                "    \"4\"\n" +
                                "  ],\n" +
                                "  \"categoryIds\": [\n" +
                                "    \"5\",\n" +
                                "    \"6\"\n" +
                                "  ],\n" +
                                "  \"skus\": [\n" +
                                "    \"1\",\n" +
                                "    \"2\"\n" +
                                "  ],\n" +
                                "  \"reportMetrics\": [\n" +
                                "    \"CHECKOUT\",\n" +
                                "    \"PRICE\",\n" +
                                "    \"SHOWS\",\n" +
                                "    \"SALES\"\n" +
                                "  ],\n" +
                                "  \"grouping\": \"OFFERS\"" +
                                "}",
                        "" +
                                "{\n" +
                                "    \"detalization\" : \"MONTH\",\n" +
                                "    \"dateFrom\" : \"2018-03-14\",\n" +
                                "    \"dateTo\" : \"2018-03-14\",\n" +
                                "    \"regionIds\" : [\n" +
                                "        1,\n" +
                                "        2\n" +
                                "    ],\n" +
                                "    \"brandIds\" : [\n" +
                                "        3,\n" +
                                "        4\n" +
                                "    ],\n" +
                                "    \"categoryIds\" : [\n" +
                                "        5,\n" +
                                "        6\n" +
                                "    ],\n" +
                                "    \"skus\" : [\n" +
                                "        \"1\",\n" +
                                "        \"2\"\n" +
                                "    ],\n" +
                                "    \"reportMetrics\" : [\n" +
                                "        \"CHECKOUT\",\n" +
                                "        \"PRICE\",\n" +
                                "        \"SHOWS\",\n" +
                                "        \"SALES\"\n" +
                                "    ],\n" +
                                "    \"grouping\" : \"OFFERS\",\n" +
                                "    \"entityId\" : 774\n" +
                                "}"
                ),
                Arguments.of(
                        "Пустые параметры",
                        "" +
                                "{\n" +
                                "  \"entityId\": 774,\n" +
                                "  \"detalization\": \"MONTH\",\n" +
                                "  \"dateFrom\": \"2018-03-14\",\n" +
                                "  \"dateTo\": \"2018-03-14\"\n" +
                                "}",
                        "" +
                                "{\n" +
                                "    \"detalization\" : \"MONTH\",\n" +
                                "    \"dateFrom\" : \"2018-03-14\",\n" +
                                "    \"dateTo\" : \"2018-03-14\",\n" +
                                "    \"regionIds\" : [ ],\n" +
                                "    \"brandIds\" : [ ],\n" +
                                "    \"categoryIds\" : [ ],\n" +
                                "    \"skus\" : [ ],\n" +
                                "    \"reportMetrics\" : [ ],\n" +
                                "    \"grouping\" : null,\n" +
                                "    \"entityId\" : 774\n" +
                                "}"
                )
        );
    }

    @MethodSource("paramsArgs")
    @ParameterizedTest()
    void testParams(String description, String input, String output) throws IOException {
        PartnerSalesStatisticsReportParams params = objectMapper.readValue(
                input,
                PartnerSalesStatisticsReportParams.class);

        JsonTestUtil.assertEquals(output, objectMapper.writer(printer).writeValueAsString(params));
    }

    @MethodSource("paramsSource")
    @ParameterizedTest(name = "{2}")
    void testParamsDeserialization(Map<String, Object> params, PartnerSalesStatisticsReportParams expected,
                                   String description) {
        Assertions.assertThat(ParamsUtils.convertToParams(params, PartnerSalesStatisticsReportParams.class))
                .isEqualTo(expected);
    }

    private static Stream<Arguments> paramsSource() {
        return Stream.of(
                Arguments.of(
                        Map.of("entityId", 13073),
                        PartnerSalesStatisticsReportParams.builder()
                                .setEntityId(13073)
                                .build(),
                        "Пустые параметры"),
                Arguments.of(
                        Map.of("entityId", 13073,
                                "regionIds", List.of(1, 2, 3),
                                "brandIds", List.of(444, 333)),
                        PartnerSalesStatisticsReportParams.builder()
                                .setEntityId(13073)
                                .setRegionIds(List.of(1L, 2L, 3L))
                                .setBrandIds(List.of(444L, 333L))
                                .build(),
                        "Десериализация коллекций"),
                Arguments.of(Map.of("entityId", 13073,
                        "regionIds", List.of(1, 2, 3),
                        "grouping", GroupingBy.BRANDS,
                        "detalization", GranularityLevel.WEEK,
                        "reportMetrics", List.of(ReportMetric.CHECKOUT_CONVERSION, ReportMetric.ITEMS_DELIVERED)),
                        PartnerSalesStatisticsReportParams.builder()
                                .setEntityId(13073)
                                .setRegionIds(List.of(1L, 2L, 3L))
                                .setGrouping(GroupingBy.BRANDS)
                                .setDetalization(GranularityLevel.WEEK)
                                .setReportMetrics(List.of(ReportMetric.CHECKOUT_CONVERSION, ReportMetric.ITEMS_DELIVERED))
                                .build(),
                        "Десериализация enum")
        );
    }
}
