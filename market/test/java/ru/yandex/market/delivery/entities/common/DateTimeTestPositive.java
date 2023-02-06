package ru.yandex.market.delivery.entities.common;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DateTimeTestPositive extends BaseTest {

    private DateTime dateTime;

    static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {"2017-09-10", "2017-09-10T00:00:00+03:00"},
            {"2017-09-10T10:30", "2017-09-10T10:30:00+03:00"},
            {"2017-09-10T05:56:10", "2017-09-10T05:56:10+03:00"},
            {"2017-09-10T10:16:00+03:00", "2017-09-10T10:16:00+03:00"},
            {"2017-09-10T14:02:01+05:00", "2017-09-10T14:02:01+05:00"},
            {"2017-09-10T12:04:11+05:30", "2017-09-10T12:04:11+05:30"},
            {"2017-09-10T12:04:11+00:00", "2017-09-10T12:04:11+00:00"},
        });
    }

    @ParameterizedTest
    @MethodSource("data")
    void itShouldHaveCorrectlyIncomingString(String incomingString, String expectedString) {
        dateTime = new DateTime(incomingString);
        softly.assertThat(dateTime.getInputString())
            .as("Date Time incoming string is invalid")
            .isEqualTo(incomingString);
    }

    @ParameterizedTest
    @MethodSource("data")
    void itShouldHaveCorrectlyDateObject(String incomingString, String expectedString) {
        dateTime = new DateTime(incomingString);
        softly.assertThat(dateTime.getOffsetDateTime())
            .as("Date Object is invalid")
            .isEqualTo(OffsetDateTime.parse(expectedString));
    }

    @ParameterizedTest
    @MethodSource("data")
    void itShouldHaveCorrectlyFormattedString(String incomingString, String expectedString) {
        dateTime = new DateTime(incomingString);
        softly.assertThat(dateTime.getFormattedDate())
            .as("Date Formatted string is invalid")
            .isEqualTo(expectedString);
    }

}
