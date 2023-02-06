package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import ru.yandex.market.api.domain.v2.GiftOffer;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.api.ApiMatchers.map;

/**
 * Created by fettsery on 07.11.18.
 */
public class GiftOfferMatcher {
    public static Matcher<GiftOffer> giftOffer(String description, String price, int photosCount) {
        return allOf(
            map(
                GiftOffer::getDescription,
                "'description'",
                is(description),
                GiftOfferMatcher::toStr
            ),
            map(
                GiftOffer::getPrice,
                "'price'",
                is(price),
                GiftOfferMatcher::toStr
            ),
            map(GiftOffer::getPhotos,
                "'photos'",
                hasSize(photosCount),
                GiftOfferMatcher::toStr
            )
        );
    }

    public static String toStr(GiftOffer giftOffer) {
        if (null == giftOffer) {
            return "null";
        }
        return MoreObjects.toStringHelper(GiftOffer.class)
            .add("description", giftOffer.getDescription())
            .add("price", giftOffer.getPrice())
            .toString();
    }
}
