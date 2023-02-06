package ru.yandex.direct.mysql.slowlog.parser;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import ru.yandex.direct.utils.JsonUtils;

/**
 * Проверяет корректность парсинга записей slow query лога путем сравнения результата с эталонным.
 */
@Execution(ExecutionMode.SAME_THREAD)
public class SlowLogRawRecordParserTest {
    private static String[][] testData;
    private static Map<String, String> paramsMap;

    @BeforeAll
    public static void beforeAll() {
        testData = TestDataResourcesLoader.loadTestData();
        paramsMap = new HashMap<>();
    }

    @Test
    public void testSlowLogRecordsParsing() {
        SoftAssertions.assertSoftly(softly -> {
            int count = testData[0].length;
            for (int i = 0; i < count; i++) {
                String input = testData[0][i];
                ParsedSlowLogRawRecord record = SlowLogRawRecordParser.parseRawRecordText(input, paramsMap);
                String recordAsJson = JsonUtils.toJson(record);
                softly.assertThat(recordAsJson)
                        .as(String.format("Record with index %d has failed test", i))
                        .isEqualTo(testData[1][i]);
            }
        });
    }
}
