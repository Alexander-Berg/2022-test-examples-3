package ru.yandex.market.common.report.indexer;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.common.report.indexer.model.DisabledReason;
import ru.yandex.market.common.report.indexer.model.OfferDetails;
import ru.yandex.market.common.report.indexer.model.PriceSource;
import ru.yandex.market.common.report.indexer.model.RegionalDeliveryOption;
import ru.yandex.market.common.report.indexer.model.RegionalDeliveryOptions;
import ru.yandex.market.common.report.indexer.model.checksupplier.CheckSupplierResult;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author artemmz
 * @date 19.04.18.
 */
public class IdxApiRespParserTest {
    private static final double DELTA = 0.0001;
    private static final Map<Long, Date> FEED_REFRESHES = new HashMap<Long, Date>() {{
        put(498470L, new Date(1534870797610154L / 1000));
        put(485107L, new Date(1535031056818482L / 1000));
        put(465417L, new Date(1535043760175497L / 1000));
        put(525844L, new Date(1534967731269491L / 1000));
    }};

    private String content;
    private String deliveryOptionsContent;

    @Before
    public void setUp() throws Exception {
        content = read("/files/feed/feed-dispatcher.xml");
        deliveryOptionsContent = read("/files/feed/feed-dispatcher-options.json");
    }

    private String read(String fileName) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(fileName));
    }

    @Test
    public void testDeliveryOptions() throws IOException {
        RegionalDeliveryOptions options = IdxApiRespParser.parseDeliveryOptions(deliveryOptionsContent);
        assertNotNull(options);
        assertNotNull(options.getRegionalDeliveryOptions());
        assertEquals(2, options.getRegionalDeliveryOptions().size());

        RegionalDeliveryOption option = options.getRegionalDeliveryOptions().get(0);
        assertTrue(option.isAllowed());
        assertEquals((Integer) 4, option.getDayFrom());
        assertEquals((Integer) 4, option.getDayTo());
        assertEquals((Integer) 24, option.getOrderBefore());
        assertEquals(BigDecimal.valueOf(400.14), option.getPrice());
        assertEquals(Currency.RUR, option.getCurrency());

        RegionalDeliveryOption option2 = options.getRegionalDeliveryOptions().get(1);
        assertFalse(option2.isAllowed());
    }

    @Test
    public void testParseContent() throws Exception {
        String OFFER_ID = "foobar";
        OfferDetails offerDetails = IdxApiRespParser.parseOfferAsXml(content, OFFER_ID);
        assertNotNull(offerDetails);
        assertEquals(7871, offerDetails.getFeedId());
        assertEquals("20150702_1425", offerDetails.getFeedSession());
        assertEquals(offerDetails.getUpdated(), Date.from(Instant.parse("2015-07-02T14:25:57Z")));
        assertEquals(offerDetails.getLastChecked(), Date.from(Instant.parse("2016-09-28T11:10:37Z")));
        assertTrue(offerDetails.getAvailable());
        assertEquals(offerDetails.getOfferId(), OFFER_ID);

        assertNotNull(offerDetails.getLocalDelivery());
        assertEquals(3, offerDetails.getLocalDelivery().size());
        assertEquals(300.14d, offerDetails.getLocalDelivery().get(0).getCost().doubleValue(), DELTA);
        assertEquals(Currency.RUR, offerDetails.getLocalDelivery().get(0).getCurrency());
        assertEquals(1, (long) offerDetails.getLocalDelivery().get(0).getDayFrom());
        assertEquals(3, (long) offerDetails.getLocalDelivery().get(0).getDayTo());
        assertTrue(offerDetails.getDelivery());
        assertTrue(offerDetails.getPickup());
        assertTrue(offerDetails.getOfferDisabled());
        assertFalse(offerDetails.getOfferHasGone());
        assertEquals(PriceSource.FEED, offerDetails.getPriceSource());
        assertEquals(EnumSet.of(DisabledReason.ABO, DisabledReason.PARTNER_API), offerDetails.getDisabledReasons());
    }

    @Test
    public void parseForFeedRefreshTime() throws IOException {
        Map<Long, Date> feedRefreshRequests = IdxApiRespParser.parseRefreshRequests(
                this.getClass().getResourceAsStream("/files/feed/refreshes.json"));
        assertThat(FEED_REFRESHES, is(feedRefreshRequests));
    }

    @Test
    public void parseCheckSupplierResult() throws IOException {
        CheckSupplierResult checkSupplierResult = IdxApiRespParser.parseCheckSupplierResult(
                getResourceAsString("/files/feed/check-supplier-result.json"));
        assertEquals(getResourceAsString("/files/feed/check-supplier-result.txt"),
                checkSupplierResult.toString());
    }

    private String getResourceAsString(String name) throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream(name)).trim();
    }
}
