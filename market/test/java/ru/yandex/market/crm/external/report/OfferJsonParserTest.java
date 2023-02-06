package ru.yandex.market.crm.external.report;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.domain.report.Offer;

public class OfferJsonParserTest {

    @Test
    public void parseTest() {
        ReportResultsListParser<Offer> parser = new ReportResultsListParser<>(new OfferJsonParser());
        List<Offer> offers = parser.parse(getClass().getResourceAsStream("offers_response.json"));

        Assertions.assertNotNull(offers);
        Assertions.assertEquals(1, offers.size());

        Offer offer = offers.get(0);
        Assertions.assertNull(offer.getMarketSkuId());
        Assertions.assertTrue(offer.isOnstock());

        Assertions.assertEquals(Long.valueOf(54), offer.getPriorityRegion());
        Assertions.assertEquals("115757023", offer.getModelId());

        Assertions.assertEquals("7290", offer.getPrice().getValue());
        Assertions.assertEquals("RUR", offer.getPrice().getCurrency());
        Assertions.assertNull(offer.getPrice().getOldValue());
        Assertions.assertFalse(offer.getPrice().getDeliveryIncluded());

        Assertions.assertEquals("gift-with-purchase", offer.getPromo().getType());
        Assertions.assertEquals("rxSjyrh7s_XbRize5rJ9CQ", offer.getPromo().getKey());
    }
}
