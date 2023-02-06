package ru.yandex.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

// XXX: This is just an utility class test, all its functionality must be
// covered by complete use cases
public class CountingInputStreamTest extends TestBase {
    public static final Charset UTF8 = Charset.forName("utf-8");
    public static final byte[] DATA = "Hello, world".getBytes(UTF8);

    @Test
    public void test() throws IOException {
        CountingInputStream is =
            new CountingInputStream(new ByteArrayInputStream(DATA));
        Assert.assertEquals('H', (char) is.read());
        Assert.assertEquals(1, is.pos());
        final int skip = 6;
        Assert.assertEquals(skip, is.skip(skip));
        Assert.assertEquals(skip + 1, is.pos());
        Assert.assertEquals('w', is.read());
        Assert.assertEquals(DATA.length - (skip + 1 + 1), is.skip(skip));
        Assert.assertEquals(DATA.length, is.pos());
        Assert.assertEquals(-1, is.read());
        Assert.assertEquals(DATA.length, is.pos());
        Assert.assertEquals(0, is.skip(skip));
        Assert.assertEquals(DATA.length, is.pos());
    }
}
