package ru.yandex.charset;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

public class DecoderTest {
    private static final byte LEADING_BYTE = (byte) 0xd0;
    private static final String REPLACEMENT = "?";

    @Test
    public void testLeadingByte() {
        Decoder decoder = new Decoder(StandardCharsets.UTF_8);
        try {
            decoder.decode(new byte[] {LEADING_BYTE});
            Assert.fail();
        } catch (CharacterCodingException e) {
            return;
        }
    }

    @Test
    public void testLeadingByteReplace() throws CharacterCodingException {
        Decoder decoder = new Decoder(StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
            .replaceWith(REPLACEMENT));
        decoder.decode(new byte[] {LEADING_BYTE});
        Assert.assertEquals(REPLACEMENT, decoder.toString());
    }
}

