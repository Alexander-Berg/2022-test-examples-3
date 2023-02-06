package ru.yandex.market.core.periodic_survey.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.periodic_survey.model.SurveyId;
import ru.yandex.market.core.periodic_survey.model.SurveyType;

public class PeriodicSurveyConverterTest extends FunctionalTest {

    public static final Instant OFFSET_DATE_TIME = LocalDateTime.of(
                    2021, 7, 14, 12, 15, 18)
            .toInstant(OffsetDateTime.now().getOffset());

    @Test
    void testCorrectEncodeAndDecode() {
        SurveyId initial = SurveyId.of(1071317L, 176520724L, SurveyType.NPS_DROPSHIP, OFFSET_DATE_TIME);
        String encoded = PeriodicSurveyConverter.encode(initial);
        SurveyId decoded = PeriodicSurveyConverter.decode(encoded);
        Assertions.assertThat(decoded).isEqualTo(initial);
    }

    @Test
    void testReduceLeadingZeros() {
        SurveyId withZeros = PeriodicSurveyConverter.decode("0000345-abc3-2-60eeefed");
        String encoded = PeriodicSurveyConverter.encode(withZeros);
        Assertions.assertThat(encoded).isEqualTo("345-abc3-2-60eeefed");
    }

    @Test
    void testCorrectEncodeAndDecodeWithZeroIds() {
        SurveyId initial = SurveyId.of(0L, 176520724L, SurveyType.NPS_DROPSHIP, OFFSET_DATE_TIME);
        String encoded = PeriodicSurveyConverter.encode(initial);
        SurveyId decoded = PeriodicSurveyConverter.decode(encoded);
        Assertions.assertThat(decoded).isEqualTo(initial);
    }

    @Test
    void testDecodeIncorrectString() {
        Assertions.assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> PeriodicSurveyConverter.decode("345-abc3-2-60eeefed-567"))
                .withMessage("Can't convert key 345-abc3-2-60eeefed-567 to SurveyId because of different number" +
                        " " +
                        "of fields. Expected fields number is 4, but actual was 5");
    }

    @Test
    void testDecodeEmptyString() {
        Assertions.assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> PeriodicSurveyConverter.decode(""))
                .withMessage("Can't convert key  to SurveyId because of different number " +
                        "of fields. Expected fields number is 4, but actual was 1");
    }

    @Test
    void testDecodeIncorrectStringParsingException() {
        Assertions.assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> PeriodicSurveyConverter.decode("345-survey-2-60eeefed"))
                .withMessage("Can't convert key 345-survey-2-60eeefed to SurveyId. Parsing error occured: For " +
                        "input string: \"survey\"");
    }

    @Test
    void testDecodeStringWithIncorrectSurveyType() {
        Assertions.assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> PeriodicSurveyConverter.decode("345-abc3-8-60eeefed"))
                .withMessage("Incorrect survey type 8");
    }
}
