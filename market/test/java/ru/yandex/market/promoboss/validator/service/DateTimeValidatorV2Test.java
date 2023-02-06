package ru.yandex.market.promoboss.validator.service;

import java.time.Duration;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import ru.yandex.market.promoboss.validator.exception.DateTimeValidationException;
import ru.yandex.mj.generated.server.model.PromoMainRequestParams;
import ru.yandex.mj.generated.server.model.PromoRequestV2;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DateTimeValidatorV2Test {

    private static final DateTimeValidator validator = new DateTimeValidator();

    @Test
    public void shouldSuccessValidate() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .main(
                        new PromoMainRequestParams()
                                .startAt(OffsetDateTime.now().minus(Duration.ofHours(1L)).toEpochSecond())
                                .endAt(OffsetDateTime.now().toEpochSecond())
                );

        // act and verify
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    public void shouldThrowExceptionWhenStartDateIsEmpty() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .main(
                        new PromoMainRequestParams()
                                .endAt(OffsetDateTime.now().toEpochSecond())
                );

        // act and verify
        DateTimeValidationException exception = assertThrows(DateTimeValidationException.class,
                () -> validator.validate(request));

        assertEquals("Start date is not passed or incorrect", exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionWhenEndDateIsEmpty() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .main(
                        new PromoMainRequestParams()
                                .startAt(OffsetDateTime.now().minus(Duration.ofHours(1L)).toEpochSecond())
                );

        // act and verify
        DateTimeValidationException exception = assertThrows(DateTimeValidationException.class,
                () -> validator.validate(request));

        assertEquals("End date is not passed or incorrect", exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionWhenEndDateLessThenStartDateEmpty() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .main(
                        new PromoMainRequestParams()
                                .startAt(OffsetDateTime.now().toEpochSecond())
                                .endAt(OffsetDateTime.now().minus(Duration.ofHours(1L)).toEpochSecond())
                );

        // act and verify
        DateTimeValidationException exception = assertThrows(DateTimeValidationException.class,
                () -> validator.validate(request));

        assertEquals("Start date is after the end date", exception.getMessage());
    }
}
