package ru.yandex.util.string;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class StringUtilsTest extends TestBase {
    public StringUtilsTest() {
        super(false, 0L);
    }

    @Test
    public void testStringHashCode() {
        int expected = "Hello, world".hashCode();
        Assert.assertEquals(
            expected,
            StringUtils.updateHashCode(
                StringUtils.updateHashCode(
                    StringUtils.updateHashCode(
                        StringUtils.updateHashCode("Hell".hashCode(), "o,"),
                        ' '),
                    'w'),
                "orld"));
        Assert.assertEquals(
            expected,
            StringUtils.updateHashCode(
                StringUtils.updateHashCode(
                    StringUtils.updateHashCode('H', 'e'),
                    "llo, worl"),
                'd'));
        Assert.assertEquals(
            "http://dpotapov@yandex.ru:8080/path/here".hashCode(),
            StringUtils.updateHashCode(
                "http://dpotapov@yandex.ru:8080".hashCode(),
                "/path/here"));
    }

    @Test
    public void testStringHashCodeCyrillic() {
        int expected = "Привет, мир".hashCode();
        Assert.assertEquals(
            expected,
            StringUtils.updateHashCode(
                StringUtils.updateHashCode(
                    StringUtils.updateHashCode(
                        StringUtils.updateHashCode("Прив".hashCode(), "ет,"),
                        ' '),
                    'м'),
                "ир"));
        Assert.assertEquals(
            expected,
            StringUtils.updateHashCode(
                StringUtils.updateHashCode(
                    StringUtils.updateHashCode('П', 'р'),
                    "ивет, ми"),
                'р'));
    }

    @Test
    public void testTrimSequences() {
        Assert.assertSame(
            "12345",
            StringUtils.trimSequences("12345", Character::isAlphabetic, 3));
        Assert.assertSame(
            "123aa123",
            StringUtils.trimSequences("123aa123", Character::isAlphabetic, 3));
        Assert.assertSame(
            "12345",
            StringUtils.trimSequences("12345", Character::isDigit, 8));
        Assert.assertSame(
            "123aa123",
            StringUtils.trimSequences("123aa123", Character::isDigit, 8));
        Assert.assertSame(
            "123",
            StringUtils.trimSequences("123", Character::isDigit, 3));
        Assert.assertEquals(
            "123",
            StringUtils.trimSequences("12345", Character::isDigit, 3));
        Assert.assertEquals(
            "aa123aa",
            StringUtils.trimSequences("aa12345aa", Character::isDigit, 3));
        Assert.assertEquals(
            "aa",
            StringUtils.trimSequences("aa12345", Character::isDigit, 0));
        Assert.assertEquals(
            "aaaa",
            StringUtils.trimSequences("aa12345aa", Character::isDigit, 0));

        Assert.assertEquals(
            "123aa456aa789aa",
            StringUtils.trimSequences(
                "12300aa45600aa7890aa",
                Character::isDigit,
                3));
        Assert.assertEquals(
            "aa123aa456aa789",
            StringUtils.trimSequences(
                "aa12300aa45600aa7890",
                Character::isDigit,
                3));
        Assert.assertEquals(
            "123aa456aa78",
            StringUtils.trimSequences(
                "12300aa45600aa78",
                Character::isDigit,
                3));
    }
}

