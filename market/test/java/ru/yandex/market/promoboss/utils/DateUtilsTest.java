package ru.yandex.market.promoboss.utils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.promoboss.utils.DateUtils.toTimestamp;

class DateUtilsTest {

    @Test
    void toTimestampTest_nonNull_ok() {
        OffsetDateTime offsetDateTime = OffsetDateTime.of(2020, 1, 2, 3,4,5,6, ZoneOffset.UTC);
        Timestamp expected = Timestamp.valueOf(LocalDateTime.of(2020, 1, 2, 3,4,5,6));
        assertEquals(expected, toTimestamp(offsetDateTime));
    }

    @Test
    void toTimestampTest_null_ok() {
        assertNull(toTimestamp(null));
    }
}
