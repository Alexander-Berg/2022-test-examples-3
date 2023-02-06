package ru.yandex.market.abo.core.ticket;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.offer.report.OfferService;
import ru.yandex.market.abo.core.offer.report.ReportParam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OfferDbServiceTest extends EmptyTest {
    @Autowired
    private OfferDbService offerDbService;
    @Autowired
    private OfferService offerService;

    @Test
    public void testStore() throws Exception {
        long shopId = 774L;
        List<Offer> offers = offerService.findWithParams(
                ReportParam.from(ReportParam.Type.CACHE, ReportParam.Value.CACHE_NO),
                ReportParam.from(ReportParam.Type.SORT, ReportParam.Value.SORT_BY_RANDOM),
                ReportParam.from(ReportParam.Type.SHOP_ID, shopId),
                ReportParam.from(ReportParam.Type.PAGE_SIZE, 1)
        );
        if (!offers.isEmpty()) {
            Offer offer = offers.get(0);
            offer.setDeliveryOptions(true);
            long offerId = offerDbService.storeOffer(offer);
            assertTrue(offerId > 0);

            Offer foundOffer = offerDbService.load(offerId);
            assertNotNull(foundOffer);
            assertEquals(offer.getShopOfferId(), foundOffer.getShopOfferId());
            assertEquals(offer.getFeedId(), foundOffer.getFeedId());
            assertEquals(offer.getShopId(), foundOffer.getShopId());
            assertEquals(offer.getFeedOfferId(), foundOffer.getFeedOfferId());
            assertEquals(offer.getWareMd5(), foundOffer.getWareMd5());
            assertEquals(offer.getUrlHash(), foundOffer.getUrlHash());
            assertEquals(offer.getPrice(), foundOffer.getPrice());
            assertEquals(offer.getPriceCurrency(), foundOffer.getPriceCurrency());
            assertEquals(offer.getHyperId(), foundOffer.getHyperId());
            assertEquals(offer.getClassifierMagicId(), foundOffer.getClassifierMagicId());
            assertTrue(foundOffer.getDeliveryOptions());
        }
    }
}
