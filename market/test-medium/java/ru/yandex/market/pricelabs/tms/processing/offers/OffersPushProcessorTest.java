package ru.yandex.market.pricelabs.tms.processing.offers;

import java.util.List;

import Market.DataCamp.API.ExportMessageOuterClass.ExportMessage;

import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;
import ru.yandex.market.pricelabs.tms.idx.OfferPush;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.offerPush;

public class OffersPushProcessorTest extends AbstractTmsSpringConfiguration {

    //@Test
    public void testPush() {
        var messages = List.of(
                message(offerPush(1, 1, "1")),
                message(offerPush(2, 2, "2")),
                message(offerPush(2, 2, "3")),
                message(offerPush(3, 3, "4")),
                message(offerPush(3, 3, "5")),
                message(offerPush(3, 3, "6")));

        var shopOffersProcessor = mock(ShopOffersProcessor.class);
        var processor = new OffersPushProcessor(timeSource(), shopOffersProcessor, 4, 1);

        processor.accept(messages);

        TimingUtils.addTime((OffersPushProcessor.OFFERS_BATCH_MAX_WAIT + 1) * 1000);

        // Три магазина - три вызова
        // Таймаут, чтобы сработал шедулер
        verify(shopOffersProcessor, timeout(5000).times(3)).pushOffers(anyList());
        verifyNoMoreInteractions(shopOffersProcessor);
    }

    static ExportMessage message(OfferPush offerpush) {
        return ExportMessage.newBuilder().setOffer(offerpush.getDcOffer()).build();
    }
}
