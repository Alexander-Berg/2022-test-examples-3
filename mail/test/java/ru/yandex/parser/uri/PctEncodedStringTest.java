package ru.yandex.parser.uri;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.util.BadRequestException;

public class PctEncodedStringTest {
    @Test
    public void test() throws BadRequestException {
        PctDecoder decoder = new PctDecoder(true);
        String etalon = "Аз+i%73+%D0%af%20Быды%d0%A9%d0%aF";
        PctEncodedString value = new PctEncodedString(etalon, decoder);
        Assert.assertEquals(etalon, value.toString());
        Assert.assertEquals("Аз is Я БыдыЩЯ", value.decode());
    }

    @Test
    public void testSingleChar() throws BadRequestException {
        String prefix = "abcd";
        String etalon = "%7e+";
        PctEncodedString value = new PctEncodedString(
            (prefix + etalon).toCharArray(),
            prefix.length(),
            etalon.length(),
            new PctDecoder(false));
        Assert.assertEquals(etalon, value.toString());
        Assert.assertEquals("~+", value.decode());
    }

    @Test
    public void malformed() throws BadRequestException {
        PctEncodedString value = new PctEncodedString(
            new char[]{'%'},
            new PctDecoder(true));
        Assert.assertEquals("%", value.toString());
        try {
            value.decode();
            Assert.fail();
        } catch (BadRequestException e) {
            return;
        }
    }

    @Test
    public void malformedHigh() throws BadRequestException {
        String etalon = "%za";
        PctEncodedString value = new PctEncodedString(
            etalon.toCharArray(),
            new PctDecoder(true));
        Assert.assertEquals(etalon, value.toString());
        try {
            value.decode();
            Assert.fail();
        } catch (BadRequestException e) {
            return;
        }
    }

    @Test
    public void malformedLow() throws BadRequestException {
        String etalon = "%az";
        PctEncodedString value = new PctEncodedString(
            etalon.toCharArray(),
            new PctDecoder(true));
        Assert.assertEquals(etalon, value.toString());
        try {
            value.decode();
            Assert.fail();
        } catch (BadRequestException e) {
            return;
        }
    }
}

