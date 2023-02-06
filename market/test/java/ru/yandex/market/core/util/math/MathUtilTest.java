package ru.yandex.market.core.util.math;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexey Shevenkov ashevenkov@yandex-team.ru
 */
public class MathUtilTest {

    @Test
    public void testMod() throws Exception {
        assertEquals(MathUtil.divHigh(5, 2), 3);
        assertEquals(MathUtil.divHigh(6, 2), 3);
        assertEquals(MathUtil.divHigh(4, 2), 2);
    }

}
