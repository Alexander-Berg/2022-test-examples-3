package ru.yandex.market.promoboss.validator.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.mj.generated.server.model.PromoMainRequestParams;
import ru.yandex.mj.generated.server.model.PromoRequestV2;
import ru.yandex.mj.generated.server.model.PromoStatus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class StatusUpdateRequestValidatorV2Test {

    private static final StatusUpdateRequestValidator validator = new StatusUpdateRequestValidator();

    @Test
    public void shouldNotThrowExceptionIfStatusIsNull() {

        // setup
        PromoRequestV2 request = new PromoRequestV2();

        // act and verify
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @ParameterizedTest(name = "{0} status is valid to update promo")
    @EnumSource(value = PromoStatus.class)
    public void shouldNotThrowExceptionIfStatusIsValid(PromoStatus status) {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .main(
                        new PromoMainRequestParams()
                                .status(status)
                );

        // act and verify
        assertDoesNotThrow(() -> validator.validate(request));
    }
}
