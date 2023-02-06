package ru.yandex.collection;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class ChunkedIntListTest extends TestBase {
    // CSOFF: MagicNumber
    @Test
    public void test() throws Exception {
        //test initial fill
        int size = 16;
        ChunkedIntList list = new ChunkedIntList(size, true);
        for (int i = 0; i < size; i++) {
            list.setInt(i, i);
        }
        for (int i = 0; i < size; i++) {
            Assert.assertEquals(i, list.getInt(i));
        }
        size = ChunkedIntList.CHUNK_SIZE + 16;
        list = new ChunkedIntList(size, true);
        for (int i = 0; i < size; i++) {
            list.setInt(i, i);
        }
        for (int i = 0; i < size; i++) {
            Assert.assertEquals(i, list.getInt(i));
        }
    }
    // CSON: MagicNumber
}

