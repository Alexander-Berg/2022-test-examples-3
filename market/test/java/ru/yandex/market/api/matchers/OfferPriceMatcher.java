package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.OfferPrice;
import ru.yandex.market.api.domain.v2.DiscountType;
import ru.yandex.market.api.domain.v2.OfferPriceV2;

public class OfferPriceMatcher {
    public static Matcher<OfferPriceV2> price(Matcher<OfferPriceV2> ... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<OfferPriceV2> value(String value) {
        return ApiMatchers.map(
            OfferPriceV2::getValue,
            "'value'",
            Matchers.is(value),
            OfferPriceMatcher::toStr
        );
    }

    public static Matcher<OfferPriceV2> discountType(DiscountType discountType) {
        return ApiMatchers.map(
            OfferPriceV2::getDiscount,
            "'discountType'",
            Matchers.is(discountType),
            OfferPriceMatcher::toStr
        );
    }

    public static String toStr(OfferPrice price) {
        if (null == price) {
            return "null";
        }

        if (!(price instanceof OfferPriceV2)) {
            return "not offer v2";
        }

        OfferPriceV2 priceV2 = (OfferPriceV2) price;

        return MoreObjects.toStringHelper(price)
            .add("value", priceV2.getValue())
            .add("discountType", priceV2.getDiscountType())
            .toString();
    }
}
