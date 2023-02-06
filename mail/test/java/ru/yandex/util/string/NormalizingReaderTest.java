package ru.yandex.util.string;

import java.io.StringReader;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.io.ChainedReader;
import ru.yandex.test.util.TestBase;

public class NormalizingReaderTest extends TestBase {
    private static final String I_SHORT = "й";

    public NormalizingReaderTest() {
        super(false, 0L);
    }

    @Test
    public void test() throws Exception {
        Assert.assertEquals(
            I_SHORT,
            StringUtils.from(
                new NormalizingReader(new StringReader("и\u0306"))));
        Assert.assertEquals(
            "скайп",
            StringUtils.from(
                new NormalizingReader(
                    new ChainedReader(
                        new StringReader("скаи"),
                        new StringReader("\u0306п")))));
    }

    @Test
    public void testLong() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = NormalizingProcessor.NORMALIZATION_BLOCK_SIZE; i-- > 0;) {
            sb.append('i');
        }
        for (int i = NormalizingProcessor.MAX_LOOKAHEAD; i-- > 1;) {
            sb.append('i');
        }
        StringBuilder normalized = new StringBuilder();
        normalized.append(sb);
        sb.append('и');
        sb.append('\u0306');
        normalized.append('й');
        Assert.assertEquals(
            new String(normalized),
            StringUtils.from(
                new NormalizingReader(new StringReader(new String(sb)))));

        sb.setLength(0);
        for (int i = NormalizingProcessor.NORMALIZATION_BLOCK_SIZE; i-- > 0;) {
            sb.append('и');
        }
        for (int i = NormalizingProcessor.NORMALIZATION_BLOCK_SIZE; i-- > 0;) {
            sb.append('\u0306');
        }
        normalized.setLength(0);
        for (int i = NormalizingProcessor.NORMALIZATION_BLOCK_SIZE; i-- > 1;) {
            normalized.append('и');
        }
        normalized.append('й');
        for (int i = NormalizingProcessor.NORMALIZATION_BLOCK_SIZE; i-- > 1;) {
            normalized.append('\u0306');
        }

        Assert.assertEquals(
            new String(normalized),
            StringUtils.from(
                new NormalizingReader(new StringReader(new String(sb)))));
    }

    @Test
    public void testState() throws Exception {
        StringBuilder sb = new StringBuilder();
        NormalizingReader reader =
            new NormalizingReader(new StringReader("йи\u0306"));
        sb.append((char) reader.read());
        sb.append((char) reader.read());
        Assert.assertEquals("йй", new String(sb));
    }
}

