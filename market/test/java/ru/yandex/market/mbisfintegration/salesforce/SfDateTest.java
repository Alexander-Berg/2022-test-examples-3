package ru.yandex.market.mbisfintegration.salesforce;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SfDateTest {

    @CsvSource({
            "2022-01-01T13:30:11, 2022-01-01T13:30:11",
            "2022-01-01, 2022-01-01",
            "13:30:11, 13:30:11",
            "T13:30:11, 13:30:11",
            "T13:30:11, 13:30:11",
            "T13:30, 13:30:00",
            "13:30:11.123, 13:30:11.123",
            "13:30:11.123000, 13:30:11.123",
    })
    @ParameterizedTest
    void testParseIso(String dateTimeString, String serializedResult) {
        assertThat(SfDate.parseIso(dateTimeString).toString()).isEqualTo(serializedResult);
    }

    @Test
    void testParseWithDateFormatter() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        assertThat(SfDate.parse("19.04.2022", formatter, LocalDate::from).toString())
                .isEqualTo("2022-04-19");
    }

    @Test
    void testParseWithDateTimeFormatter() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        assertThat(SfDate.parse("19.04.2022 13:30:11", formatter, LocalDateTime::from).toString())
                .isEqualTo("2022-04-19T13:30:11");
    }

    @Test
    void testParseWithTimeFormatter() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        assertThat(SfDate.parse("13:30:11", formatter, LocalTime::from).toString())
                .isEqualTo("13:30:11");
    }
}