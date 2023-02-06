package ru.yandex.market.loyalty.core.utils;

import org.hamcrest.Matcher;

import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class DiscountResponseUtil {

    public static <T> Matcher<T> hasCouponError(MarketLoyaltyErrorCode errorCode) {
        return hasProperty(
                "couponError", hasProperty(
                        "error",
                        hasProperty("code", equalTo(errorCode.name()))
                )
        );
    }

    public static <T> Matcher<T> hasNoErrors() {
        return allOf(
                hasProperty("couponError", nullValue()),
                hasProperty("coinErrors", is(empty()))
        );
    }
}
