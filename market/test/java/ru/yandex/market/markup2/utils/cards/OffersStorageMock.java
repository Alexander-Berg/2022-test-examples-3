package ru.yandex.market.markup2.utils.cards;

import ru.yandex.market.mbo.http.OfferStorageServiceStub;
import ru.yandex.market.mbo.http.OffersStorage;

import java.util.ArrayList;
import java.util.List;

public class OffersStorageMock extends OfferStorageServiceStub {
    private List<Offer> offers;
    private int requestCounter;

    //NOTE: use this constructor to create broken offersStorage
    public OffersStorageMock() { }

    public OffersStorageMock(List<Offer> offers) {
        this.offers = offers;
        this.requestCounter = 0;
    }

    public OffersStorage.GetOffersResponse getOffersByIds(OffersStorage.GetOffersRequest getOffersRequest) {
        requestCounter += 1;

        OffersStorage.GetOffersResponse.Builder responseBuilder =
            OffersStorage.GetOffersResponse.newBuilder();
        OffersStorage.GenerationDataOffer.Builder offersBuilder = OffersStorage.GenerationDataOffer.newBuilder();

        List<Offer> foundOffers = new ArrayList<>();

        getOffersRequest.getClassifierMagicIdsList().forEach(id -> {
            Offer foundOffer = offers.stream()
                                     .filter(offer -> offer.getId().equals(id))
                                     .findFirst().get();

            foundOffers.add(foundOffer);
        });


        for (Offer foundOffer: foundOffers) {
            offersBuilder.setClassifierMagicId(foundOffer.getId());
            offersBuilder.setLongClusterId(foundOffer.getClusterId());
            offersBuilder.setDescription(foundOffer.getDescription());
            offersBuilder.setModelId(foundOffer.getModelId());
            offersBuilder.setPicUrls(foundOffer.getPicUrl());
            offersBuilder.setFeedId(foundOffer.getFeedId());
            offersBuilder.setWareMd5(foundOffer.getWareMd5());
            responseBuilder.addOffers(offersBuilder);
        }

        return responseBuilder.build();
    }

    public int getRequestCounter() {
        return requestCounter;
    }
}
