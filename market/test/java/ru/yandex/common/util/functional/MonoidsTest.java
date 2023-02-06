package ru.yandex.common.util.functional;

import junit.framework.TestCase;
import org.junit.Test;
import ru.yandex.common.util.collections.Cf;

/**
 * Date: 9/26/11
 *
 * @author btv (btv@yandex-team.ru)
 */
public class MonoidsTest extends TestCase {
    @Test
    public void testCombine() throws Exception {
        assertEquals(0, Monoids.combine(Monoids.INT_SUM, Cf.newList()).intValue());
        assertEquals(956, Monoids.combine(Monoids.INT_SUM, Cf.list(956)).intValue());
        assertEquals(10 + 12 + (-110), Monoids.combine(Monoids.INT_SUM, Cf.list(10, 12, -110)).intValue());
    }
}
