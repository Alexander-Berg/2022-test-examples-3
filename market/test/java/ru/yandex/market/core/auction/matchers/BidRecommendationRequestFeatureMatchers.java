package ru.yandex.market.core.auction.matchers;

import org.hamcrest.Factory;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.core.auction.recommend.BidRecommendationRequest;

import static org.hamcrest.Matchers.equalTo;

/**
 * feature-матчеры для {@link BidRecommendationRequest}.
 *
 * @author vbudnev
 */
public class BidRecommendationRequestFeatureMatchers {

    @Factory
    public static Matcher<BidRecommendationRequest> hasRegionId(final Long comparedVal) {
        return new FeatureMatcher<BidRecommendationRequest, Long>(
                equalTo(comparedVal),
                "regionId",
                "regionId"
        ) {
            @Override
            protected Long featureValueOf(final BidRecommendationRequest actual) {
                return actual.getRegionId();
            }
        };
    }

    @Factory
    public static Matcher<BidRecommendationRequest> hasOfferId(final AuctionOfferId comparedVal) {
        return new FeatureMatcher<BidRecommendationRequest, AuctionOfferId>(
                equalTo(comparedVal),
                "offerId",
                "offerId"
        ) {
            @Override
            protected AuctionOfferId featureValueOf(final BidRecommendationRequest actual) {
                return actual.getOfferId();
            }
        };
    }

    @Factory
    public static Matcher<BidRecommendationRequest> hasShopId(final Long comparedVal) {
        return new FeatureMatcher<BidRecommendationRequest, Long>(
                equalTo(comparedVal),
                "shopId",
                "shopId"
        ) {
            @Override
            protected Long featureValueOf(final BidRecommendationRequest actual) {
                return actual.getShopId();
            }
        };
    }

    @Factory
    public static Matcher<BidRecommendationRequest> hasSearchQuery(final String comparedVal) {
        return new FeatureMatcher<BidRecommendationRequest, String>(
                equalTo(comparedVal),
                "searchQuery",
                "searchQuery"
        ) {
            @Override
            protected String featureValueOf(final BidRecommendationRequest actual) {
                return actual.getSearchQuery();
            }
        };
    }

    @Factory
    public static Matcher<BidRecommendationRequest> hasAuctionBulkQuery(final String comparedVal) {
        return new FeatureMatcher<BidRecommendationRequest, String>(
                equalTo(comparedVal),
                "auctionBulkQuery",
                "auctionBulkQuery"
        ) {
            @Override
            protected String featureValueOf(final BidRecommendationRequest actual) {
                return actual.getAuctionBlockQuery();
            }
        };
    }

}
