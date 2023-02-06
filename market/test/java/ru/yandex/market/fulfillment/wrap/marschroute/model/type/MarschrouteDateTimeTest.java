package ru.yandex.market.fulfillment.wrap.marschroute.model.type;

import org.junit.jupiter.api.Test;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteTemporalType.getMarschrouteZoneOffset;

class MarschrouteDateTimeTest extends BaseIntegrationTest {

    /**
     * Проверяет корректность созданного из строки объекта MarschrouteDateTime.
     */
    @Test
    void testCreateFromString() throws Exception {
        String stringValue = "01.01.1970 14:00:00";
        MarschrouteDateTime result = MarschrouteDateTime.create(stringValue);

        softly.assertThat(result.getValue())
                .as("Asserting date time string value ")
                .isEqualTo(stringValue);

        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 14, 0, 0);

        softly.assertThat(result.getOffsetDateTime())
                .as("Asserting date time offset dt value ")
                .isEqualTo(OffsetDateTime.of(ldt, getMarschrouteZoneOffset()));
    }


    /**
     * Проверяет корректность созданного из LocalDateTime объекта MarschrouteDateTime.
     */
    @Test
    void testCreateFromLocalDateTime() throws Exception {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 14, 0, 0);
        MarschrouteDateTime result = MarschrouteDateTime.create(ldt);

        softly.assertThat(result.getValue())
                .as("Asserting string value")
                .isEqualTo("01.01.1970 14:00:00");

        softly.assertThat(result.getOffsetDateTime())
                .as("Asserting offset date time value")
                .isEqualTo(OffsetDateTime.of(ldt, getMarschrouteZoneOffset()));
    }

    /**
     * Проверяет корректность созданного из OffsetDateTime (c аналогичным ZoneOffset)
     * объекта MarschrouteDateTime.
     */
    @Test
    void testCreateFromOffsetDateTime() throws Exception {
        LocalDateTime dateTime = LocalDate.of(1970, 1, 1).atStartOfDay();

        OffsetDateTime offsetDt = OffsetDateTime.of(dateTime, getMarschrouteZoneOffset());
        MarschrouteDateTime result = MarschrouteDateTime.create(offsetDt);

        softly.assertThat(result.getValue())
                .as("Asserting string value")
                .isEqualTo("01.01.1970 00:00:00");

        softly.assertThat(result.getOffsetDateTime())
                .as("Asserting offset date time value")
                .isEqualTo(offsetDt);
    }

    /**
     * Проверяет корректность созданного из OffsetDateTime (c другим ZoneOffset)
     * объекта MarschrouteDateTime.
     */
    @Test
    void testCreateFromOffsetDateTimeWithDifferentOffset() throws Exception {
        LocalDateTime dateTime = LocalDate.of(1970, 1, 2).atStartOfDay();

        OffsetDateTime offsetDt = OffsetDateTime.of(dateTime, ZoneOffset.ofHours(4));
        MarschrouteDateTime result = MarschrouteDateTime.create(offsetDt);

        softly.assertThat(result.getValue())
                .as("Asserting string value")
                .isEqualTo("01.01.1970 23:00:00");

        softly.assertThat(result.getOffsetDateTime())
                .as("Asserting offset date time value")
                .isEqualTo(OffsetDateTime.of(dateTime.minusHours(1), getMarschrouteZoneOffset()));
    }

    /**
     * Проверяет корректность созданного из строки
     * объекта MarschrouteDateTime при условии, что в строке отсутствуют секунды.
     */
    @Test
    void testConvertFromStringWithoutSeconds() throws Exception {
        String stringValue = "01.01.1970 14:00";

        MarschrouteDateTime result = MarschrouteDateTime.create(stringValue);
        softly.assertThat(result.getValue())
                .as("Asserting string value")
                .isEqualTo("01.01.1970 14:00:00");

        OffsetDateTime offsetDateTime = OffsetDateTime.of(
                LocalDateTime.of(1970, 1, 1, 14, 0),
                getMarschrouteZoneOffset()
        );

        softly.assertThat(result.getOffsetDateTime())
                .as("Asserting offset date time value")
                .isEqualTo(offsetDateTime);
    }
}
