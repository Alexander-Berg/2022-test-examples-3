package ru.yandex.market.api.internal.report.parsers.json;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.option.AvailableReportSort;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.report.SortOrder;
import ru.yandex.market.api.internal.report.ReportSortType;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.*;

/**
 *
 * Created by apershukov on 14.09.16.
 */
public class ReportSortJsonParserTest extends UnitTestBase {

    private ReportSortJsonParser parser;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        parser = new ReportSortJsonParser();
    }

    @Test
    public void testParseReportSort() {
        AvailableReportSort sort = parser.parse(ResourceHelpers.getResource("report-sort1.json"));

        assertEquals("по цене", sort.getText());
        assertEquals(ReportSortType.PRICE, sort.getField());

        assertEquals(2, sort.getOptions().size());

        assertEquals("aprice", sort.getOptions().get(0).getId());
        assertEquals(SortOrder.ASC, sort.getOptions().get(0).getHow());

        assertEquals("dprice", sort.getOptions().get(1).getId());
        assertEquals(SortOrder.DESC, sort.getOptions().get(1).getHow());
    }

    @Test
    public void testParseReportContainingOptionWithoutType() {
        AvailableReportSort sort = parser.parse(ResourceHelpers.getResource("report-sort2.json"));

        assertEquals("по рейтингу", sort.getText());
        assertEquals(ReportSortType.QUALITY, sort.getField());

        assertEquals(1, sort.getOptions().size());

        assertEquals("quality", sort.getOptions().get(0).getId());
        assertEquals(SortOrder.DESC, sort.getOptions().get(0).getHow());
    }

    @Test
    public void testParseSortWithoutOptions() {
        AvailableReportSort sort = parser.parse(ResourceHelpers.getResource("report-sort3.json"));

        assertEquals("по популярности", sort.getText());
    }

    @Test
    public void testParseSortWithInvalidType() {
        AvailableReportSort sort = parser.parse(ResourceHelpers.getResource("report-sort4.json"));

        assertEquals("по цене", sort.getText());
        assertEquals(ReportSortType.PRICE, sort.getField());

        assertEquals(2, sort.getOptions().size());

        assertEquals("aprice", sort.getOptions().get(0).getId());
        assertEquals(SortOrder.ASC, sort.getOptions().get(0).getHow());

        assertEquals("dprice", sort.getOptions().get(1).getId());
        assertEquals(SortOrder.DESC, sort.getOptions().get(1).getHow());
    }

    @Test
    public void testParseSortWithOneUnknownId() {
        AvailableReportSort sort = parser.parse(ResourceHelpers.getResource("report-sort5.json"));

        assertEquals("по цене", sort.getText());
        assertEquals(ReportSortType.PRICE, sort.getField());

        assertEquals(1, sort.getOptions().size());

        assertEquals("dprice", sort.getOptions().get(0).getId());
        assertEquals(SortOrder.DESC, sort.getOptions().get(0).getHow());
    }

    @Test
    public void testParseUnknownSort() {
        AvailableReportSort sort = parser.parse(ResourceHelpers.getResource("report-sort6.json"));
        assertNull(sort);
    }
}
