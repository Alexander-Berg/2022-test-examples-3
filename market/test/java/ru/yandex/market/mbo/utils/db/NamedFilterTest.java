package ru.yandex.market.mbo.utils.db;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


@SuppressWarnings("checkstyle:magicnumber")
public class NamedFilterTest {
    private NamedFilter where;

    @Before
    public void setup() {
        where = new NamedFilter();
    }

    @Test
    public void testAndCondition() {
        where.and("something > :something", 1);

        assertEquals("where something > :something", where.getQuery("where "));
        assertEquals("and something > :something", where.getQuery("and "));
        assertEquals(" and something > :something", where.getQuery()); // Default from filter
        assertEquals("something > :something", where.getQuery(""));

        assertEquals(ImmutableMap.of("something", 1), where.getNamedParams());

        where.and("other = :other", 2);
        assertEquals("where something > :something and other = :other", where.getQuery("where "));
        assertEquals(ImmutableMap.of("something", 1, "other", 2), where.getNamedParams());
    }

    @Test
    public void testIfNotNull() {
        where.andIfNotNull("x = :x", null);
        where.andIfNotNull("y = :y", 1);
        assertEquals("where y = :y", where.getQuery("where "));
        assertEquals(ImmutableMap.of("y", 1), where.getNamedParams());
    }

    @Test
    public void testIfNotNullConverter() {
        where.andIfNotNull("x = :x", null);
        where.andIfNotNull("y = :y", 1, y -> y + 1); // For cases like format date, etc
        assertEquals("where y = :y", where.getQuery("where "));
        assertEquals(ImmutableMap.of("y", 2), where.getNamedParams());
    }

    @Test
    public void testMultipleParams() {
        where.and("something between :a and :b", 1, 2);
        assertEquals(ImmutableMap.of("a", 1, "b", 2), where.getNamedParams());
    }

    @Test
    public void testRepeatedParamFail() {
        try {
            where.and("a = :a and b = :a", 1, 2);
            fail("Must fail, :a repeated twice");
        } catch (IllegalArgumentException ignored) {
        }

        assertUntouched();
    }

    @Test
    public void testCallWithMap() {
        where.and("a = :a and b = :a", ImmutableMap.of("a", 1));
        assertEquals("where a = :a and b = :a", where.getQuery("where "));
        assertEquals(ImmutableMap.of("a", 1), where.getNamedParams());
    }

    @Test
    public void testDoubleValuesOkIfEqual() {
        where.and("a = :a", 1)
                .and("b = :a", 1);

        assertEquals("where a = :a and b = :a", where.getQuery("where "));
        assertEquals(ImmutableMap.of("a", 1), where.getNamedParams());
    }

    @Test
    public void testDoubleValuesFailIfNotEqual() {
        try {
            where.and("a = :a", 1)
                    .and("b = :a", 2);
            fail("Must fail, :a has different items");
        } catch (IllegalArgumentException ignored) {
        }

        // Second and call failed
        assertEquals("where a = :a", where.getQuery("where "));
        assertEquals(ImmutableMap.of("a", 1), where.getNamedParams());
    }

    @Test
    public void testNotEnoughParams() {
        try {
            where.and("something between :a and :b", 1);
            fail("Must throw exception");
        } catch (IllegalArgumentException ignored) {
        }

        assertUntouched();
    }

    private void assertUntouched() {
        // State shouldn't be affected by wrong data
        assertEquals("", where.getQuery());
        assertEquals(ImmutableMap.of(), where.getNamedParams());
    }

    @Test
    public void testTooManyParams() {
        try {
            where.and("something between :a and :b", 1, 2, 3);
            fail("Must throw exception");
        } catch (IllegalArgumentException ignored) {
        }

        assertUntouched();
    }

    @Test
    public void testDifferentParams() {
        try {
            where.and("something between :a and :b", 1, 2, 3);
            fail("Must throw exception");
        } catch (IllegalArgumentException ignored) {
        }

        assertUntouched();
    }
}
