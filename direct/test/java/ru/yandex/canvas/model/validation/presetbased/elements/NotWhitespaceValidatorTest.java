package ru.yandex.canvas.model.validation.presetbased.elements;

import org.junit.Test;

import ru.yandex.canvas.model.validation.NotWhitespaceValidator;

import static org.junit.Assert.assertEquals;

public class NotWhitespaceValidatorTest {

    @Test
    public void testSingleWhitespace() {
        assertEquals(new NotWhitespaceValidator().isValid(" ", null), false);
    }

    @Test
    public void testMultipleWhitespace() {
        assertEquals(new NotWhitespaceValidator().isValid(" \t\n ", null), false);
    }

    @Test
    public void testNull() {
        assertEquals(new NotWhitespaceValidator().isValid(null, null), true);
    }

    @Test
    public void testEmpty() {
        assertEquals(new NotWhitespaceValidator().isValid("", null), true);
    }

    @Test
    public void testNotWhitespace() {
        assertEquals(new NotWhitespaceValidator().isValid(" a ", null), true);
    }

}
