package ru.yandex.common.util.collections;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Tests for priority bag.
 *
 * @author maxkar
 *
 */
public class PriorityBagTest {
    /**
     * Tests, that elemnts are consistent with the removal.
     */
    @Test
    public void testRemoval() {
        final PriorityBag<String> pb = PriorityBag.newInsertionOrderedPriorityBag();
        pb.add("test");
        pb.remove("test");
        Assert.assertFalse(pb.inDescendingOrder().iterator().hasNext());
    }
}
