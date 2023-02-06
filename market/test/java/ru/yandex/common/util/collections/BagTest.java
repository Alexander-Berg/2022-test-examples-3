package ru.yandex.common.util.collections;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Tests for bag.
 *
 * @author ssimonchik@yandex-team.ru
 */
public class BagTest {

    public void checkBag(Bag<String> b) {
        String a1 = "a1";
        String a2 = "a2";
        b.add(a1);
        Assert.assertTrue(b.cardinality() == 1 && b.get(a1) == 1);
        b.remove(a1);
        Assert.assertTrue(b.isEmpty());
        b.add(a1, 2);
        Assert.assertTrue(b.cardinality() == 2 && b.get(a1) == 2);
        b.add(a2, 2);
        Assert.assertTrue(b.cardinality() == 4 && b.get(a1) == 2 && b.get(a2) == 2);
        b.clear(a1);
        Assert.assertTrue(b.cardinality() == 2 && b.get(a1) == 0 && b.get(a2) == 2);

        Bag<String> b2 = Bag.newHashBag();
        b2.add(a1, 5);
        b2.add(a2, 6);
        b2.remove(a1, 2);
        b2.remove(a2, 2);
        b.union(b2);
        Assert.assertTrue(b.cardinality() == 7 && b.get(a1) == 3 && b.get(a2) == 4);

        b2.remove(a1, 3);
        Assert.assertTrue(b2.cardinality() == 4 && b2.get(a1) == 0 && b2.get(a2) == 4);
        b2.add(a1);
        b2.add(a2);
        Assert.assertTrue(b2.cardinality() == 6 && b2.get(a1) == 1 && b2.get(a2) == 5);
        b.intersect(b2);
        Assert.assertTrue(b.cardinality() == 5 && b.get(a1) == 1 && b.get(a2) == 4);

        b.addAll(b2);
        Assert.assertTrue(b.cardinality() == 11 && b.get(a1) == 2 && b.get(a2) == 9);

        b.removeAll(b2);
        Assert.assertTrue(b.cardinality() == 5 && b.get(a1) == 1 && b.get(a2) == 4);

        b.removeAll(b2);
        Assert.assertTrue(b.cardinality() == 0 && b.get(a1) == 0 && b.get(a2) == 0);

        b.removeAll(b2);
        Assert.assertTrue(b.cardinality() == 0 && b.get(a1) == 0 && b.get(a2) == 0);

        b.addAll(b2);
        Assert.assertTrue(b.cardinality() == 6 && b.get(a1) == 1 && b.get(a2) == 5);

        b.remove(a1, 10);
        b.remove(a2, 10);
        Assert.assertTrue(b.isEmpty());
    }

    @Test
    public void testHashBag() {
        checkBag(Bag.<String>newHashBag());
    }

    @Test
    public void testInsertionOrderedBag() {
        checkBag(Bag.<String>newInsertionOrderedBag());
    }
}
