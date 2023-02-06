package ru.yandex.market.promoboss.validator.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.promoboss.validator.exception.StatusValidationException;
import ru.yandex.mj.generated.server.model.PromoMainRequestParams;
import ru.yandex.mj.generated.server.model.PromoRequestV2;
import ru.yandex.mj.generated.server.model.PromoStatus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StatusCreateRequestValidatorV2Test {

    private static final StatusCreateRequestValidator validator = new StatusCreateRequestValidator();

    @Test
    public void shouldNotThrowExceptionIfStatusIsNull() {

        // setup
        PromoRequestV2 request = new PromoRequestV2();

        // act and verify
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    public void shouldNotThrowExceptionIfStatusIsNew() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .main(
                        new PromoMainRequestParams()
                                .status(PromoStatus.NEW)
                );

        // act and verify
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @ParameterizedTest(name = "{0} status is invalid to create promo")
    @EnumSource(
            value = PromoStatus.class,
            names = {"NEW"},
            mode = EnumSource.Mode.EXCLUDE)
    public void shouldThrowExceptionIfStatusIsNotNew(PromoStatus status) {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .main(
                        new PromoMainRequestParams()
                                .status(status)
                );

        // act and verify
        StatusValidationException e = assertThrows(StatusValidationException.class,
                () -> validator.validate(request));

        assertEquals("Invalid status in the request", e.getMessage());
    }
}
