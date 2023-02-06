package ru.yandex.market.checkout.checkouter.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Denis Chernyshov <zoom@yandex-team.ru>
 */
public class PhoneValidatorTest {

    private final PhoneValidator validator = new PhoneValidator();

    @Test
    public void shouldInvalidateNullOrEmptyPhone() {
        assertFalse(validator.isValid(null));
        assertFalse(validator.isValid(""));
    }

    @Test
    public void shouldValidateCorrectPhones() {
        assertTrue(validator.isValid("+70001234567"));
        assertTrue(validator.isValid("+380123456789"));
        assertTrue(validator.isValid("+375123456789"));
    }

    @Test
    public void shouldInvalidateIncorrectPhoneLength() {
        assertFalse(validator.isValid("+7"));
        assertFalse(validator.isValid("+7000123456"));
        assertFalse(validator.isValid("+700012345678"));

        assertFalse(validator.isValid("+380"));
        assertFalse(validator.isValid("+38012345678"));
        assertFalse(validator.isValid("+3801234567891"));

        assertFalse(validator.isValid("+3751234567890"));
        assertFalse(validator.isValid("+37512345678"));
    }

    @Test
    public void shouldIgnoreSymbols() {
        assertTrue(validator.isValid("+70001234567a"));
        assertTrue(validator.isValid("+7000qa1234567"));
        assertTrue(validator.isValid("+7(000)123-45-67"));
        assertTrue(validator.isValid("+380123456789a"));
        assertTrue(validator.isValid("+3801qw23456789"));
        assertTrue(validator.isValid("+380(123)456-789"));
    }
}
