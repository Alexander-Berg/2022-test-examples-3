package ru.yandex.market.core.auction.matchers;

import org.hamcrest.Matcher;

import ru.yandex.market.core.auction.model.ComplexBid;
import ru.yandex.market.core.auction.model.StatusCode;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * @author vbudnev
 */
public class ComplexBidMatchers {

    public static Matcher<ComplexBid> hasBid(Integer expectedValue) {
        return MbiMatchers.<ComplexBid>newAllOfBuilder()
                .add(ComplexBid::getBid, expectedValue, "bid")
                .build();
    }

    public static Matcher<ComplexBid> hasFee(Integer expectedValue) {
        return MbiMatchers.<ComplexBid>newAllOfBuilder()
                .add(ComplexBid::getFee, expectedValue, "fee")
                .build();
    }

    public static Matcher<ComplexBid> hasStatus(StatusCode expectedValue) {
        return MbiMatchers.<ComplexBid>newAllOfBuilder()
                .add(ComplexBid::getCode, expectedValue, "code")
                .build();
    }

}
