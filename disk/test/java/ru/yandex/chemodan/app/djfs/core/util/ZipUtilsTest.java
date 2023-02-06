package ru.yandex.chemodan.app.djfs.core.util;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class ZipUtilsTest {

    // Magic header meaning:
    // 78 01 - No Compression/low
    // 78 9C - Default Compression
    // 78 DA - Best Compression
    private static final byte[] BYTES_1 = new byte[]{120, 1, -85, -82, 5, 0, 1, 117, 0, -7};
    private static final byte[] BYTES_2 = new byte[]{120, 1, -85, 86, -54, 77, 45, 73, 84, -78, -86, 86, 74, 46,
            -55, -52, 77, 85, -78, 50, 52, -79, -76, 52, 52, 48, 48, 49, 51, -87, -83, 5, 0, -119, 103, 8, -84};

    @Test
    public void compressString() {
        Assert.arraysEquals(BYTES_1, ZipUtils.compressString("{}", 1));
    }

    @Test
    public void decompressToString() {
        Assert.equals("{}", ZipUtils.decompressToString(BYTES_1));
    }

    @Test
    public void compressString2() {
        Assert.arraysEquals(BYTES_2, ZipUtils.compressString("{\"meta\":{\"ctime\":1499100464}}", 1));
    }

    @Test
    public void decompressToString2() {
        Assert.equals("{\"meta\":{\"ctime\":1499100464}}", ZipUtils.decompressToString(BYTES_2));
    }
}
