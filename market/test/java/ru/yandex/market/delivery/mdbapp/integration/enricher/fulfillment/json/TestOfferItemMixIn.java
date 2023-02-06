package ru.yandex.market.delivery.mdbapp.integration.enricher.fulfillment.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import ru.yandex.market.checkout.checkouter.order.OfferItem;
import ru.yandex.market.common.report.model.FeedOfferId;

public class TestOfferItemMixIn extends OfferItem {
    @JsonProperty("feedOfferId")
    @Override
    public FeedOfferId getFeedOfferId() {
        return super.getFeedOfferId();
    }
}
