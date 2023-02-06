package ru.yandex.market.logistic.api.utils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.fulfillment.wrap.test.FulfillmentWrapTest;

import static ru.yandex.market.logistic.api.utils.DateTimeInterval.fromFormattedValue;

/**
 * Unit тесты для {@link DateTimeInterval}.
 *
 * @author avetokhin 21/09/17.
 */
public class DateTimeIntervalTest extends FulfillmentWrapTest {

    private static final LocalDateTime FROM_LOCAL_1 = LocalDateTime.of(2014, 1, 2, 11, 5, 1);
    private static final LocalDateTime FROM_LOCAL_2 = LocalDateTime.of(2014, 1, 2, 0, 0, 0);

    private static final LocalDateTime TO_LOCAL_1 = LocalDateTime.of(2015, 2, 12, 10, 5, 0);
    private static final LocalDateTime TO_LOCAL_2 = LocalDateTime.of(2015, 2, 12, 10, 0, 0);
    private static final LocalDateTime TO_LOCAL_3 = LocalDateTime.of(2015, 2, 12, 0, 0, 0);

    private static final OffsetDateTime FROM_OFFSET_1 = FROM_LOCAL_1.atOffset(ZoneOffset.UTC);
    private static final OffsetDateTime FROM_OFFSET_2 = FROM_LOCAL_1.atOffset(ZoneOffset.ofHours(3));
    private static final OffsetDateTime FROM_OFFSET_3 = FROM_LOCAL_2.atOffset(ZoneOffset.ofHours(3));

    private static final OffsetDateTime TO_OFFSET_1 = TO_LOCAL_1.atOffset(ZoneOffset.UTC);
    private static final OffsetDateTime TO_OFFSET_2 = TO_LOCAL_1.atOffset(ZoneOffset.ofHours(3));
    private static final OffsetDateTime TO_OFFSET_3 = TO_LOCAL_2.atOffset(ZoneOffset.ofHours(3));
    private static final OffsetDateTime TO_OFFSET_4 = TO_LOCAL_3.atOffset(ZoneOffset.ofHours(3));

    private static final DateTimeInterval INTERVAL_1 = new DateTimeInterval(FROM_OFFSET_1, TO_OFFSET_1);
    private static final DateTimeInterval INTERVAL_2 = new DateTimeInterval(FROM_OFFSET_2, TO_OFFSET_2);
    private static final DateTimeInterval INTERVAL_3 = new DateTimeInterval(FROM_OFFSET_2, TO_OFFSET_3);
    private static final DateTimeInterval INTERVAL_4 = new DateTimeInterval(FROM_OFFSET_3, TO_OFFSET_4);

    /**
     * Тест метода создания объекта из строки с интервалом.
     */
    @Test
    public void fromFormattedValueTest() {
        // Явно указанный UTC.
        assertions().assertThat(fromFormattedValue("2014-01-02T11:05:01+00:00/2015-02-12T10:05+00:00"))
            .isEqualTo(INTERVAL_1);

        // в 'to' TZ и секунды не указаны, по умолчанию 00 и Москва.
        assertions().assertThat(fromFormattedValue("2014-01-02T11:05:01/2015-02-12T10:05"))
            .isEqualTo(INTERVAL_2);

        // Не указана TZ в 'from', TZ и минуты в 'to'.
        assertions().assertThat(fromFormattedValue("2014-01-02T11:05:01/2015-02-12T10"))
            .isEqualTo(INTERVAL_3);

        // Не указаны часы.
        assertions().assertThat(fromFormattedValue("2014-01-02/2015-02-12"))
            .isEqualTo(INTERVAL_4);


    }

    /**
     * Тест метода получения форматированной строки из объекта.
     */
    @Test
    public void getFormattedTest() {
        assertions().assertThat(INTERVAL_1.getFormatted())
            .isEqualTo("2014-01-02T11:05:01+00:00/2015-02-12T10:05:00+00:00");

        assertions().assertThat(INTERVAL_2.getFormatted())
            .isEqualTo("2014-01-02T11:05:01+03:00/2015-02-12T10:05:00+03:00");
    }

}
