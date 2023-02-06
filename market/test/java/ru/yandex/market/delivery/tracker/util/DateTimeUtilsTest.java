package ru.yandex.market.delivery.tracker.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DateTimeUtilsTest {

    private final SoftAssertions softAssertions = new SoftAssertions();

    @AfterEach
    public void tearDown() {
        softAssertions.assertAll();
    }

    @Test
    void testConvertOffsetDateTimeToDate() {
        Instant now = Instant.now();

        OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(now, ZoneId.systemDefault());
        Date expectedDate = Date.from(now);

        Date actualDate = DateTimeUtils.convertToDate(offsetDateTime);

        softAssertions.assertThat(actualDate)
            .as("Asserting that the dateTime is converted correctly")
            .isEqualTo(expectedDate);
    }

    @Test
    void testConvertOffsetDateTimeNullFailed() {
        softAssertions.assertThatThrownBy(
            () -> DateTimeUtils.convertToDate(null),
            "Asserting that attempt to convert null causes valid exception"
        )
            .as("Asserting that attempt to convert null causes valid exception")
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("offsetDateTime was null");
    }
}
