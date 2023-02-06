package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.DeliveryV2;
import ru.yandex.market.api.domain.v2.OfferPriceV2;

public class DeliveryMatcher {
    public static Matcher<DeliveryV2> delivery(Matcher<DeliveryV2> ... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<DeliveryV2> price(Matcher<OfferPriceV2> price) {
        return  ApiMatchers.map(
            x -> (OfferPriceV2) x.getPrice(),
            "'price'",
            price,
            DeliveryMatcher::toStr
        );
    }

    public static String toStr(DeliveryV2 delivery) {
        if (null == delivery) {
            return "null";
        }
        return MoreObjects.toStringHelper(delivery)
            .add("price", OfferPriceMatcher.toStr(delivery.getPrice()))
            .toString();
    }
}
