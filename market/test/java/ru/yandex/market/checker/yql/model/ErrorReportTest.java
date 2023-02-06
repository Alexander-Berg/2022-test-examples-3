package ru.yandex.market.checker.yql.model;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checker.utils.CheckerUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorReportTest {

    private static final String JSON = "{\n" +
            "  \"data\": [\n" +
            "    {\n" +
            "      \"Write\": [\n" +
            "        {\n" +
            "          \"Type\": [\n" +
            "            \"ListType\",\n" +
            "            [\n" +
            "              \"StructType\",\n" +
            "              [\n" +
            "                [\n" +
            "                  \"mismatch_field_name\",\n" +
            "                  [\n" +
            "                    \"DataType\",\n" +
            "                    \"String\"\n" +
            "                  ]\n" +
            "                ],\n" +
            "                [\n" +
            "                  \"mismatch_type\",\n" +
            "                  [\n" +
            "                    \"DataType\",\n" +
            "                    \"String\"\n" +
            "                  ]\n" +
            "                ],\n" +
            "                [\n" +
            "                  \"errors_count\",\n" +
            "                  [\n" +
            "                    \"DataType\",\n" +
            "                    \"Uint64\"\n" +
            "                  ]\n" +
            "                ]\n" +
            "              ]\n" +
            "            ]\n" +
            "          ],\n" +
            "          \"Data\": [\n" +
            "            [\n" +
            "              \"count\",\n" +
            "              \"FIRST_ABSENT\",\n" +
            "              \"153\"\n" +
            "            ],\n" +
            "            [\n" +
            "              \"defect_count\",\n" +
            "              \"FIRST_ABSENT\",\n" +
            "              \"153\"\n" +
            "            ],\n" +
            "            [\n" +
            "              \"fact_count\",\n" +
            "              \"FIRST_ABSENT\",\n" +
            "              \"153\"\n" +
            "            ],\n" +
            "            [\n" +
            "              \"shortage_count\",\n" +
            "              \"FIRST_ABSENT\",\n" +
            "              \"153\"\n" +
            "            ],\n" +
            "            [\n" +
            "              \"surplus_count\",\n" +
            "              \"FIRST_ABSENT\",\n" +
            "              \"153\"\n" +
            "            ],\n" +
            "            [\n" +
            "              \"shortage_count\",\n" +
            "              \"VALUE_MISMATCH\",\n" +
            "              \"44\"\n" +
            "            ],\n" +
            "            [\n" +
            "              \"fact_count\",\n" +
            "              \"VALUE_MISMATCH\",\n" +
            "              \"6\"\n" +
            "            ],\n" +
            "            [\n" +
            "              \"defect_count\",\n" +
            "              \"VALUE_MISMATCH\",\n" +
            "              \"1\"\n" +
            "            ],\n" +
            "            [\n" +
            "              \"surplus_count\",\n" +
            "              \"VALUE_MISMATCH\",\n" +
            "              \"1\"\n" +
            "            ]\n" +
            "          ]\n" +
            "        }\n" +
            "      ],\n" +
            "      \"Position\": {\n" +
            "        \"Column\": \"1\",\n" +
            "        \"Row\": \"1\",\n" +
            "        \"File\": \"<main>\"\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"errors\": [],\n" +
            "  \"id\": \"606b3a3ad2b70ca3836d49b7\",\n" +
            "  \"issues\": [],\n" +
            "  \"status\": \"COMPLETED\",\n" +
            "  \"updatedAt\": \"2021-04-05T16:27:45.537Z\",\n" +
            "  \"version\": 1000000\n" +
            "}";

    private static final String INCORRECT_JSON = "" +
            "{\n" +
            "  \"data\": [\n" +
            "    {\n" +
            "      \"Write\": [\n" +
            "        {\n" +
            "          \"Type\": [\n" +
            "            \"ListType\",\n" +
            "            [\n" +
            "              \"StructType\",\n" +
            "              [\n" +
            "                [\n" +
            "                  \"errors_count\",\n" +
            "                  [\n" +
            "                    \"DataType\",\n" +
            "                    \"Uint64\"\n" +
            "                  ]\n" +
            "                ]\n" +
            "              ]\n" +
            "            ]\n" +
            "          ],\n" +
            "          \"Data\": [\n" +
            "            [\n" +
            "              \"817\"\n" +
            "            ]\n" +
            "          ]\n" +
            "        }\n" +
            "      ],\n" +
            "      \"Position\": {\n" +
            "        \"Column\": \"1\",\n" +
            "        \"Row\": \"1\",\n" +
            "        \"File\": \"<main>\"\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"errors\": [],\n" +
            "  \"id\": \"606af174d2b70ca3836cdbca\",\n" +
            "  \"issues\": [],\n" +
            "  \"status\": \"COMPLETED\",\n" +
            "  \"updatedAt\": \"2021-04-05T11:16:05.941Z\",\n" +
            "  \"version\": 1000000\n" +
            "}";

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    @Test
    @DisplayName("Создание отчета по ошибкам из JsonNode")
    void test_ErrorReportShouldCreate_WhenJsonNodeGiven() throws IOException {
        JsonNode jsonNode = objectMapper.readTree(JSON);
        ErrorReport report = ErrorReport.fromJsonNode(1, "QUEUE-1", "abc", jsonNode);
        assertEquals(9, report.getErrorData().size());

        List<ErrorReportRecord> errorReportRecords = report.toRecords();
        assertEquals(10L, errorReportRecords.size());
        assertEquals(errorReportRecords.get(9).getMismatchFieldName(), MismatchType.TOTAL.name());
        assertEquals(errorReportRecords.get(9).getMismatchCount(), 817L);
        assertEquals(errorReportRecords.get(9).getMismatchType(), MismatchType.TOTAL);
    }

    @Test
    @DisplayName("Graceful degradation")
    void test_shouldReturnEmptyReport_whenIncorrectJsonGiven() throws IOException {
        ErrorReport emptyReport = ErrorReport.fromJsonNode(1, "QUEUE-1", "abc", objectMapper.readTree(INCORRECT_JSON));

        assertEquals(emptyReport.getErrorData().size(), 0);
    }

    @Test
    @DisplayName("Генерация таблицы для отчета")
    void test_shouldBuildTableFromRecords() {
        ErrorReport errorReport = new ErrorReport(1L, "stid", "yqlid",
                List.of(new ErrorReportRow("field1", MismatchType.FIRST_ABSENT, 1),
                        new ErrorReportRow("field2", MismatchType.SECOND_ABSENT, 2),
                        new ErrorReportRow("field3", MismatchType.VALUE_MISMATCH, 3)
                )
        );
        String table = errorReport.getErrorCommentTable();
        String expected = "" +
                "#|\n" +
                "|| Название поля| Тип расхождения| Кол-во||\n" +
                "|| field1| FIRST_ABSENT| 1||\n" +
                "|| field2| SECOND_ABSENT| 2||\n" +
                "|| field3| VALUE_MISMATCH| 3||\n" +
                "|| TOTAL| TOTAL| 6||\n" +
                "|#";

        assertEquals(table, expected);
        assertEquals(CheckerUtils.reportText(errorReport), "Таблица найденных расхождений:\n" + table);
    }
}
