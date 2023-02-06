package ru.yandex.market.common.report.parser;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.common.report.model.ModelCountersInfo;
import ru.yandex.market.common.report.parser.xml.BulkModelOfferCountsReportXmlParser;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author vbudnev
 */
public class BulkModelOfferCountsReportXmlParserTest {
    private static final String FILE = "/files/bulk_model_offer_counts.xml";

    private BulkModelOfferCountsReportXmlParser parser;

    @Before
    public void setUp() throws Exception {
        parser = new BulkModelOfferCountsReportXmlParser();
    }


    @Test
    public void test_should_parse_when_xmlIsOk() throws Exception {
        parser.parse(MainReportXmlTest.class.getResourceAsStream(FILE));
        Map<Long, ModelCountersInfo> offerCounts = parser.getResult();

        assertEquals(2, offerCounts.size());
        assertEquals(1000_0000, offerCounts.get(123L).getTotal());
        assertEquals(2000_0000, offerCounts.get(456L).getTotal());
    }
}
