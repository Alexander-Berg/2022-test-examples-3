package ru.yandex.market.core.auction.matchers;

import org.hamcrest.Matcher;

import ru.yandex.market.common.report.model.RecommendationType;
import ru.yandex.market.core.auction.recommend.OfferAuctionStats;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author vbudnev
 */
public class OfferAuctionStatsMatchers {

    public static Matcher<OfferAuctionStats> hasMinBid(Integer expectedValue) {
        return MbiMatchers.<OfferAuctionStats>newAllOfBuilder()
                .add(OfferAuctionStats::getMinBid, expectedValue, "minBid")
                .build();
    }

    public static Matcher<OfferAuctionStats> hasMinFee(Integer expectedValue) {
        return MbiMatchers.<OfferAuctionStats>newAllOfBuilder()
                .add(OfferAuctionStats::getMinFee, expectedValue, "minFee")
                .build();
    }


    public static Matcher<OfferAuctionStats> containsStats(RecommendationType expectedType) {
        return MbiMatchers.<OfferAuctionStats>newAllOfBuilder()
                .add(x -> x.getTargetStats(expectedType), not(nullValue()), "targetStats for " + expectedType)
                .build();
    }
}
