package ru.yandex.parser.uri;

import java.nio.charset.CharacterCodingException;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.function.StringVoidProcessor;

public class PctEncoderTest {
    @Test
    public void testPath() throws CharacterCodingException {
        StringVoidProcessor<char[], CharacterCodingException> encoder =
            new StringVoidProcessor<>(new PctEncoder(PctEncodingRule.PATH));
        encoder.process("hello/?#=& world");
        Assert.assertEquals("hello%2F%3F%23=&%20world", encoder.toString());
    }

    @Test
    public void testQuery() throws CharacterCodingException {
        StringVoidProcessor<char[], CharacterCodingException> encoder =
            new StringVoidProcessor<>(new PctEncoder(PctEncodingRule.QUERY));
        encoder.process("hello/?#&= world");
        Assert.assertEquals("hello/?%23%26%3D+world", encoder.toString());
        encoder.process("he+ll");
        Assert.assertEquals("he%2Bll", encoder.toString());
        encoder.process("Привет,_ми! р");
        Assert.assertEquals(
            "%D0%9F%D1%80%D0%B8%D0%B2%D0%B5%D1%82,_%D0%BC%D0%B8!+%D1%80",
            encoder.toString());
    }

    @Test
    public void testStringBuilder() throws CharacterCodingException {
        StringVoidProcessor<char[], CharacterCodingException> encoder =
            new StringVoidProcessor<>(new PctEncoder(PctEncodingRule.FRAGMENT));
        encoder.process("how are you#");
        Assert.assertEquals("how%20are%20you%23", encoder.toString());
    }

    @Test
    public void testWriter() throws CharacterCodingException {
        StringVoidProcessor<char[], CharacterCodingException> encoder =
            new StringVoidProcessor<>(new PctEncoder(PctEncodingRule.FRAGMENT));
        encoder.process("i+m fine");
        Assert.assertEquals("i+m%20fine", encoder.toString());
    }
}

