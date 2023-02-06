package ru.yandex.vendor.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class SubstrTest {

    @Test
    public void contains() {
        Substr substr = Substr.contains("zzz");
        assertEquals("zzz", substr.getPattern());
        assertEquals(Substr.Type.CONTAINS, substr.getType());

        assertTrue(substr.test("zzz"));
        assertTrue(substr.test("azzz"));
        assertTrue(substr.test("zzza"));
        assertTrue(substr.test("azzza"));

        assertFalse(substr.test("azza"));
        assertFalse(substr.test("zz"));
        assertFalse(substr.test(""));
        assertFalse(substr.test(null));

        assertEquals("%zzz%", substr.toSqlLike());
    }

    @Test
    public void startsWith() {
        Substr substr = Substr.startsWith("zzz");
        assertEquals("zzz", substr.getPattern());
        assertEquals(Substr.Type.STARTS_WITH, substr.getType());

        assertTrue(substr.test("zzz"));
        assertFalse(substr.test("azzz"));
        assertTrue(substr.test("zzza"));
        assertFalse(substr.test("azzza"));

        assertFalse(substr.test("azza"));
        assertFalse(substr.test("zz"));
        assertFalse(substr.test(""));
        assertFalse(substr.test(null));

        assertEquals("zzz%", substr.toSqlLike());
    }

    @Test
    public void endsWith() {
        Substr substr = Substr.endsWith("zzz");
        assertEquals("zzz", substr.getPattern());
        assertEquals(Substr.Type.ENDS_WITH, substr.getType());

        assertTrue(substr.test("zzz"));
        assertTrue(substr.test("azzz"));
        assertFalse(substr.test("zzza"));
        assertFalse(substr.test("azzza"));

        assertFalse(substr.test("azza"));
        assertFalse(substr.test("zz"));
        assertFalse(substr.test(""));
        assertFalse(substr.test(null));

        assertEquals("%zzz", substr.toSqlLike());
    }

    @Test
    public void equals() {
        Substr substr = Substr.equals("zzz");
        assertEquals("zzz", substr.getPattern());
        assertEquals(Substr.Type.EQUALS, substr.getType());

        assertTrue(substr.test("zzz"));
        assertFalse(substr.test("azzz"));
        assertFalse(substr.test("zzza"));
        assertFalse(substr.test("azzza"));

        assertFalse(substr.test("azza"));
        assertFalse(substr.test("zz"));
        assertFalse(substr.test(""));
        assertFalse(substr.test(null));

        assertEquals("zzz", substr.toSqlLike());
    }
}