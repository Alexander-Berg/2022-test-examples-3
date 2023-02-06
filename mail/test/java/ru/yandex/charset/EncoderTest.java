package ru.yandex.charset;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.function.OutputStreamProcessorAdapter;

public class EncoderTest {
    private static final char[] HIGH_SURROGATE = new char[]{'\ud835'};

    @Test
    public void testHighSurrogate() {
        Encoder encoder = new Encoder(StandardCharsets.UTF_8);
        try {
            encoder.process(HIGH_SURROGATE);
            Assert.fail();
        } catch (CharacterCodingException e) {
            return;
        }
    }

    @Test
    public void testHighSurrogateReplace() throws IOException {
        Encoder encoder = new Encoder(StandardCharsets.UTF_8.newEncoder()
            .onMalformedInput(CodingErrorAction.REPLACE));
        encoder.process(HIGH_SURROGATE);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encoder.processWith(new OutputStreamProcessorAdapter(baos));
        Assert.assertArrayEquals(new byte[]{(byte) '?'}, baos.toByteArray());
    }
}

