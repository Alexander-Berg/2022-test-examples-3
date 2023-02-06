package ru.yandex.market.common.report;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author zoom
 */
public class AbstractMarketReportServiceTest extends Assert {

    @Test
    public void shouldNotAddNullBooleanParams() {
        StringBuilder sb = new StringBuilder();
        AbstractMarketReportService.appendUrlBoolParam(sb, "name", null);
        assertEquals("", sb.toString());
    }

    @Test
    public void shouldAddTrueBoolParamAsOne() {
        StringBuilder sb = new StringBuilder();
        AbstractMarketReportService.appendUrlBoolParam(sb, "name", true);
        assertEquals("&name=1", sb.toString());
    }

    @Test
    public void shouldAddFalseBoolParamAsZero() {
        StringBuilder sb = new StringBuilder();
        AbstractMarketReportService.appendUrlBoolParam(sb, "name", false);
        assertEquals("&name=0", sb.toString());
    }
}