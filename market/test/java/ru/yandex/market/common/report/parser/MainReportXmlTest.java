package ru.yandex.market.common.report.parser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.LocalDeliveryOption;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author kukabara
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-bean.xml")
public class MainReportXmlTest {
    private static final String MAIN_REPORT_PATH = "/files/mainreport.xml";
    public static final double DELTA = 0.0001;

    @Autowired
    private MainReportXmlParserFactory testReportXmlParserFactory;
    private MainReportXmlParser parser;

    @Before
    public void setUp() throws Exception {
        parser = testReportXmlParserFactory.newParser();
    }

    @Test
    public void testParse() throws Exception {
        parser.parse(MainReportXmlTest.class.getResourceAsStream(MAIN_REPORT_PATH));
        List<FoundOffer> offers = parser.getOffers();
        assertEquals(offers.size(), 1);
        FoundOffer offer = offers.get(0);
        assertEquals("i7p1BqwerlsFjz", offer.getPromoMd5());
        assertTrue(offer.isPromotedByVendor());
        assertNotNull(offer.getQuantityLimits());
        assertEquals(3, offer.getQuantityLimits().getMinimum());
        assertEquals(4, offer.getQuantityLimits().getStep());

        assertFalse(offer.getLocalDelivery().isEmpty());
        LocalDeliveryOption option = offer.getLocalDelivery().iterator().next();
        assertEquals(300.14d, option.getPrice().doubleValue(), DELTA);
    }

}
