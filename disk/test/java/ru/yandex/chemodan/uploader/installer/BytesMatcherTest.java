package ru.yandex.chemodan.uploader.installer;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class BytesMatcherTest {

    @Test
    public void findPatternIndexFromEnd() throws Exception {
        byte[] data = new byte[]{3, 2, 1, 3, 2, 0, 0, 0, 2};
        BytesMatcher m = new BytesMatcher();

        Assert.equals(7, m.findPatternIndexFromEnd(data, new byte[]{0, 2}));
        Assert.equals(3, m.findPatternIndexFromEnd(data, new byte[]{3, 2}));
        Assert.equals(2, m.findPatternIndexFromEnd(data, new byte[]{1, 3, 2}));
        Assert.equals(0, m.findPatternIndexFromEnd(data, new byte[]{3, 2, 1}));
        Assert.equals(-1, m.findPatternIndexFromEnd(data, new byte[]{3, 0, 1}));
        Assert.equals(7, m.findPatternIndexFromEnd(data, new byte[]{0}));
    }
}
