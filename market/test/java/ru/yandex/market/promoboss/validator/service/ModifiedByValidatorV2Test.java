package ru.yandex.market.promoboss.validator.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.promoboss.validator.exception.FieldsValidationException;
import ru.yandex.mj.generated.server.model.PromoRequestV2;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModifiedByValidatorV2Test {
    private final static ModifiedByValidator validator = new ModifiedByValidator();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    public void shouldThrowExceptionIfModifiedByNullOrEmptyOrBlank(String modifiedBy) {
        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .modifiedBy(modifiedBy);

        // act and verify
        FieldsValidationException exception =
                assertThrows(FieldsValidationException.class, () -> validator.validate(request));

        assertEquals("Field modifiedBy is null or blank", exception.getMessage());
    }

    @Test
    public void shouldNotThrowExceptionIfModifiedByIsValid() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .modifiedBy("modifiedBy");

        // act and verify
        assertDoesNotThrow(() -> validator.validate(request));
    }
}
