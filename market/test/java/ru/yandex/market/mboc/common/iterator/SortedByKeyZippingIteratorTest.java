package ru.yandex.market.mboc.common.iterator;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import static org.apache.commons.collections4.IteratorUtils.arrayIterator;
import static org.apache.commons.collections4.IteratorUtils.emptyIterator;

/**
 * @author danfertev
 * @since 28.05.2020
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SortedByKeyZippingIteratorTest {
    @Test
    public void testEmpty() {
        var iterator = create(emptyIterator(), emptyIterator());
        assertEmpty(iterator);
    }

    @Test
    public void testEmptyFirst() {
        var iterator = create(emptyIterator(), iterator("1"));
        assertNext(iterator, Pair.of(null, "1"));
        assertEmpty(iterator);
    }

    @Test
    public void testEmptySecond() {
        var iterator = create(iterator(1), emptyIterator());
        assertNext(iterator, Pair.of(1, null));
        assertEmpty(iterator);
    }

    @Test
    public void testEqual() {
        var iterator = create(iterator(1), iterator("1"));
        assertNext(iterator, Pair.of(1, "1"));
        assertEmpty(iterator);
    }

    @Test
    public void testFirstLessThanSecond() {
        var iterator = create(iterator(1), iterator("2"));
        assertNext(iterator, Pair.of(1, null));
        assertNext(iterator, Pair.of(null, "2"));
        assertEmpty(iterator);
    }

    @Test
    public void testFirstGreaterThanSecond() {
        var iterator = create(iterator(2), iterator("1"));
        assertNext(iterator, Pair.of(null, "1"));
        assertNext(iterator, Pair.of(2, null));
        assertEmpty(iterator);
    }

    @Test
    public void testMultipleFirst() {
        var iterator = create(iterator(1, 2, 3), emptyIterator());
        assertNext(iterator, Pair.of(1, null));
        assertNext(iterator, Pair.of(2, null));
        assertNext(iterator, Pair.of(3, null));
        assertEmpty(iterator);
    }

    @Test
    public void testMultipleSecond() {
        var iterator = create(emptyIterator(), iterator("1", "2", "3"));
        assertNext(iterator, Pair.of(null, "1"));
        assertNext(iterator, Pair.of(null, "2"));
        assertNext(iterator, Pair.of(null, "3"));
        assertEmpty(iterator);
    }

    @Test
    public void testMultiple() {
        var iterator = create(iterator(1, 2, 4, 5, 6), iterator("2", "3", "4"));
        assertNext(iterator, Pair.of(1, null));
        assertNext(iterator, Pair.of(2, "2"));
        assertNext(iterator, Pair.of(null, "3"));
        assertNext(iterator, Pair.of(4, "4"));
        assertNext(iterator, Pair.of(5, null));
        assertNext(iterator, Pair.of(6, null));
        assertEmpty(iterator);
    }

    @Test
    public void testNonUniqueAndNotSorted() {
        var iterator = create(iterator(5, 1, 2, 6), iterator("1", "1", "6"));
        assertNext(iterator, Pair.of(null, "1"));
        assertNext(iterator, Pair.of(null, "1"));
        assertNext(iterator, Pair.of(5, null));
        assertNext(iterator, Pair.of(1, null));
        assertNext(iterator, Pair.of(2, null));
        assertNext(iterator, Pair.of(6, "6"));
        assertEmpty(iterator);
    }

    private SortedByKeyZippingIterator<Long, Integer, String> create(Iterator<Integer> t1Iterator,
                                                                     Iterator<String> t2Iterator) {
        return SortedByKeyZippingIterator.create(
            t1Iterator, t2Iterator,
            Long::compareTo,
            Integer::longValue,
            Long::parseLong
        );
    }

    private <T> Iterator<T> iterator(T... elements) {
        return arrayIterator(elements);
    }

    private <T> void assertEmpty(Iterator<T> iterator) {
        Assertions.assertThat(iterator.hasNext()).isFalse();
        Assertions.assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
    }

    private <T> void assertNext(Iterator<T> iterator, T next) {
        Assertions.assertThat(iterator.hasNext()).isTrue();
        Assertions.assertThat(iterator.next()).isEqualTo(next);
    }
}
