package ru.yandex.base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.io.ChainedInputStream;
import ru.yandex.test.util.TestBase;

public class Base64InputStreamTest extends TestBase {
    public Base64InputStreamTest() {
        super(false, 0L);
    }

    private static String toString(final InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int b = is.read(); b != -1; b = is.read()) {
            sb.append((char) b);
        }
        return sb.toString();
    }

    @Test
    public void test() throws IOException {
        Base64InputStream in = new Base64InputStream(new ByteArrayInputStream(
            "TWFu".getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals((int) 'M', in.read());
        Assert.assertEquals((int) 'a', in.read());
        Assert.assertEquals((int) 'n', in.read());
        Assert.assertEquals(-1, in.read());
        Assert.assertEquals("   @@>ng?", toString(
            new Base64InputStream(new ByteArrayInputStream(
                "ICAgQEA+bmc/".getBytes(StandardCharsets.UTF_8)))));
    }

    @Test
    public void testPadding() throws IOException {
        Assert.assertEquals("any carnal pleasure.", toString(
            new Base64InputStream(new ByteArrayInputStream(
                "YW55IGNhcm5hbCBwbGVhc3VyZS4="
                    .getBytes(StandardCharsets.UTF_8)))));
        Assert.assertEquals("any carnal pleasure", toString(
            new Base64InputStream(new ByteArrayInputStream(
                "YW55IGNhcm5hbCBwbGVhc3VyZQ=="
                    .getBytes(StandardCharsets.UTF_8)))));
        Assert.assertEquals("any carnal pleasur", toString(
            new Base64InputStream(new ByteArrayInputStream(
                "YW55IGNhcm5hbCBwbGVhc3Vy"
                    .getBytes(StandardCharsets.UTF_8)))));
    }

    @Test
    public void testPadless() throws IOException {
        Assert.assertEquals('a' + "ny carnal pleasure.", toString(
            new Base64InputStream(new ByteArrayInputStream(
                "YW55IGNhcm5hbCBwbGVhc3VyZS4"
                    .getBytes(StandardCharsets.UTF_8)))));
        Assert.assertEquals('a' + "ny carnal pleasure", toString(
            new Base64InputStream(new ByteArrayInputStream(
                "YW55IGNhcm5hbCBwbGVhc3VyZQ"
                    .getBytes(StandardCharsets.UTF_8)))));
    }

    @Test
    public void testBoundaryInput() throws IOException {
        Assert.assertEquals(
            "any carna",
            toString(
                new Base64InputStream(
                    new ChainedInputStream(
                        new ByteArrayInputStream(
                            "YW55I".getBytes(StandardCharsets.UTF_8)),
                        new ByteArrayInputStream(
                            "GNhcm5h".getBytes(StandardCharsets.UTF_8))))));
        Assert.assertEquals(
            "any carna",
            toString(
                new Base64InputStream(
                    new ChainedInputStream(
                        new ByteArrayInputStream(
                            "YW55IG".getBytes(StandardCharsets.UTF_8)),
                        new ByteArrayInputStream(
                            "Nhcm5h".getBytes(StandardCharsets.UTF_8))))));
        Assert.assertEquals(
            "any carna",
            toString(
                new Base64InputStream(
                    new ChainedInputStream(
                        new ByteArrayInputStream(
                            "YW55IGN".getBytes(StandardCharsets.UTF_8)),
                        new ByteArrayInputStream(
                            "hcm5h".getBytes(StandardCharsets.UTF_8))))));
        Assert.assertEquals(
            "any carna",
            toString(
                new Base64InputStream(
                    new ChainedInputStream(
                        new ByteArrayInputStream(
                            "YW55IGNh".getBytes(StandardCharsets.UTF_8)),
                        new ByteArrayInputStream(
                            "cm5h".getBytes(StandardCharsets.UTF_8))))));
    }

    @Test
    public void testOnlyGarbageChunk() throws IOException {
        Assert.assertEquals(
            "any carna",
            toString(
                new Base64InputStream(
                    new ChainedInputStream(
                        new ByteArrayInputStream(
                            "\r\n".getBytes(StandardCharsets.UTF_8)),
                        new ByteArrayInputStream(
                            "YW55IGN".getBytes(StandardCharsets.UTF_8)),
                        new ByteArrayInputStream(
                            "hcm5h".getBytes(StandardCharsets.UTF_8))))));
    }
}

