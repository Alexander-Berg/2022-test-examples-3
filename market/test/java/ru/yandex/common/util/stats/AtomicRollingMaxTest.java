package ru.yandex.common.util.stats;

import junit.framework.TestCase;

/**
 * Date: 4/18/11
 *
 * @author Alexander Astakhov (leftie@yandex-team.ru)
 */
public class AtomicRollingMaxTest extends TestCase {
    public void testSimpleAdd() {
        final AtomicRollingMax max = new AtomicRollingMax();
        max.add(1);
        assertEquals(1, max.getMax());
        max.add(50);
        assertEquals(50, max.getMax());
        max.add(30);
        assertEquals(50, max.getMax());
        max.add(100);
        assertEquals(100, max.getMax());
    }
}
