package ru.yandex.market.partner.content.common.service.mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.SyncAPI.OffersBatch;
import Market.DataCamp.SyncAPI.SyncChangeOffer;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.partner.content.common.service.DataCampService;

public class DataCampServiceMock implements DataCampService {
    private final Map<Integer, DataCampOffer.Offer[]> offersByBusinessId = new HashMap<>();

    public void setOffersForBusinessId(int businessId, DataCampOffer.Offer... offers) {
        offersByBusinessId.put(businessId, offers);
    }

    @Override
    public SyncChangeOffer.FullOfferResponse getOffersByShopId(long shopId, SyncChangeOffer.ChangeOfferRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getOffersAndProcessResponse(Integer businessId, List<String> shopSkuIds, Consumer<OffersBatch.UnitedOffersBatchResponse> responseConsumer) {
        DataCampOffer.Offer[] offers = offersByBusinessId.get(businessId);
        if (offers != null && offers.length > 0) {
            OffersBatch.UnitedOffersBatchResponse response = OffersBatch.UnitedOffersBatchResponse.newBuilder()
                .addAllEntries(Arrays.stream(offers)
                    .map(offer -> OffersBatch.UnitedOffersBatchResponse.Entry.newBuilder()
                        .setUnitedOffer(DataCampUnitedOffer.UnitedOffer.newBuilder().setBasic(offer))
                        .build())
                    .collect(Collectors.toList()))
                .build();
            responseConsumer.accept(response);
        }
    }

    @Override
    public Stream<DataCampUnitedOffer.UnitedOffer> getOffersAndProcessResponse(List<Pair<Integer, String>> businessIdsAndOfferIds) {
        return null;
    }

    @Override
    public OffersBatch.UnitedOffersBatchResponse updateOffers(List<DataCampUnitedOffer.UnitedOffer> offers) {
        return null;
    }
}
