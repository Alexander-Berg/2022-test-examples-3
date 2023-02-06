package ru.yandex.collection;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class ByteArrayBitIteratorTest extends TestBase {
    // CSOFF: MagicNumber
    @Test
    public void test() {
        // Note that bits will be iterated in reverse order in byte
        byte[] data = new byte[] {(byte) 0b00101101, (byte) 0b10101010};
        Assert.assertEquals(
            "[0, 0, 1, 0, 1, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0]",
            Iterators.toList(new ByteArrayBitIterator(data)).toString());
        final int off = 5;
        Assert.assertEquals(
            "[1, 0, 1, 1, 0, 1, 0]",
            Iterators.toList(new ByteArrayBitIterator(data, off, off + 2))
                .toString());
    }
    // CSON: MagicNumber
}

