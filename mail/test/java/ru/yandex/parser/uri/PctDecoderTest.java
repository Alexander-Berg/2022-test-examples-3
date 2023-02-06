package ru.yandex.parser.uri;

import java.nio.charset.CharacterCodingException;

import org.junit.Assert;
import org.junit.Test;

public class PctDecoderTest {
    @Test
    public void test() throws CharacterCodingException {
        PctDecoder decoder = new PctDecoder(false);
        Assert.assertEquals("Hello+world", decoder.decode("Hello+" + "world"));
        Assert.assertEquals("Hell", decoder.decode("He" + "ll"));
        Assert.assertEquals("me@ya.ru", decoder.decode("me%40ya.ru"));
        Assert.assertEquals("m@ya.ru", decoder.decode("m%40ya.ru"));
    }

    @Test
    public void testPlus() throws CharacterCodingException {
        PctDecoder decoder = new PctDecoder(true);
        Assert.assertEquals("H ll", decoder.decode("H+ll".toCharArray()));
        Assert.assertEquals("H@ll", decoder.decode("H%40ll"));
        Assert.assertEquals("Hll", decoder.decode("Hl" + 'l'));
        Assert.assertEquals("Hl l", decoder.decode('H' + "l+l"));
        Assert.assertEquals("W@nderf l", decoder.decode("W%40nderf+l"));
        Assert.assertEquals("W r@e", decoder.decode("W+r%40e"));
        Assert.assertEquals("W@r e", decoder.decode("W%40r+e".toCharArray()));
    }
}

