package ru.yandex.market.api.util.parser2.validation;

import org.junit.Test;

import ru.yandex.common.util.collections.Maybe;
import ru.yandex.market.api.util.parser2.validation.errors.GreaterEqualsConstraintViolationError;
import ru.yandex.market.api.util.parser2.validation.errors.ParsedValueValidationError;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class GreaterEqualsValidatorTest {

    GreaterEqualsValidator<Integer> validator = new GreaterEqualsValidator<>(100);

    @Test
    public void shouldValidateCorrectValue() throws Exception {
        ParsedValueValidationError error = validator.validate(Maybe.just(1000));
        assertNull(error);
    }

    @Test
    public void shouldValidateNotPresentValue() throws Exception {
        ParsedValueValidationError error = validator.validate(Maybe.nothing());
        assertNull(error);
    }

    @Test
    public void shouldNotValidateLowerValue() throws Exception {
        ParsedValueValidationError error = validator.validate(Maybe.just(99));
        assertNotNull(error);
        GreaterEqualsConstraintViolationError violationError = (GreaterEqualsConstraintViolationError) error;
        assertEquals(100, violationError.getLowerBond());
        assertEquals(99, violationError.getActualValue());
        assertEquals("Parameter does not fit range constraint (actual value = 99, min value = 100)",
                error.getMessage(null));
    }

}
