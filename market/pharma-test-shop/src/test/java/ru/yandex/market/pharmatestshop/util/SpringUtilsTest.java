package ru.yandex.market.pharmatestshop.util;

import org.junit.Test;

import static org.junit.Assert.*;
import static ru.yandex.market.pharmatestshop.util.StringUtils.isNaturalNumberFormat;

public class SpringUtilsTest {

    @Test
    public void testIsNaturalNumberFormat() {
        assertTrue(isNaturalNumberFormat("1,2,3"));
        assertFalse(isNaturalNumberFormat("a1,2,3"));
        assertTrue(isNaturalNumberFormat("123"));
        assertFalse(isNaturalNumberFormat("a123"));
        assertFalse(isNaturalNumberFormat("-1, 2, 0"));
        assertTrue(isNaturalNumberFormat("1,2,"));
        assertFalse(isNaturalNumberFormat("1, 2"));
        assertTrue(isNaturalNumberFormat(""));
        assertFalse(isNaturalNumberFormat(","));
        assertFalse(isNaturalNumberFormat(" ,  "));
    }
}
