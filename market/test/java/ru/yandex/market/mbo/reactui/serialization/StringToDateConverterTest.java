package ru.yandex.market.mbo.reactui.serialization;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * @author yuramalinov
 * @created 04.11.2019
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class StringToDateConverterTest {
    private final ZoneId moscow = ZoneId.of("Europe/Moscow");
    private StringToDateConverter converter = new StringToDateConverter();

    @Before
    public void setup() {
        converter = new StringToDateConverter();
        ReflectionTestUtils.setField(converter, "zone", moscow);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void shouldConvertDates() {
        Date date = converter.convert("2019-11-04");
        Assertions.assertThat(date.getTime()).isEqualTo(
            LocalDateTime.of(2019, 11, 4, 0, 0, 0).atZone(moscow).toEpochSecond() * 1_000
        );
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void shouldConvertDateTimes() {
        // JavaScripts new Date().toISOString()
        Date date = converter.convert("2019-11-04T11:08:26.601Z");
        Assertions.assertThat(date.getTime()).isEqualTo(
            LocalDateTime.of(2019, 11, 4, 14, 8, 26).atZone(moscow).toEpochSecond() * 1_000
        );
    }
}
