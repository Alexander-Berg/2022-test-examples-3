package ru.yandex.market.checkout.checkouter.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.ping.CheckResult;

public class CheckResultParserTest {

    @Test
    public void shouldParse0OK() {
        CheckResult parse = CheckResultParser.parse("0;OK");
        Assertions.assertEquals(CheckResult.Level.OK, parse.getLevel());
        Assertions.assertEquals("OK", parse.getMessage());
    }

    @Test
    public void shouldParse1withText() {
        CheckResult parse = CheckResultParser.parse("1;Some message");
        Assertions.assertEquals(CheckResult.Level.WARNING, parse.getLevel());
        Assertions.assertEquals("Some message", parse.getMessage());
    }

    @Test
    public void shouldParse2withError() {
        CheckResult parse = CheckResultParser.parse("2;Some error");
        Assertions.assertEquals(CheckResult.Level.CRITICAL, parse.getLevel());
        Assertions.assertEquals("Some error", parse.getMessage());
    }
}
