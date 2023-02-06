package ru.yandex.market.fulfillment.wrap.marschroute.model.type;

import org.junit.jupiter.api.Test;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import static ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteTemporalType.getMarschrouteZoneOffset;

class MarschrouteTimeTest extends BaseIntegrationTest {

    /**
     * Проверяет успешность конвертации из строкового значения
     */
    @Test
    void testConversionFromString() throws Exception {
        String strValue = "14:15";
        MarschrouteTime result = MarschrouteTime.create(strValue);

        softly.assertThat(result.getValue())
                .as("Asserting string value")
                .isEqualTo("14:15:00");

        OffsetTime offsetTime = OffsetTime.of(
                LocalTime.of(14, 15),
                getMarschrouteZoneOffset()
        );

        softly.assertThat(result.getOffsetTime())
                .as("Asserting offset time value")
                .isEqualTo(offsetTime);
    }


    /**
     * Проверяет, что конвертация из OffsetDateTime (с тайм зоной маршрута) проходит успешно.
     */
    @Test
    void testConversionFromOffsetTime() throws Exception {
        OffsetTime offsetTime = OffsetTime.of(
                LocalTime.of(14, 15),
                getMarschrouteZoneOffset()
        );

        MarschrouteTime result = MarschrouteTime.create(offsetTime);

        softly.assertThat(result.getValue())
                .as("Asserting string value")
                .isEqualTo("14:15:00");

        softly.assertThat(result.getOffsetTime())
                .as("Asserting offset time value")
                .isEqualTo(offsetTime);
    }

    /**
     * Проверяет, что конвертация из OffsetDateTime (с тайм зоной отличной от маршрута)
     * проходит успешно.
     */
    @Test
    void testConversionFromOffsetTimeWithDifferentTimeZone() throws Exception {
        OffsetTime offsetTime = OffsetTime.of(
                LocalTime.of(14, 15),
                ZoneOffset.ofHours(4)
        );

        MarschrouteTime result = MarschrouteTime.create(offsetTime);

        softly.assertThat(result.getValue())
                .as("Asserting string value")
                .isEqualTo("13:15:00");

        softly.assertThat(result.getOffsetTime())
                .as("Asserting offset time value")
                .isEqualTo(OffsetTime.of(
                        LocalTime.of(13, 15),
                        getMarschrouteZoneOffset())
                );
    }

    /**
     * Проверяет, что конвертация из строки с указанием секунд проходит успешно.
     */
    @Test
    void testParsingWithSeconds() throws Exception {
        String strValue = "14:15:16";
        MarschrouteTime result = MarschrouteTime.create(strValue);

        softly.assertThat(result.getValue())
                .as("Asserting string value")
                .isEqualTo(strValue);

        softly.assertThat(result.getOffsetTime())
                .as("Asserting offset time value")
                .isEqualTo(OffsetTime.of(
                        LocalTime.of(14, 15, 16),
                        getMarschrouteZoneOffset()
                ));
    }
}
