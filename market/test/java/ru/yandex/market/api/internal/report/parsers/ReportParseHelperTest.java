package ru.yandex.market.api.internal.report.parsers;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class ReportParseHelperTest extends UnitTestBase {

    @Test
    public void shouldProcessPoundCorrectly() {
        Assert.assertEquals(
                "http://example.com/long/path?param1=1&param2=2#!some/additional/info",
                ReportParseHelper.clearDirectUrl("http://example.com/long/path?param1=1&param2=2#!some/additional/info")
        );
    }

    @Test
    public void shouldDeleteMarketParameters() {
        Assert.assertEquals(
                "http://example.com/long/path?param3=3",
                ReportParseHelper.clearDirectUrl("http://example.com/long/path?frommarket=1&utm_campaign=2&param3=3")
        );
    }
}
