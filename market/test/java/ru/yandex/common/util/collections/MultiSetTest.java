package ru.yandex.common.util.collections;

import junit.framework.TestCase;

/**
 * Author: Yury Chuyko mrgrey@yandex-team.ru
 * Date: 01.03.11
 */
public class MultiSetTest extends TestCase {

    public void testValueCount() throws Exception {
        final MultiSet<Integer, Integer> multiSet = Cf.newMultiSet();
        assertEquals(0, multiSet.valueCount());
        multiSet.add(1, 1);
        assertEquals(1, multiSet.valueCount());
        multiSet.add(1, 1);
        assertEquals(1, multiSet.valueCount());
        multiSet.add(1, 2);
        assertEquals(2, multiSet.valueCount());
        multiSet.add(2, 1);
        assertEquals(3, multiSet.valueCount());
        multiSet.addAll(2, Cf.list(2, 3, 4));
        assertEquals(6, multiSet.valueCount());
        multiSet.addAll(2, Cf.list(5, 6, 7));
        assertEquals(9, multiSet.valueCount());
        multiSet.addAll(2, Cf.<Integer>list());
        assertEquals(9, multiSet.valueCount());
        multiSet.addAll(2, Cf.list(2, 5));
        assertEquals(9, multiSet.valueCount());
        multiSet.addAll(2, Cf.list(5, 8));
        assertEquals(10, multiSet.valueCount());
    }

}
