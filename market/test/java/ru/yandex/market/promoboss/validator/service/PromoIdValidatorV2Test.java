package ru.yandex.market.promoboss.validator.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.promoboss.validator.exception.PromoIdValidationException;
import ru.yandex.mj.generated.server.model.PromoRequestV2;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PromoIdValidatorV2Test {

    private final static PromoIdValidator validator = new PromoIdValidator();

    @Test
    public void shouldThrowExceptionIfPromoIdIsNull() {

        // setup
        PromoRequestV2 request = new PromoRequestV2();

        // act and verify
        PromoIdValidationException exception =
                assertThrows(PromoIdValidationException.class, () -> validator.validate(request));

        assertEquals("Promo id is empty or longer than 255", exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionIfPromoIdIsEmpty() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .promoId("");

        // act and verify
        PromoIdValidationException exception =
                assertThrows(PromoIdValidationException.class, () -> validator.validate(request));

        assertEquals("Promo id is empty or longer than 255", exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionIfPromoIdIsLongerThenAllowed() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .promoId(RandomStringUtils.random(256));

        // act and verify
        PromoIdValidationException exception =
                assertThrows(PromoIdValidationException.class, () -> validator.validate(request));

        assertEquals("Promo id is empty or longer than 255", exception.getMessage());
    }

    @Test
    public void shouldNotThrowExceptionIfPromoIdIsValid() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .promoId("123");

        // act and verify
        assertDoesNotThrow(() -> validator.validate(request));
    }
}
