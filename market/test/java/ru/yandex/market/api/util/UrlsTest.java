package ru.yandex.market.api.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author dimkarp93
 */
public class UrlsTest {

    @Test
    public void encodeSafeNull() {
        String input = null;
        Assert.assertNull(Urls.encodeSafe(input));
    }

    @Test
    public void encodeSafeEmpty() {
        String input = "";
        Assert.assertNull(Urls.encodeSafe(input));
    }

    @Test
    public void encodeSafeSimple() {
        String input = "abc:def";
        Assert.assertEquals("abc%3Adef", Urls.encodeSafe(input));
    }

    @Test
    public void decodeSafeNull() {
        String input = null;
        Assert.assertNull(Urls.decodeSafe(input));
    }

    @Test
    public void decodeSafeEmpty() {
        String input = "";
        Assert.assertNull(Urls.decodeSafe(input));
    }

    @Test
    public void decodeSafeSimple() {
        String input = "abc%3Adef";
        Assert.assertEquals("abc:def", Urls.decodeSafe(input));
    }

}
