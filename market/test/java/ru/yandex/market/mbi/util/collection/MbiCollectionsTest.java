package ru.yandex.market.mbi.util.collection;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.mbi.util.collection.MbiCollections.greatestCommonStringPrefix;
import static ru.yandex.market.mbi.util.collection.MbiCollections.hasDuplicates;

/**
 * Тесты для {@link MbiCollections}.
 */
@ParametersAreNonnullByDefault
class MbiCollectionsTest {

    @Test
    void testGreatestCommonStringPrefix() {
        assertEquals("", greatestCommonStringPrefix("", "a"));
        assertEquals("", greatestCommonStringPrefix("", "aa"));
        assertEquals("a", greatestCommonStringPrefix("a", "a"));
        assertEquals("aa", greatestCommonStringPrefix("aa", "aa"));
        assertEquals("", greatestCommonStringPrefix("sdflkl", "asfkksdk"));
        assertEquals("", greatestCommonStringPrefix("welrtoiwu", "aaireotie"));
        assertEquals("a", greatestCommonStringPrefix("autyr", "aerture"));
        assertEquals("aa", greatestCommonStringPrefix("aa", "aa"));
        assertEquals("", greatestCommonStringPrefix("aa", "bb"));
        assertEquals("a", greatestCommonStringPrefix("aa", "ab"));
        assertEquals("a", greatestCommonStringPrefix("aa", "ab"));
        assertEquals("abc", greatestCommonStringPrefix("abcasfj", "abcdklsghl", "abcksahflahf"));
        assertEquals("", greatestCommonStringPrefix("", "a", "bc"));
        assertEquals("", greatestCommonStringPrefix("aa", "bb", "cc"));
        assertEquals("", greatestCommonStringPrefix("aa", "ab", "cc"));
        assertEquals("a", greatestCommonStringPrefix("aa", "ab", "ac"));
        assertEquals("abc", greatestCommonStringPrefix("abcasfj", "abcdklsghl", "abcksahflahf"));
    }

    @Test
    void testHasDuplicates() {
        final Function<Integer, Integer> f = x -> x;

        assertFalse(hasDuplicates(null, f));
        assertFalse(hasDuplicates(Arrays.asList(), f));
        assertFalse(hasDuplicates(Arrays.asList(1), f));
        assertFalse(hasDuplicates(Arrays.asList(8, 9, 1), f));
        assertFalse(hasDuplicates(Collections.singletonList(null), f));

        assertTrue(hasDuplicates(Arrays.asList(8, 9, 1, 8), f));
        assertTrue(hasDuplicates(Arrays.asList(8, 8), f));
        assertTrue(hasDuplicates(Arrays.asList(null, null), f));
    }
}
