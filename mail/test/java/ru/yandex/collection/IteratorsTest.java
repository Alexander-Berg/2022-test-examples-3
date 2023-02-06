package ru.yandex.collection;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class IteratorsTest extends TestBase {
    // CSOFF: MagicNumber
    @Test
    public void test() throws Exception {
        // Check toList
        Assert.assertEquals(Arrays.asList(1, 2, 3),
            Iterators.toList(Arrays.asList(1, 2, 3).iterator()));
        YandexAssert.assertNotEquals(
            Arrays.asList(1, 2, 3),
            Iterators.toList(Arrays.asList(3, 2, 1).iterator()));
        // Check randomIterator
        List<Integer> original = Arrays.asList(1, 2, 3, 4, 5);
        Set<List<Integer>> random = new HashSet<>();
        for (int hash = 0; hash < 100; ++hash) {
            random.add(
                Iterators.toList(
                    Iterators.randomIterator(original, hash)));
        }
        YandexAssert.assertNotLess(4, random.size());
        for (List<Integer> check: random) {
            Assert.assertEquals(
                new HashSet<Integer>(original),
                new HashSet<Integer>(check));
        }
    }
    // CSON: MagicNumber
}

