package ru.yandex.market.mboc.common.services.ultracontroller;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import lombok.extern.slf4j.Slf4j;

import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.mboc.common.datacamp.DataCampOfferUtil;
import ru.yandex.market.mboc.common.offers.model.Offer;

@Slf4j
public class UltraControllerServiceMock implements UltraControllerService {
    private Function<List<Offer>, List<UltraController.EnrichedOffer>> createMockResponse = offers ->
        offers.stream()
            .map(o -> UltraController.EnrichedOffer.newBuilder().build())
            .collect(Collectors.toList());

    public void createMockResponse(DataCampOffer.Offer... dcOffers) {
        createMockResponse(Arrays.asList(dcOffers));
    }

    public void createMockResponse(List<DataCampOffer.Offer> dcOffers) {
        var sku2enriched = dcOffers.stream()
            .collect(Collectors.toMap(
                DataCampOfferUtil::extractExternalBusinessSkuKey,
                DataCampOfferUtil::convertToEnrichedOffer
            ));
        createMockResponse = offers ->
            offers.stream()
                .map(o ->
                    sku2enriched.getOrDefault(o.getBusinessSkuKey(), UltraController.EnrichedOffer.newBuilder().build())
                )
                .collect(Collectors.toList());
    }

    @Override
    public void enrich(Consumer<String> humanLogger,
                       List<Offer> offers,
                       BiConsumer<List<Offer>, List<UltraController.EnrichedOffer>> enrichmentConsumer,
                       boolean usePrice) {
        enrichmentConsumer.accept(offers, createMockResponse.apply(offers));
    }
}
