package ru.yandex.market.fulfillment.stockstorage.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.fulfillment.stockstorage.util.LocalDateTimeUtil.DEFAULT_UPDATED_VALUE;
import static ru.yandex.market.fulfillment.stockstorage.util.LocalDateTimeUtil.DEFAULT_ZONE_OFFSET;
import static ru.yandex.market.fulfillment.stockstorage.util.LocalDateTimeUtil.toMillisOrZero;

public class LocalDateTimeUtilTest {

    @Test
    public void defaultLocalDateTimeToMillis() {
        OffsetDateTime offsetDateTime = null;
        LocalDateTime localDateTime = LocalDateTimeUtil.getOrDefault(offsetDateTime);
        assertEquals("Default timestamp", toMillisOrZero(localDateTime), 0);
    }

    @Test
    public void withDefaultOffset() {
        OffsetDateTime offsetDateTime = LocalDateTimeUtil.withDefaultOffset(LocalDateTime.now());
        assertEquals("Check offset",
                OffsetDateTime.now(LocalDateTimeUtil.DEFAULT_ZONE_ID).getOffset(),
                offsetDateTime.getOffset()
        );
    }

    @Test
    public void defaultUpdatedValue() {
        assertEquals("Defaul datetime value formatted",
                DEFAULT_UPDATED_VALUE.getFormattedDate(),
                "1970-01-01T03:00:00+03:00"
        );

        assertEquals("Defaul datetime value offset",
                DEFAULT_UPDATED_VALUE.getOffsetDateTime(),
                OffsetDateTime.of(LocalDateTime.of(1970, 1, 1, 3, 0), DEFAULT_ZONE_OFFSET)
        );
    }
}
