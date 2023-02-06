package ru.yandex.market.checker.yql;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static java.time.format.DateTimeFormatter.ISO_DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.checker.utils.YqlFormatter.formattedString;
import static ru.yandex.market.checker.yql.YqlQueryPlaceholders.DATE_FROM;
import static ru.yandex.market.checker.yql.YqlQueryPlaceholders.DATE_TO;
import static ru.yandex.market.checker.yql.YqlQueryPlaceholders.FIRST_COMPONENT_NAME;
import static ru.yandex.market.checker.yql.YqlQueryPlaceholders.FIRST_COMPONENT_TABLE_PATH;
import static ru.yandex.market.checker.yql.YqlQueryPlaceholders.PREV_DATE_TO;
import static ru.yandex.market.checker.yql.YqlQueryPlaceholders.RESULT_TABLE_FOLDER;
import static ru.yandex.market.checker.yql.YqlQueryPlaceholders.RESULT_TABLE_NAME;
import static ru.yandex.market.checker.yql.YqlQueryPlaceholders.SECOND_COMPONENT_NAME;
import static ru.yandex.market.checker.yql.YqlQueryPlaceholders.SECOND_COMPONENT_TABLE_PATH;
import static ru.yandex.market.checker.yql.YqlQueryPlaceholders.TIME_WINDOW_UNIT;

class YqlFormatterTest {

    private static final String TEST_QUERY = "" +
            "%(DATE_FROM)" +
            "%(DATE_TO)" +
            "%(PREV_DATE_TO)" +
            "%(RESULT_TABLE_FOLDER)" +
            "%(RESULT_TABLE_NAME)" +
            "%(TIME_WINDOW_UNIT)" +
            "%(FIRST_COMPONENT_NAME)" +
            "%(SECOND_COMPONENT_NAME)" +
            "%(FIRST_COMPONENT_TABLE_PATH)" +
            "%(SECOND_COMPONENT_TABLE_PATH)";

    private static final String EXPECTED_RESULT = "" +
            "2020-01-01" +
            "2020-02-01" +
            "2020-01-31" +
            "/result/folder" +
            "result_table" +
            "DAYS" +
            "Fulfillment1P" +
            "Axapta1P" +
            "/first/path" +
            "/second/path";

    @Test
    void test_formattedYqlQuery() {
        Map<YqlQueryPlaceholders, String> config = new HashMap<>();

        config.put(DATE_FROM, ISO_DATE.format(LocalDate.of(2020, 1, 1)));
        config.put(DATE_TO, ISO_DATE.format(LocalDate.of(2020, 2, 1)));
        config.put(PREV_DATE_TO, ISO_DATE.format(LocalDate.of(2020, 2, 1).minusDays(1)));
        config.put(RESULT_TABLE_FOLDER, "/result/folder");
        config.put(RESULT_TABLE_NAME, "result_table");
        config.put(TIME_WINDOW_UNIT, "DAYS");
        config.put(FIRST_COMPONENT_NAME, "Fulfillment1P");
        config.put(SECOND_COMPONENT_NAME, "Axapta1P");
        config.put(FIRST_COMPONENT_TABLE_PATH, "/first/path");
        config.put(SECOND_COMPONENT_TABLE_PATH, "/second/path");

        assertThat(formattedString(TEST_QUERY, config)).isEqualTo(EXPECTED_RESULT);
    }

    @Test
    @DisplayName("Тест на парсинг даты обновления задачи.")
    void test_dateParse() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        dateTimeFormatter.parse("2021-10-01T00:02:35.3453Z".split("[.|Z]")[0]);
        dateTimeFormatter.parse("2021-10-01T00:02:35Z".split("[.|Z]")[0]);
    }
}
