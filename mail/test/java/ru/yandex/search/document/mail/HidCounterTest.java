package ru.yandex.search.document.mail;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class HidCounterTest extends TestBase {
    private static final String ONE = "1";
    private static final String ONE_TWO = "1.2";

    @Test
    public void testHid() {
        HidCounter counter = new HidCounter();
        Assert.assertEquals(ONE, counter.hid());
        counter.createPart();
        Assert.assertEquals("1.1", counter.hid());
        counter.popPart();
        Assert.assertEquals(ONE, counter.hid());
        counter.createPart();
        Assert.assertEquals(ONE_TWO, counter.hid());
        counter.createPart();
        Assert.assertEquals("1.2.1", counter.hid());
        counter.popPart();
        Assert.assertEquals(ONE_TWO, counter.hid());
        counter.createPart();
        Assert.assertEquals("1.2.2", counter.hid());
        counter.popPart();
        Assert.assertEquals(ONE_TWO, counter.hid());
        counter.popPart();
        Assert.assertEquals(ONE, counter.hid());
        counter.createPart();
        Assert.assertEquals("1.3", counter.hid());
    }
}

