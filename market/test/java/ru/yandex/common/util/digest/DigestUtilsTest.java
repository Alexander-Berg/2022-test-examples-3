package ru.yandex.common.util.digest;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import static org.apache.commons.codec.binary.Hex.encodeHexString;

/**
 * Date: Oct 24, 2010
 * Time: 2:19:11 AM
 *
 * @author dimas  mailto:dimas@yandex-team.ru
 */
public class DigestUtilsTest extends TestCase {

    public void testDigests() throws Exception {
        assertEquals(
                "1bc29b36f623ba82aaf6724fd3b16718",
                encodeHexString(DigestUtils.md5("md5".getBytes()))
        );
        assertEquals(
                "415ab40ae9b7cc4e66d6769cb2c08106e8293b48",
                encodeHexString(DigestUtils.sha1("sha1".getBytes()))
        );
    }

    public void testMd5Stream() throws Exception {
        final byte[] empty = generateBytes(0);
        assertTrue("md5 digests are not equals", Arrays.equals(DigestUtils.md5(empty), DigestUtils.md5(new ByteArrayInputStream(empty))));
        final byte[] small = generateBytes(34);
        assertTrue("md5 digests are not equals", Arrays.equals(DigestUtils.md5(small), DigestUtils.md5(new ByteArrayInputStream(small))));
        final byte[] medium = generateBytes(65536);
        assertTrue("md5 digests are not equals", Arrays.equals(DigestUtils.md5(medium), DigestUtils.md5(new ByteArrayInputStream(medium))));
        final byte[] big = generateBytes(65536 + 35);
        assertTrue("md5 digests are not equals", Arrays.equals(DigestUtils.md5(big), DigestUtils.md5(new ByteArrayInputStream(big))));
    }

    private byte[] generateBytes(int length) {
        final byte[] b =  new byte[length];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte)i;
        }
        return b;
    }
}
