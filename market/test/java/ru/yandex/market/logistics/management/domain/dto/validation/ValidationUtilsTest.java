package ru.yandex.market.logistics.management.domain.dto.validation;

import java.time.LocalTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ValidationUtilsTest {


    @Test
    public void testGetPropertyValue() {
        LetterTestClass letter = new LetterTestClass("mySender", new LetterTestClass.Contents("myTitle"));
        LetterTestClass letterWithoutContents = new LetterTestClass(null, null);

        Assertions.assertThrows(IllegalStateException.class,
            () -> ValidationUtils.getPropertyValue(letter, "rocket", Object.class));

        Assertions.assertThrows(IllegalStateException.class,
            () -> ValidationUtils.getPropertyValue(letter, "rocket.science", Object.class));

        Assertions.assertThrows(IllegalStateException.class,
            () -> ValidationUtils.getPropertyValue(letter, "sender", Integer.class));

        Assertions.assertEquals("mySender", ValidationUtils.getPropertyValue(letter, "sender", String.class));
        Assertions.assertEquals("myTitle", ValidationUtils.getPropertyValue(letter, "contents.title", String.class));
        Assertions.assertNull(ValidationUtils.getPropertyValue(letterWithoutContents, "sender", String.class));
        Assertions.assertNull(ValidationUtils.getPropertyValue(letterWithoutContents, "contents.title", String.class));
    }

    @Test
    public void testIsDayWithin() {

        Assertions.assertTrue(ValidationUtils.isDayWithin(
            LocalTime.of(0, 0),
            LocalTime.of(0, 0),
            LocalTime.of(0, 0),
            LocalTime.of(0, 0)
        ));

        Assertions.assertTrue(ValidationUtils.isDayWithin(
            LocalTime.of(0, 0),
            LocalTime.of(0, 0),
            LocalTime.of(9, 0),
            LocalTime.of(0, 0)
        ));

        Assertions.assertTrue(ValidationUtils.isDayWithin(
            LocalTime.of(0, 0),
            LocalTime.of(0, 0),
            LocalTime.of(9, 0),
            LocalTime.of(10, 0)
        ));

        Assertions.assertTrue(ValidationUtils.isDayWithin(
            LocalTime.of(0, 0),
            LocalTime.of(10, 0),
            LocalTime.of(9, 0),
            LocalTime.of(10, 0)
        ));

        Assertions.assertFalse(ValidationUtils.isDayWithin(
            LocalTime.of(0, 0),
            LocalTime.of(10, 0),
            LocalTime.of(9, 0),
            LocalTime.of(11, 0)
        ));

        Assertions.assertFalse(ValidationUtils.isDayWithin(
            LocalTime.of(9, 0),
            LocalTime.of(0, 0),
            LocalTime.of(0, 0),
            LocalTime.of(0, 0)
        ));

        Assertions.assertFalse(ValidationUtils.isDayWithin(
            null,
            null,
            LocalTime.of(0, 0),
            LocalTime.of(0, 0)
        ));

        Assertions.assertTrue(ValidationUtils.isDayWithin(
            LocalTime.of(9, 0),
            LocalTime.of(0, 0),
            null,
            null
        ));
    }
}
