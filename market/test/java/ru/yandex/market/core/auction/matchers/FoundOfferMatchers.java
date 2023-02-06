package ru.yandex.market.core.auction.matchers;

import org.hamcrest.Matcher;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * @author vbudnev
 */
public class FoundOfferMatchers {

    public static Matcher<FoundOffer> hasBid(Integer expectedValue) {
        return MbiMatchers.<FoundOffer>newAllOfBuilder()
                .add(FoundOffer::getBid, expectedValue, "bid")
                .build();
    }

    public static Matcher<FoundOffer> hasFee(Integer expectedValue) {
        return MbiMatchers.<FoundOffer>newAllOfBuilder()
                .add(FoundOffer::getFee, expectedValue, "fee")
                .build();
    }

    public static Matcher<FoundOffer> hasMinBid(Integer expectedValue) {
        return MbiMatchers.<FoundOffer>newAllOfBuilder()
                .add(FoundOffer::getMinBid, expectedValue, "minBid")
                .build();
    }

    public static Matcher<FoundOffer> hasMinFee(Integer expectedValue) {
        return MbiMatchers.<FoundOffer>newAllOfBuilder()
                .add(FoundOffer::getMinFee, expectedValue, "minFee")
                .build();
    }

    public static Matcher<FoundOffer> hasPullupFlag(Boolean expectedValue) {
        return MbiMatchers.<FoundOffer>newAllOfBuilder()
                .add(FoundOffer::getPullUpBids, expectedValue, "pullUpBids")
                .build();
    }

    public static Matcher<FoundOffer> hasWareMd5(String expectedValue) {
        return MbiMatchers.<FoundOffer>newAllOfBuilder()
                .add(FoundOffer::getWareMd5, expectedValue, "wareMd5")
                .build();
    }

    public static Matcher<FoundOffer> hasHyperId(Long expectedValue) {
        return MbiMatchers.<FoundOffer>newAllOfBuilder()
                .add(FoundOffer::getHyperId, expectedValue, "hyperId")
                .build();
    }

    public static Matcher<FoundOffer> hasUrl(String expectedValue) {
        return MbiMatchers.<FoundOffer>newAllOfBuilder()
                .add(FoundOffer::getUrl, expectedValue, "url")
                .build();
    }

    public static Matcher<FoundOffer> hasName(String expectedValue) {
        return MbiMatchers.<FoundOffer>newAllOfBuilder()
                .add(FoundOffer::getName, expectedValue, "name")
                .build();
    }

    public static Matcher<FoundOffer> hasPriceCurrency(Currency expectedValue) {
        return MbiMatchers.<FoundOffer>newAllOfBuilder()
                .add(FoundOffer::getPriceCurrency, expectedValue, "pirceCurrency")
                .build();
    }

    public static Matcher<FoundOffer> hasHidd(Integer expectedValue) {
        return MbiMatchers.<FoundOffer>newAllOfBuilder()
                .add(FoundOffer::getHyperCategoryId, expectedValue, "hyperCategoryId")
                .build();
    }

}
