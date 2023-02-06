package ru.yandex.market.checkout.util.report.generators.offerinfo;

import java.util.List;

import ru.yandex.market.common.report.model.FoundOffer;

public interface OfferInfoGeneratorParameters {

    long getShopId();

    List<FoundOffer> getFoundOffers();
}
