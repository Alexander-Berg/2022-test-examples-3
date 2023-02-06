package ru.yandex.travel.commons.messaging;


import org.junit.Test;


import ru.yandex.travel.commons.proto.EMessageCodec;

import static org.junit.Assert.*;

public class CompressionUtilsTests {
    final byte[] bytes = "aaaaaaaaaabbbbbbbbbbaaaaaaaaaabbbbbbbbbb".getBytes();

    @Test
    public void testNoCompression() {
        CompressionSettings compressionSettings = new CompressionSettings(EMessageCodec.MC_NONE, 0);
        assertArrayEquals(CompressionUtils.compress(compressionSettings, bytes), bytes);
        assertArrayEquals(CompressionUtils.decompress(compressionSettings, CompressionUtils.compress(compressionSettings, bytes)), bytes);
    }

    @Test
    public void testZLibWithCompressionLevelOne() {
        CompressionSettings compressionSettings = new CompressionSettings(EMessageCodec.MC_ZLIB, 1);
        final byte[] expected = {(byte)120, (byte)1, (byte)75, (byte)76, (byte)132, (byte)129, (byte)36, (byte)56, (byte)128, (byte)137,
                                 (byte)36, (byte)38, (byte)194, (byte)133, (byte)146, (byte)0, (byte)56, (byte)33, (byte)15, (byte)61};
        assertArrayEquals(CompressionUtils.compress(compressionSettings, bytes), expected);
        assertArrayEquals(CompressionUtils.decompress(compressionSettings, expected), bytes);

    }

    @Test
    public void testZLibWithCompressionLevelNine() {
        CompressionSettings compressionSettings = new CompressionSettings(EMessageCodec.MC_ZLIB, 9);
        final byte[] expected = {(byte)120, (byte)218, (byte)75, (byte)76, (byte)132, (byte)129, (byte)36, (byte)56, (byte)72, (byte)196,
                                 (byte)34, (byte)6, (byte)0, (byte)56, (byte)33, (byte)15, (byte)61};
        assertArrayEquals(CompressionUtils.compress(compressionSettings, bytes), expected);
        assertArrayEquals(CompressionUtils.decompress(compressionSettings, expected), bytes);
    }

    @Test
    public void testZLibWithSmallBuffer() {
        final byte[] expected = {(byte)120, (byte)218, (byte)75, (byte)76, (byte)132, (byte)129, (byte)36, (byte)56, (byte)72, (byte)196,
                                 (byte)34, (byte)6, (byte)0, (byte)56, (byte)33, (byte)15, (byte)61};
        assertArrayEquals(CompressionUtils.compressZLib(9, 3, bytes), expected);
        assertArrayEquals(CompressionUtils.decompressZLib(9, expected), bytes);
    }

    @Test
    public void testZStdWithCompressionLevelOne() {
        CompressionSettings compressionSettings = new CompressionSettings(EMessageCodec.MC_ZSTD, 1);
        final byte[] expected = {(byte)40, (byte)181, (byte)47, (byte)253, (byte)32, (byte)40, (byte)125, (byte)0, (byte)0, (byte)40,
                                 (byte)97, (byte)97, (byte)98, (byte)97, (byte)98, (byte)4, (byte)16, (byte)0, (byte)69, (byte)202,
                                 (byte)21, (byte)70, (byte)180, (byte)1};
        assertArrayEquals(CompressionUtils.compress(compressionSettings, bytes), expected);
        assertArrayEquals(CompressionUtils.decompress(compressionSettings, expected), bytes);
    }

    @Test
    public void testZStdWithCompressionLevelTwentyTwo() {
        CompressionSettings compressionSettings = new CompressionSettings(EMessageCodec.MC_ZSTD, 22);
        final byte[] expected = {(byte)40, (byte)181, (byte)47, (byte)253, (byte)32, (byte)40, (byte)101, (byte)0, (byte)0, (byte)24,
                                 (byte)97, (byte)98, (byte)97, (byte)3, (byte)64, (byte)1, (byte)55, (byte)19, (byte)40, (byte)5, (byte)8};
        assertArrayEquals(CompressionUtils.compress(compressionSettings, bytes), expected);
        assertArrayEquals(CompressionUtils.decompress(compressionSettings, expected), bytes);
    }
}
