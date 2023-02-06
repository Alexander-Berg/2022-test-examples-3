package ru.yandex.market.psku.postprocessor.bazinga.errremove;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.SyncAPI.OffersBatch;
import Market.DataCamp.SyncAPI.SyncChangeOffer;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.psku.postprocessor.common.service.DataCampService;

public class DataCampServiceMock implements DataCampService {
    Map<Pair<Integer, String>, DataCampUnitedOffer.UnitedOffer> offers = new HashMap<>();

    public DataCampServiceMock(Iterable<DataCampUnitedOffer.UnitedOffer> offers) {
        offers.forEach(offer -> {
            DataCampOfferIdentifiers.OfferIdentifiers ids = offer.getBasic().getIdentifiers();
            this.offers.put(Pair.of(ids.getBusinessId(), ids.getOfferId()), offer);
        });
    }

    @Override
    public SyncChangeOffer.FullOfferResponse getOffersByShopId(long shopId,
                                                               SyncChangeOffer.ChangeOfferRequest request) {
        return null;
    }

    @Override
    public void getOffers(Integer businessId, List<String> shopSkuIds, Consumer<OffersBatch.UnitedOffersBatchResponse> responseConsumer) {}

    @Override
    public Stream<DataCampUnitedOffer.UnitedOffer> getOffers(List<Pair<Integer, String>> businessIdsAndOfferIds) {
        return businessIdsAndOfferIds.stream().map(offers::get);
    }

    @Override
    public List<DataCampUnitedOffer.UnitedOffer> updateOffers(List<DataCampUnitedOffer.UnitedOffer> offers) {
        offers.forEach(offer -> {
            DataCampOfferIdentifiers.OfferIdentifiers identifiers = offer.getBasic().getIdentifiers();
            Pair<Integer, String> id = Pair.of(identifiers.getBusinessId(), identifiers.getOfferId());
            this.offers.put(id, offer);
        });

        return offers;
    }
}
