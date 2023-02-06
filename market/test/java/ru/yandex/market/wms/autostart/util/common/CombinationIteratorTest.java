package ru.yandex.market.wms.autostart.util.common;

import java.util.NoSuchElementException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CombinationIteratorTest {
    @Test
    void shouldAcceptEmptySetType0() {
        CombinationsIterator<String> iterator =
                new CombinationsIterator<>(ImmutableList.of(), CombinationType.IN_DEPTH);

        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void shouldAcceptOneValueType0() {
        CombinationsIterator<String> iterator =
                new CombinationsIterator<>(ImmutableList.of("A"), CombinationType.IN_DEPTH);

        assertTrue(iterator.hasNext());
        assertEquals(ImmutableSet.of("A"), iterator.next());

        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void shouldAcceptThreeValueType0() {
        CombinationsIterator<String> iterator =
                new CombinationsIterator<>(ImmutableList.of("A", "B", "C"), CombinationType.IN_DEPTH);

        assertTrue(iterator.hasNext());
        assertEquals(ImmutableSet.of("A"), iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals(ImmutableSet.of("B"), iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals(ImmutableSet.of("C"), iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals(ImmutableSet.of("A", "B"), iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals(ImmutableSet.of("A", "C"), iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals(ImmutableSet.of("B", "C"), iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals(ImmutableSet.of("A", "B", "C"), iterator.next());

        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void shouldAcceptManyValueType0() {
        CombinationsIterator<String> iterator =
                new CombinationsIterator<>(ImmutableList.of("A", "B", "C", "D", "E"), CombinationType.IN_DEPTH);

        int counter = 0;
        while (++counter < 5) {
            iterator.next();
        }
        assertEquals(ImmutableSet.of("E"), iterator.next());

        while (++counter < 5 + 10) {
            iterator.next();
        }
        assertEquals(ImmutableSet.of("D", "E"), iterator.next());

        while (++counter < 5 + 10 + 10) {
            iterator.next();
        }
        assertEquals(ImmutableSet.of("C", "D", "E"), iterator.next());

        while (++counter < 5 + 10 + 10 + 5) {
            iterator.next();
        }
        assertEquals(ImmutableSet.of("B", "C", "D", "E"), iterator.next());

        while (++counter < 5 + 10 + 10 + 5 + 1) {
            iterator.next();
        }
        assertEquals(ImmutableSet.of("A", "B", "C", "D", "E"), iterator.next());

        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void shouldAcceptEmptySetType1() {
        CombinationsIterator<String> iterator =
                new CombinationsIterator<>(ImmutableList.of(), CombinationType.BINARY);

        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void shouldAcceptOneValueType1() {
        CombinationsIterator<String> iterator =
                new CombinationsIterator<>(ImmutableList.of("A"), CombinationType.BINARY);

        assertTrue(iterator.hasNext());
        assertEquals(ImmutableSet.of("A"), iterator.next());

        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void shouldAcceptThreeValueType1() {
        CombinationsIterator<String> iterator =
                new CombinationsIterator<>(ImmutableList.of("A", "B", "C"), CombinationType.BINARY);

        assertTrue(iterator.hasNext());
        assertEquals(ImmutableSet.of("A"), iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals(ImmutableSet.of("B"), iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals(ImmutableSet.of("A", "B"), iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals(ImmutableSet.of("C"), iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals(ImmutableSet.of("A", "C"), iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals(ImmutableSet.of("B", "C"), iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals(ImmutableSet.of("A", "B", "C"), iterator.next());

        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void shouldAcceptEmptySetType2() {
        CombinationsIterator<String> iterator =
                new CombinationsIterator<>(ImmutableList.of(), CombinationType.STANDARD);

        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void shouldAcceptOneValueType2() {
        CombinationsIterator<String> iterator =
                new CombinationsIterator<>(ImmutableList.of("A"), CombinationType.STANDARD);

        assertTrue(iterator.hasNext());
        assertEquals(ImmutableSet.of("A"), iterator.next());

        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void shouldAcceptManyValueType2() {
        CombinationsIterator<String> iterator =
                new CombinationsIterator<>(ImmutableList.of("A", "B", "C", "D", "E"), CombinationType.STANDARD);

        assertEquals(ImmutableSet.of("A"), iterator.next());
        assertEquals(ImmutableSet.of("A", "B"), iterator.next());
        assertEquals(ImmutableSet.of("A", "B", "C"), iterator.next());
        assertEquals(ImmutableSet.of("A", "B", "C", "D"), iterator.next());
        assertEquals(ImmutableSet.of("A", "B", "C", "D", "E"), iterator.next());

        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }
}
