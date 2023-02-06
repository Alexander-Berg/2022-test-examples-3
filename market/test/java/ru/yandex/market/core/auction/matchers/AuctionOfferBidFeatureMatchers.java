package ru.yandex.market.core.auction.matchers;

import java.math.BigInteger;
import java.util.Map;

import org.hamcrest.Factory;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.core.auction.model.AuctionBidComponentsLink;
import ru.yandex.market.core.auction.model.AuctionBidStatus;
import ru.yandex.market.core.auction.model.AuctionBidValues;
import ru.yandex.market.core.auction.model.AuctionGoalPlace;
import ru.yandex.market.core.auction.model.AuctionOfferBid;
import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.core.auction.model.BidPlace;

import static org.hamcrest.Matchers.equalTo;

/**
 * feature-матчеры для {@link AuctionOfferBid}.
 *
 * @author vbudnev
 */
public class AuctionOfferBidFeatureMatchers {

    @Factory
    public static Matcher<AuctionOfferBid> hasId(final AuctionOfferId i) {
        return new FeatureMatcher<AuctionOfferBid, AuctionOfferId>(
                equalTo(i),
                "bid id",
                "bid id"
        ) {
            @Override
            protected AuctionOfferId featureValueOf(final AuctionOfferBid actual) {
                return actual.getOfferId();
            }
        };
    }

    @Factory
    public static Matcher<AuctionOfferBid> hasSearchQuery(final String comparedValue) {
        return new FeatureMatcher<AuctionOfferBid, String>(
                equalTo(comparedValue),
                "bid searchQuery",
                "bid searchQuery"
        ) {
            @Override
            protected String featureValueOf(final AuctionOfferBid actual) {
                return actual.getSearchQuery();
            }
        };
    }

    @Factory
    public static Matcher<AuctionOfferBid> hasGroupId(final Long i) {
        return new FeatureMatcher<AuctionOfferBid, Long>(
                equalTo(i),
                "bid groupId",
                "bid groupId"
        ) {
            @Override
            protected Long featureValueOf(final AuctionOfferBid actual) {
                return actual.getGroupId();
            }
        };
    }

    @Factory
    public static Matcher<AuctionOfferBid> hasLinkType(final AuctionBidComponentsLink i) {
        return new FeatureMatcher<AuctionOfferBid, AuctionBidComponentsLink>(
                equalTo(i),
                "bid linkType",
                "bid linkType"
        ) {
            @Override
            protected AuctionBidComponentsLink featureValueOf(final AuctionOfferBid actual) {
                return actual.getLinkType();
            }
        };
    }

    @Factory
    public static Matcher<AuctionOfferBid> hasShopId(final Long comparedVal) {
        return new FeatureMatcher<AuctionOfferBid, Long>(
                equalTo(comparedVal),
                "bid shopId",
                "bid shopId"
        ) {
            @Override
            protected Long featureValueOf(final AuctionOfferBid actual) {
                return actual.getShopId();
            }
        };
    }

    @Factory
    public static Matcher<AuctionOfferBid> hasStatus(final AuctionBidStatus comparedVal) {
        return new FeatureMatcher<AuctionOfferBid, AuctionBidStatus>(
                equalTo(comparedVal),
                "bid status",
                "bid status"
        ) {
            @Override
            protected AuctionBidStatus featureValueOf(final AuctionOfferBid actual) {
                return actual.getStatus();
            }
        };
    }

    @Factory
    public static Matcher<AuctionOfferBid> hasPlaceBid(final BidPlace comparedPlace, final BigInteger comparedPlaceValue) {
        return new FeatureMatcher<AuctionOfferBid, BigInteger>(
                equalTo(comparedPlaceValue),
                "bid value for place " + comparedPlace,
                "bid value for place " + comparedPlace
        ) {
            @Override
            protected BigInteger featureValueOf(final AuctionOfferBid actual) {
                return actual.getValues().getBidValue(comparedPlace);
            }
        };
    }

    @Factory
    public static Matcher<AuctionOfferBid> hasBidValues(final AuctionBidValues comparedValues) {
        return new FeatureMatcher<AuctionOfferBid, AuctionBidValues>(
                equalTo(comparedValues),
                "bid values",
                "bid values"
        ) {
            @Override
            protected AuctionBidValues featureValueOf(final AuctionOfferBid actual) {
                return actual.getValues();
            }
        };
    }

    @Factory
    public static Matcher<AuctionOfferBid> hasKeepOldBidValues() {
        return hasBidValues(AuctionBidValues.KEEP_OLD);
    }

    @Factory
    public static Matcher<AuctionOfferBid> hasResetedBidValues() {
        return hasBidValues(AuctionBidValues.RESET);
    }

    @Factory
    public static Matcher<AuctionOfferBid> hasGoalValues(final Map<MarketReportPlace, AuctionGoalPlace> comparedValues) {
        return new FeatureMatcher<AuctionOfferBid, Map<MarketReportPlace, AuctionGoalPlace>>(
                equalTo(comparedValues),
                "bid goals",
                "bid goals"
        ) {
            @Override
            protected Map<MarketReportPlace, AuctionGoalPlace> featureValueOf(final AuctionOfferBid actual) {
                return actual.getGoalPlaces();
            }
        };
    }

    @Factory
    public static Matcher<AuctionOfferBid> hasNoGoalValues() {
        return hasGoalValues(null);
    }

}
