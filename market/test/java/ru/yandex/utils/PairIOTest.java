package ru.yandex.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * todo описать предназначение.
 *
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
public class PairIOTest {
    @Test
    public void testEquals() {
        final int first = 100;
        final String second = "200";
        assertEquals(new PairIO<>(first, second), new PairIO<>(first, second));
        final String anotherSecond = "20".concat("0");
        assertFalse(second == anotherSecond);
        assertTrue(second.equals(anotherSecond));
        assertEquals(new PairIO<>(first, second), new PairIO<>(first, anotherSecond));
    }
}
