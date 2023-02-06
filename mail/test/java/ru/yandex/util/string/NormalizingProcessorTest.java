package ru.yandex.util.string;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.function.CharArrayVoidProcessor;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class NormalizingProcessorTest extends TestBase {
    private static final int MAX_NORMALIZATION_TIME = 2000;
    private static final int READ_BLOCK_SIZE = 8192;

    public NormalizingProcessorTest() {
        super(false, 0L);
    }

    @Test
    public void testShare() {
        NormalizingProcessor normalizer = new NormalizingProcessor(true);
        normalizer.process("и\u0306".toCharArray());
        Assert.assertEquals("й", normalizer.toString());

        normalizer.process("שּׁשּׂ".toCharArray());
        Assert.assertEquals(
            "\u05e9\u05bc\u05c1\u05e9\u05bc\u05c2",
            normalizer.toString());

        final char[] testBuf = "прийвет мир".toCharArray();
        normalizer.process(testBuf);
        normalizer.processWith(
            new CharArrayVoidProcessor<RuntimeException>() {
                @Override
                public void process(
                    final char[] buf,
                    final int off,
                    final int len)
                {
                    Assert.assertSame(buf, testBuf);
                    Assert.assertEquals(0, off);
                    Assert.assertEquals(testBuf.length, len);
                }
            });
    }

    @Test
    public void testNoShare() {
        NormalizingProcessor normalizer = new NormalizingProcessor();
        final char[] testBuf = "copy this".toCharArray();
        normalizer.process(testBuf);
        normalizer.processWith(
            new CharArrayVoidProcessor<RuntimeException>() {
                @Override
                public void process(
                    final char[] buf,
                    final int off,
                    final int len)
                {
                    Assert.assertFalse(buf == testBuf);
                    Assert.assertEquals(0, off);
                    Assert.assertEquals(testBuf.length, len);
                    Assert.assertArrayEquals(testBuf, buf);
                }
            });
    }

    private String assertFastNormalization(final char[] buf) {
        NormalizingProcessor normalizer = new NormalizingProcessor(true);
        long start = System.currentTimeMillis();
        normalizer.process(buf);
        long end = System.currentTimeMillis();
        YandexAssert.assertLess(start + MAX_NORMALIZATION_TIME, end);
        return normalizer.toString();
    }

    @Test
    public void testSlowNormalization() throws IOException {
        try (FileInputStream in =
                new FileInputStream(
                    Paths.getSandboxResourcesRoot() + "/ultra-slow-nfc");
            Reader reader =
                new InputStreamReader(in, StandardCharsets.UTF_8))
        {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[READ_BLOCK_SIZE];
            while (true) {
                int read = reader.read(buf);
                if (read == -1) {
                    break;
                }
                sb.append(buf, 0, read);
            }
            assertFastNormalization(new String(sb).toCharArray());
        }
    }

    @Test
    public void testHebrewNormalization() {
        char[] buf = new char[READ_BLOCK_SIZE << 2];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buf.length; ++i) {
            buf[i] = 'שּׁ';
            sb.append("\u05e9\u05bc\u05c1");
        }
        Assert.assertEquals(new String(sb), assertFastNormalization(buf));
    }

    @Test
    public void testBadInput() {
        char[] buf = new char[READ_BLOCK_SIZE << 2];
        buf[0] = 'и';
        for (int i = 1; i < buf.length; ++i) {
            buf[i] = '\u0306';
        }
        StringBuilder sb = new StringBuilder();
        sb.append('й');
        for (int i = 2; i < buf.length; ++i) {
            sb.append('\u0306');
        }
        Assert.assertEquals(new String(sb), assertFastNormalization(buf));
    }

    @Test
    public void testBoundary() {
        StringBuilder sb = new StringBuilder();
        for (int i = NormalizingProcessor.NORMALIZATION_BLOCK_SIZE; i-- > 0;) {
            sb.append(' ');
        }
        sb.append('и');
        sb.append('\u0306');
        for (int i = NormalizingProcessor.NORMALIZATION_BLOCK_SIZE; i-- > 2;) {
            sb.append(' ');
        }
        sb.append('\ud801');
        sb.append('\udc37');
        for (int i = NormalizingProcessor.NORMALIZATION_BLOCK_SIZE; i-- > 0;) {
            sb.append(' ');
        }

        char[] buf = new String(sb).toCharArray();
        sb.setLength(0);
        for (int i = NormalizingProcessor.NORMALIZATION_BLOCK_SIZE; i-- > 0;) {
            sb.append(' ');
        }
        sb.append('й');
        for (int i = NormalizingProcessor.NORMALIZATION_BLOCK_SIZE; i-- > 2;) {
            sb.append(' ');
        }
        sb.append('\ud801');
        sb.append('\udc37');
        for (int i = NormalizingProcessor.NORMALIZATION_BLOCK_SIZE; i-- > 0;) {
            sb.append(' ');
        }
        Assert.assertEquals(new String(sb), assertFastNormalization(buf));

        sb.setLength(0);
        for (int i = NormalizingProcessor.NORMALIZATION_BLOCK_SIZE; i-- > 0;) {
            sb.append('и');
        }
        for (int i = NormalizingProcessor.NORMALIZATION_BLOCK_SIZE; i-- > 0;) {
            sb.append('\u0306');
        }

        StringBuilder normalized = new StringBuilder();
        for (int i = NormalizingProcessor.NORMALIZATION_BLOCK_SIZE; i-- > 1;) {
            normalized.append('и');
        }
        normalized.append('й');
        for (int i = NormalizingProcessor.NORMALIZATION_BLOCK_SIZE; i-- > 1;) {
            normalized.append('\u0306');
        }
        Assert.assertEquals(
            new String(normalized),
            assertFastNormalization(new String(sb).toCharArray()));
    }

    @Test
    public void testSlowCheck() throws IOException {
        try (Reader reader = new InputStreamReader(
                getClass().getResourceAsStream("ultra-slow-check"),
                StandardCharsets.UTF_8))
        {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[READ_BLOCK_SIZE];
            while (true) {
                int read = reader.read(buf);
                if (read == -1) {
                    break;
                }
                sb.append(buf, 0, read);
            }
            assertFastNormalization(new String(sb).toCharArray());
        }
    }
}

