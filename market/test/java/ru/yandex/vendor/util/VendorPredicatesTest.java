package ru.yandex.vendor.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.vendor.util.VendorPredicates.byProperty;
import static ru.yandex.vendor.util.VendorPredicates.containsIgnoreCase;
import static ru.yandex.vendor.util.VendorPredicates.distinctBy;
import static ru.yandex.vendor.util.VendorPredicates.startsWithIgnoreCase;

@RunWith(JUnit4.class)
public class VendorPredicatesTest {

    @Test
    public void testDistinctBy() {
        List<String> list = Stream.of("qwe", "rty", "qw", "rt", "qaz", "q")
                .filter(distinctBy(String::length)).collect(toList());
        assertEquals(asList("qwe", "qw", "q"), list);
    }

    @Test
    public void testStartsWithIgnoreCase() {
        Predicate<Integer> p = startsWithIgnoreCase(String::valueOf, "42");
        assertTrue(p.test(4234));
        assertTrue(p.test(4255));
        assertTrue(p.test(42111111));
        assertTrue(p.test(42424242));
        assertFalse(p.test(24242));
        assertFalse(p.test(114242));
        assertFalse(p.test(333333));
    }

    @Test
    public void testContainsIgnoreCase() {
        Predicate<Integer> p = containsIgnoreCase(String::valueOf, "42");
        assertTrue(p.test(4234));
        assertTrue(p.test(3442));
        assertTrue(p.test(2424));
        assertTrue(p.test(333333342));
        assertFalse(p.test(23432));
        assertFalse(p.test(55545323));
        assertFalse(p.test(243243));
    }

    @Test
    public void testStartsWithIgnoreCase_IgnoreCase() {
        Predicate<List<String>> p = startsWithIgnoreCase(String::valueOf, "[qwe");
        assertTrue(p.test(asList("qwe", "rty")));
        assertTrue(p.test(asList("QWE", "rty")));
        assertTrue(p.test(asList("QwE", "rty")));
        assertFalse(p.test(asList("rty", "qwe")));
        assertFalse(p.test(asList("rty", "QWE")));
        assertFalse(p.test(asList("QwQ", "rty")));
        assertFalse(p.test(asList("wEr", "rty")));
    }

    @Test
    public void testContainsIgnoreCase_IgnoreCase() {
        Predicate<List<String>> p = containsIgnoreCase(String::valueOf, "qwe");
        assertTrue(p.test(asList("qwe", "rty")));
        assertTrue(p.test(asList("QWE", "rty")));
        assertTrue(p.test(asList("QwE", "rty")));
        assertTrue(p.test(asList("rty", "qwe")));
        assertTrue(p.test(asList("rty", "QWE")));
        assertFalse(p.test(asList("QwQ", "rty")));
        assertFalse(p.test(asList("wEr", "rty")));
    }

    @Test
    public void test_byProperty_predicate() throws Exception {

        boolean expectedTrue = byProperty((String s) -> s.substring(0, 3), s -> {
            assertEquals("qwe", s);
            return true;
        }).test("qwerty");

        boolean expectedFalse = byProperty((String s) -> s.substring(3), s -> {
            assertEquals("rty", s);
            return false;
        }).test("qwerty");

        assertTrue(expectedTrue);
        assertFalse(expectedFalse);
    }
}
