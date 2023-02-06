package ru.yandex.market.core.auction.matchers;

import org.hamcrest.Matcher;

import ru.yandex.market.core.auction.recommend.OfferAuctionStats;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * @author vbudnev
 */
public class TargetAuctionStatsMatchers {

    public static Matcher<OfferAuctionStats.TargetAuctionStats> hasCurPosAll(Integer expectedValue) {
        return MbiMatchers.<OfferAuctionStats.TargetAuctionStats>newAllOfBuilder()
                .add(OfferAuctionStats.TargetAuctionStats::getCurrentPosAll, expectedValue, "currentPosAll")
                .build();
    }

    public static Matcher<OfferAuctionStats.TargetAuctionStats> hasCurPosTop(Integer expectedValue) {
        return MbiMatchers.<OfferAuctionStats.TargetAuctionStats>newAllOfBuilder()
                .add(OfferAuctionStats.TargetAuctionStats::getCurrentPosTop, expectedValue, "currentPosTop")
                .build();
    }

    public static Matcher<OfferAuctionStats.TargetAuctionStats> hasModelCount(Integer expectedValue) {
        return MbiMatchers.<OfferAuctionStats.TargetAuctionStats>newAllOfBuilder()
                .add(OfferAuctionStats.TargetAuctionStats::getModelCount, expectedValue, "modelCount")
                .build();
    }

    public static Matcher<OfferAuctionStats.TargetAuctionStats> hasTopOffersCount(Integer expectedValue) {
        return MbiMatchers.<OfferAuctionStats.TargetAuctionStats>newAllOfBuilder()
                .add(OfferAuctionStats.TargetAuctionStats::getTopOffersCount, expectedValue, "topOffersCount")
                .build();
    }

}
