package ru.yandex.market.core.auction.matchers;

import javax.annotation.Nonnull;

import org.hamcrest.Matcher;

import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * @author vbudnev
 */
public class OfferIdMatchers {

    public static Matcher<AuctionOfferId> hasOfferId(@Nonnull AuctionOfferId offerId) {
        return MbiMatchers.<AuctionOfferId>newAllOfBuilder()
                .add(AuctionOfferId::getId, offerId.getId(), "offerId")
                .add(AuctionOfferId::getFeedId, offerId.getFeedId(), "feedId")
                .add(AuctionOfferId::getIdType, offerId.getIdType(), "offerType")
                .build();
    }

    public static Matcher<AuctionOfferId> hasOfferId(@Nonnull String offerId, @Nonnull long feedId) {
        return hasOfferId(new AuctionOfferId(feedId, offerId));
    }

    public static Matcher<AuctionOfferId> hasOfferId(@Nonnull String offerId) {
        return hasOfferId(new AuctionOfferId(offerId));
    }

}
