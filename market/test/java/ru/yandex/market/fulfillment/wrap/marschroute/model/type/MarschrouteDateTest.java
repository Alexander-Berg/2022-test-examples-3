package ru.yandex.market.fulfillment.wrap.marschroute.model.type;

import org.junit.jupiter.api.Test;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

class MarschrouteDateTest extends BaseIntegrationTest {

    /**
     * Проверяет, что OffsetDateTime в Московском часовом поясе будет успешно преобразован в правильную дату.
     */
    @Test
    void testConversionFromOffsetDateTime() throws Exception {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0);

        OffsetDateTime offsetDateTime = OffsetDateTime.of(ldt, ZoneOffset.ofHours(3));
        MarschrouteDate marschrouteDate = MarschrouteDate.create(offsetDateTime);

        softly.assertThat(marschrouteDate.getDate())
                .as("Checking date value")
                .isEqualTo(ldt.toLocalDate());

        softly.assertThat(marschrouteDate.getValue())
                .as("Checking date string value")
                .isEqualTo("01.01.1970");

    }

    /**
     * Проверяет, что если OffsetDateTime будет в часовом поясе, отличным от Московского -
     * дни будут сконвертированы с учетом часового пояса.
     */
    @Test
    void testConversionFromOffsetDateTimeWithDifferentOffset() throws Exception {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 2, 0, 0);

        OffsetDateTime offsetDateTime = OffsetDateTime.of(ldt, ZoneOffset.ofHours(4));
        MarschrouteDate marschrouteDate = MarschrouteDate.create(offsetDateTime);

        softly.assertThat(marschrouteDate.getDate())
                .as("Checking date value")
                .isEqualTo(ldt.toLocalDate().minusDays(1));

        softly.assertThat(marschrouteDate.getValue())
                .as("Checking date string value")
                .isEqualTo("01.01.1970");
    }

    /**
     * Проверяет успешность конвертации из строкового значения
     */
    @Test
    void testConversionFromString() throws Exception {
        String strValue = "12.12.2015";
        MarschrouteDate date = MarschrouteDate.create(strValue);

        softly.assertThat(date.getDate())
                .as("Asserting date value")
                .isEqualTo(LocalDate.of(2015, 12, 12));

        softly.assertThat(date.getValue())
                .as("Asserting date string value")
                .isEqualTo(strValue);
    }
}
