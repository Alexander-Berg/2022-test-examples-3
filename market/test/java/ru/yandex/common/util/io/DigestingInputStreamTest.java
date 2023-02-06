package ru.yandex.common.util.io;

import junit.framework.TestCase;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestingInputStreamTest extends TestCase {

    public void testWithEmtpyStream() {
        final byte[] probe = {};
        final String expected = "d41d8cd98f00b204e9800998ecf8427e";
        assertEquals(expected, processOneByOne(new ByteArrayInputStream(probe)));
    }

    public void testCornerCaseWhenStreamHasMinusOne() {
        final byte[] probe = {(byte) 0xFF};
        final String expected = "00594fd4f42ba43fc1ca0427a0576295";
        assertEquals(expected, processOneByOne(new ByteArrayInputStream(probe)));
    }

    public void testWithPrecoumputedStream() {
        try {
            final byte[] probe = "Hello, World!".getBytes("ascii");
            final String expected = "65a8e27d8879283831b664bd8b7f0ad4";
            assertEquals(expected, processOneByOne(new ByteArrayInputStream(probe)));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void testWithBufferedApi() {
        try {
            final byte[] probe = "Hello, World!".getBytes("ascii");
            final String expected = "65a8e27d8879283831b664bd8b7f0ad4";
            assertEquals(expected, processUsingBuffer(new BufferedInputStream(new ByteArrayInputStream(probe))));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private String processOneByOne(final InputStream in) {
        try {
            final DigestingInputStream withMd5;
            try {
                withMd5 = new DigestingInputStream(in, MessageDigest.getInstance("MD5"));
            } catch (NoSuchAlgorithmException e) {
                throw new AssertionError("MD5 not supported");
            }

            byte b;
            while ((b = (byte) withMd5.read()) != -1) {
                //
            }
            in.close();
            return withMd5.getDigestAsHex();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected error during execution of test", e);
        }
    }

    private String processUsingBuffer(final InputStream in) {
        try {
            final DigestingInputStream withMd5;
            try {
                withMd5 = new DigestingInputStream(in, MessageDigest.getInstance("MD5"));
            } catch (NoSuchAlgorithmException e) {
                throw new AssertionError("MD5 not supported");
            }

            final byte b[] = new byte[1024*64];
            int res;
            while ((res = withMd5.read(b)) != -1) {

            }
            in.close();
            return withMd5.getDigestAsHex();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected error during execution of test", e);
        }
    }

}
