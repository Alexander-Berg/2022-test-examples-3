package ru.yandex.market.markup2.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author inenakhov
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class StatUtilsTest {
    @Test
    public void sampleSize() throws Exception {
        assertEquals(1501, StatUtils.sampleSize(4000, 0.5, 0.02, 1.96));
    }
}
