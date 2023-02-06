package ru.yandex.logger;

import org.junit.Assert;
import org.junit.Test;

public class IdGeneratorTest {
    @Test
    public void testNegative() {
        String[] ids = new String[] {
            "ZZZZZX",
            "ZZZZZY",
            "ZZZZZZ",
            "000000",
            "000001",
            "000002",
        };
        final long start = -3;
        final int iterations = 6;
        IdGenerator gen = new IdGenerator(start);
        for (int i = 0; i < iterations; ++i) {
            Assert.assertEquals(ids[i], gen.next());
        }

        final long radix = 36;
        long mask = 1;
        for (int i = 0; i < iterations; ++i) {
            mask *= radix;
        }
        final long start2 = -mask + start;
        IdGenerator gen2 = new IdGenerator(start2);
        for (int i = 0; i < iterations; ++i) {
            Assert.assertEquals(ids[i], gen2.next());
        }
    }

    @Test
    public void testMaxValue() {
        String[] ids = new String[] {
            "32E8E5",
            "32E8E6",
            "32E8E7",
            "WXLRLS",
            "WXLRLT",
            "WXLRLU",
        };
        final int iterations = 6;
        IdGenerator gen = new IdGenerator(Long.MAX_VALUE - 2);
        for (int i = 0; i < iterations; ++i) {
            Assert.assertEquals(ids[i], gen.next());
        }
    }
}

