package ru.yandex.url.processor;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class CommonZoneExtractorTest extends TestBase {
    @Test
    public void testDomains() {
        Assert.assertEquals("yandex.ru", CommonZoneExtractor.INSTANCE.apply("front1l.mail.yandex.ru"));
        Assert.assertEquals("bkx.tk", CommonZoneExtractor.INSTANCE.apply("bkx.tk"));
        Assert.assertEquals("localhost", CommonZoneExtractor.INSTANCE.apply("localhost"));
        Assert.assertEquals("ru.ru", CommonZoneExtractor.INSTANCE.apply("a.b.ru.ru"));
        Assert.assertEquals("u.nu", CommonZoneExtractor.INSTANCE.apply("n.u.nu."));
        Assert.assertEquals("site.com.ua", CommonZoneExtractor.INSTANCE.apply("site.com.ua"));
        Assert.assertEquals("р.рф", CommonZoneExtractor.INSTANCE.apply("пр.име.р.рф"));
        Assert.assertEquals("147.23.231.32", CommonZoneExtractor.INSTANCE.apply("147.23.231.32"));
        Assert.assertEquals("fe80::::200:f8ff:fe21:67cf", CommonZoneExtractor.INSTANCE.apply("fe80::::200:f8ff:fe21:67cf"));
    }
}
