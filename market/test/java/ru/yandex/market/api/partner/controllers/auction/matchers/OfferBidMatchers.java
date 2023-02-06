package ru.yandex.market.api.partner.controllers.auction.matchers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.hamcrest.Matcher;

import ru.yandex.market.api.partner.controllers.auction.model.OfferBid;
import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * @author vbudnev
 */
public class OfferBidMatchers {
    public static Matcher<OfferBid> hasSearchQuery(@Nullable String searchQuery) {
        return MbiMatchers.<OfferBid>newAllOfBuilder()
                .add(OfferBid::getSearchQuery, searchQuery, "searchQuery")
                .build();
    }

    public static Matcher<OfferBid> hasNoSearchQuery() {
        return hasSearchQuery(null);
    }

    public static Matcher<OfferBid> hasError(@Nullable String errorStr) {
        return MbiMatchers.<OfferBid>newAllOfBuilder()
                .add(OfferBid::getError, errorStr, "error")
                .build();
    }

    public static Matcher<OfferBid> hasNoError() {
        return hasError(null);
    }

    public static Matcher<OfferBid> hasOfferId(@Nonnull AuctionOfferId offerId) {
        return MbiMatchers.<OfferBid>newAllOfBuilder()
                .add(OfferBid::getOfferId, offerId, "offerId")
                .build();
    }

    public static Matcher<OfferBid> hasFeedOfferId(@Nonnull String offerId, long feedId) {
        return MbiMatchers.<OfferBid>newAllOfBuilder()
                .add(OfferBid::getOfferId, new AuctionOfferId(feedId, offerId), "offerId")
                .build();
    }

    public static Matcher<OfferBid> hasTitleOfferId(@Nonnull String offerId) {
        return MbiMatchers.<OfferBid>newAllOfBuilder()
                .add(OfferBid::getOfferId, new AuctionOfferId(offerId), "offerId")
                .build();
    }

}
