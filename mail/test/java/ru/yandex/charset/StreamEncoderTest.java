package ru.yandex.charset;

import java.io.ByteArrayOutputStream;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class StreamEncoderTest extends TestBase {
    public StreamEncoderTest() {
        super(false, 0L);
    }

    private void test(
        final byte[] expected,
        final boolean shortcutSingleChars,
        final boolean flush,
        final char[]... cbufs)
        throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (StreamEncoder encoder =
            new StreamEncoder(
                baos,
                StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT),
                8))
        {
            for (char[] cbuf: cbufs) {
                if (cbuf.length == 1 && shortcutSingleChars) {
                    encoder.write(cbuf[0]);
                } else {
                    encoder.write(cbuf);
                }
                if (flush) {
                    encoder.flush();
                }
            }
        }
        Assert.assertArrayEquals(expected, baos.toByteArray());
    }

    private void test(final byte[] expected, final char[]... cbufs)
        throws Exception
    {
        test(expected, false, false, cbufs);
        test(expected, false, true, cbufs);
        test(expected, true, false, cbufs);
        test(expected, true, true, cbufs);
    }

    private void simpleTest(final String text) throws Exception {
        test(text.getBytes("UTF-8"), text.toCharArray());
    }

    @Test
    public void test() throws Exception {
        simpleTest("Hello, world!");
        simpleTest("Привет, мир!");
        simpleTest("Привет, !\ud800\uddd0");
    }

    @Test
    public void testSplit() throws Exception {
        test(
            "Привет, !\ud800\uddd0".getBytes("UTF-8"),
            "Привет, !\ud800".toCharArray(),
            new char[]{'\uddd0'});
        test(
            "\ud800\uddd0, мир!".getBytes("UTF-8"),
            new char[]{'\ud800'},
            "\uddd0, мир!".toCharArray());
        test(
            "Привет, !\ud800\uddd0some text here".getBytes("UTF-8"),
            "Привет, !\ud800".toCharArray(),
            "\uddd0some text here".toCharArray());
        test(
            "Hello, world!".getBytes("UTF-8"),
            "Hello".toCharArray(),
            ", ".toCharArray(),
            "world!".toCharArray());
    }

    @Test
    public void stressTest() throws Exception {
        String str = "Hello, мирок! \ud800\uddd0 how ты \ufb3e тута?";
        byte[] buf = str.getBytes("UTF-8");
        char[] cbuf = str.toCharArray();
        char[][] cbufs = new char[5][];
        for (int a = 1; a < cbuf.length - 3; ++a) {
            cbufs[0] = Arrays.copyOfRange(cbuf, 0, a);
            for (int b = a + 1; b < cbuf.length - 2; ++b) {
                cbufs[1] = Arrays.copyOfRange(cbuf, a, b);
                for (int c = b + 1; c < cbuf.length - 1; ++c) {
                    cbufs[2] = Arrays.copyOfRange(cbuf, b, c);
                    for (int d = c + 1; d < cbuf.length; ++d) {
                        cbufs[3] = Arrays.copyOfRange(cbuf, c, d);
                        cbufs[4] = Arrays.copyOfRange(cbuf, d, cbuf.length);
                        test(buf, cbufs);
                    }
                }
            }
        }
    }

    @Test
    public void testErrors() throws Exception {
        try {
            test(new byte[0], false, false, new char[]{'\uddd0'});
            Assert.fail();
        } catch (Exception e) {
        }
        try {
            test(new byte[0], true, false, new char[]{'\uddd0'});
            Assert.fail();
        } catch (Exception e) {
        }
        try {
            test(new byte[0], false, false, new char[]{'\ud800'});
            Assert.fail();
        } catch (Exception e) {
        }
        try {
            test(new byte[0], true, false, new char[]{'\ud800'});
            Assert.fail();
        } catch (Exception e) {
        }
        try {
            test(new byte[0], false, false, new char[]{'\ud800', '\ud800'});
            Assert.fail();
        } catch (Exception e) {
        }
    }
}

