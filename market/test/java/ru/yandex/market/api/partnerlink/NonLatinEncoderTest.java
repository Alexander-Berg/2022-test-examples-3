package ru.yandex.market.api.partnerlink;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.util.NonLatinEncoder;

public class NonLatinEncoderTest {

    @Test
    public void testEncode() {
        Assert.assertEquals(
                "%D0%B0%D0%BA%D0%BA%D1%83%D0%BC%D1%83%D0%BB%D1%8F%D1%82%D0%BE%D1%80%D1%8B%20makita%20%D0%BA%D1%83%D0%BF%D0%B8%D1%82%D1%8C",
                NonLatinEncoder.encodeNonLatinSubstrings("аккумуляторы%20makita%20купить"));
        Assert.assertEquals("iphone%20apple", NonLatinEncoder.encodeNonLatinSubstrings("iphone%20apple"));
        Assert.assertEquals("", NonLatinEncoder.encodeNonLatinSubstrings(""));
        Assert.assertEquals("%C3%BA", NonLatinEncoder.encodeNonLatinSubstrings("ú"));
    }

}