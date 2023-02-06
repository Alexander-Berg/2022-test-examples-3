package ru.yandex.market.aliasmaker.cache.offers;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import ru.yandex.market.aliasmaker.models.CategoryKnowledge;
import ru.yandex.market.aliasmaker.offers.Offer;
import ru.yandex.market.mbo.http.OffersStorage;

/**
 * @author york
 * @since 09.04.2020
 */
public class YtSessionReaderMock implements YtSessionReader {
    private List<OffersStorage.GenerationDataOffer> offers;
    private String sessionId;

    public void setOffers(String sessionId, List<OffersStorage.GenerationDataOffer> offers) {
        this.offers = offers;
        this.sessionId = sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void readOffers(SerializedSessionsService.SessionInfo sessionInfo, CategoryKnowledge categoryKnowledge,
                           Consumer<Offer> offerConsumer) {
        offers.forEach(o -> offerConsumer.accept(convert(o)));
    }

    private Offer convert(OffersStorage.GenerationDataOffer offer) {
        return new Offer(offer, Collections.emptyList(), Collections.emptyList());
    }

    @Override
    public String getLastPreparedSession() {
        return sessionId;
    }
}
