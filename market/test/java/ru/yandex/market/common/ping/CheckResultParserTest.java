package ru.yandex.market.common.ping;

import org.junit.Assert;
import org.junit.Test;

public class CheckResultParserTest {
    @Test
    public void shouldParse0OK() {
        CheckResult parse = CheckResultParser.parse("0;OK");
        Assert.assertEquals(CheckResult.Level.OK, parse.getLevel());
        Assert.assertEquals("OK", parse.getMessage());
    }

    @Test
    public void shouldParse1withText() {
        CheckResult parse = CheckResultParser.parse("1;Some message");
        Assert.assertEquals(CheckResult.Level.WARNING, parse.getLevel());
        Assert.assertEquals("Some message", parse.getMessage());
    }

    @Test
    public void shouldParse2withError() {
        CheckResult parse = CheckResultParser.parse("2;Some error");
        Assert.assertEquals(CheckResult.Level.CRITICAL, parse.getLevel());
        Assert.assertEquals("Some error", parse.getMessage());
    }
}
