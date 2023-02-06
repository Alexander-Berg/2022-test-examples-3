package ru.yandex.common.util;

import junit.framework.TestCase;

import java.util.Arrays;

/**
 * Date: 3/10/11
 *
 * @author Alexander Astakhov (leftie@yandex-team.ru)
 */
public class ChecksumsTest extends TestCase {

    public void testTwoMd5sDiffs() {
        final byte[] bytes1 = "abcdefghijklmnopqrstuvwxyz".getBytes();
        final byte[] bytes2 = "12345678901234567890".getBytes();
        assertFalse(Arrays.equals(bytes1, bytes2));
        assertFalse(Arrays.equals(Checksums.md5(bytes1, 0, bytes1.length), Checksums.md5(bytes2, 0, bytes2.length)));
    }

    public void testMd5AllString() {
        final byte[] bytes = "abcdefghijklmnopqrstuvwxyz".getBytes();
        assertTrue(Arrays.equals(Checksums.md5(bytes), Checksums.md5(bytes, 0, bytes.length)));
        assertFalse(Arrays.equals(Checksums.md5(bytes), Checksums.md5(bytes, 0, bytes.length - 5)));
    }

    public void testMd5ToString() {
        assertEquals(
            "9e107d9d372bb6826bd81d3542a419d6",
            Checksums.toHexString(
                Checksums.md5("The quick brown fox jumps over the lazy dog".getBytes())));
    }

    public void testMd5EmptyString() {
        assertEquals(
            "d41d8cd98f00b204e9800998ecf8427e",
            Checksums.toHexString(
                Checksums.md5("".getBytes())));
    }
    
    public void testSha512() {
        assertEquals(
                "67b39c6d9324779523270efb74733c7a195dbfd4afcf0d2085cd2e61b8365af9b8b6e8b239f87509353c9d47c84ce09c3a3c3d9ba56f7c2f7917254d75000903",
                Checksums.toHexString(
                        Checksums.sha512("Can calcualte a sha512 hash of me ASAP, please?".getBytes())
                        )
        );
    }
}
