package ru.yandex.market.pers.notify;


import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmailValidationTest {

    @Test
    public void russianEmailIsValid() {
        assertTrue(isValid("леха@почта.рф"));
    }

    @Test
    public void commonEmailIsValid() {
        assertTrue(isValid("a-danilov@yandex-team.ru"));
    }

    @Test
    public void longEmailIsValid() {
        assertTrue(isValid("14ebe954-f791-4a97-a7f6-ecc1f343f950@f8274f3d-6eab-4c98-8cee-9636aea0cbe9.ru"));
    }

    @Test
    public void tooLongEmailIsInvalid() {
        assertFalse(isValid("14ebe954-f791-4a97-a7f6-ecc1f343f950@14ebe954-f791-4a97-a7f6-ecc1f343f950f8274f3d-6eab-4c98-8cee-9636aea0cbe9.ru"));
    }

    @Test
    public void emailWithSpaceIsInvalid() {
        assertFalse(isValid("user user@example.com"));
        assertFalse(isValid("user@exam\tple.com"));
    }

    @Test
    public void emailWithDoubleAtIsInvalid() {
        assertFalse(isValid("user@user@example.com"));
    }

    @Test
    public void emailWithoutDomainIsInvalid() {
        assertFalse(isValid("user"));
        assertFalse(isValid("user@example"));
        assertFalse(isValid("user@"));
    }

    @Test
    public void nullEmailIsInvalid() {
        assertFalse(isValid(null));
    }

    @Test
    public void emptyOrBlankEmailIsInvalid() {
        assertFalse(isValid(""));
        assertFalse(isValid(" "));
        assertFalse(isValid("\t\n"));
    }

    @Test
    public void injectionIsInvalidEmail() {
        assertFalse(isValid("\"><svg/onload=alert()>"));
    }

    private boolean isValid(String email) {
        return EmailValidationUtils.isValidEmail(email);
    }

}
