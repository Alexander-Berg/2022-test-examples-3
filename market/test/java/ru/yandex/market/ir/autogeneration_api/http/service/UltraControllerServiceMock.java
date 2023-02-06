package ru.yandex.market.ir.autogeneration_api.http.service;

import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.ir.http.UltraControllerService;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class UltraControllerServiceMock implements UltraControllerService {
    public static final String ONE_SIZED = "one_sized";
    public static final String NO_MATCH = "no_match";

    private static final int MATCHED_ID = 1234;

    Map<String, UltraController.DataResponse> specialResponsesByClassifierMagicId;

    public UltraControllerServiceMock() {
        this.specialResponsesByClassifierMagicId = new HashMap<>();
        this.specialResponsesByClassifierMagicId.put(ONE_SIZED, UltraController.DataResponse.newBuilder()
            .addOffers(UltraController.EnrichedOffer.newBuilder().getDefaultInstanceForType())
            .build());
        this.specialResponsesByClassifierMagicId.put(NO_MATCH, UltraController.DataResponse.newBuilder()
            .addOffers(UltraController.EnrichedOffer.newBuilder()
                .setMatchedId(0))
            .build());
    }

    @Override
    public UltraController.EnrichedOffer enrichSingleOffer(UltraController.Offer offer) {
        return null;
    }

    @Override
    public UltraController.DataResponse enrich(UltraController.DataRequest dataRequest) {
        String specialKey = extractControlStringFromClassifierMagicIdInRequest(dataRequest);
        if (specialResponsesByClassifierMagicId.containsKey(specialKey)) {
            return specialResponsesByClassifierMagicId.get(specialKey);
        }
        return UltraController.DataResponse.newBuilder()
            .addAllOffers(
                dataRequest.getOffersList().stream()
                    .map(offer -> UltraController.EnrichedOffer.newBuilder()
                        .setMatchedId(MATCHED_ID)
                        .build()
                    )
                    .collect(Collectors.toList())
            )
            .build();
    }

    @Override
    public UltraController.MovedResponse getMovedOffers(UltraController.MovedRequest movedRequest) {
        return null;
    }

    @Override
    public UltraController.VoidResponse reload(UltraController.VoidRequest voidRequest) {
        return null;
    }

    @Override
    public UltraController.SKUMappingResponse getMarketSKU(UltraController.MarketSKURequest marketSKURequest) {
        return null;
    }

    @Override
    public UltraController.SKUMappingResponse getShopSKU(UltraController.ShopSKURequest shopSKURequest) {
        return null;
    }

    @Override
    public MonitoringResult ping() {
        return null;
    }

    @Override
    public MonitoringResult monitoring() {
        return null;
    }

    private String extractControlStringFromClassifierMagicIdInRequest(
        UltraController.DataRequest ultraControllerRequest) {
        return ultraControllerRequest.getOffersList().get(0).getClassifierMagicId();
    }

}
