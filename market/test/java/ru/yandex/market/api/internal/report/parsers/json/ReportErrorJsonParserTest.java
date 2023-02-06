package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.report.ReportError;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.*;

/**
 *
 * Created by apershukov on 23.12.16.
 */
public class ReportErrorJsonParserTest extends UnitTestBase {

    private ReportErrorJsonParser parser;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        parser = new ReportErrorJsonParser();
    }


    @Test
    public void testParseError() throws Exception {
        ReportError error = parser.parse(ResourceHelpers.getResource("report-error.json"));
        assertEquals("EMPTY_REQUEST", error.code());
    }

    @Test
    public void testParseXmlError() throws Exception {
        ReportError error = parser.parse(ResourceHelpers.getResource("report-error.xml"));
        assertNull(error);
    }

    @Test
    public void testParseRegularJson() throws Exception {
        ReportError error = parser.parse(ResourceHelpers.getResource("offer.json"));
        assertNull(error);
    }
}
