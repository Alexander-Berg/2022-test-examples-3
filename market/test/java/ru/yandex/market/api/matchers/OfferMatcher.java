package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.v2.DeliveryV2;
import ru.yandex.market.api.domain.v2.OfferPriceV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.domain.v2.RegionV2;
import ru.yandex.market.api.domain.v2.ShopInfoV2;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.api.ApiMatchers.map;

public class OfferMatcher {
    public static Matcher<OfferV2> offer(Matcher<OfferV2> ... matchers) {
        return allOf(matchers);
    }

    public static Matcher<OfferV2> name(String name) {
        return map(
          OfferV2::getName,
          "'name'",
          is(name),
          OfferMatcher::toStr
        );
    }

    public static Matcher<OfferV2> sku(String sku) {
        return map(
            OfferV2::getSku,
            "'sku'",
            is(sku),
            OfferMatcher::toStr
        );
    }

    public static Matcher<OfferV2> skuType(String skuType) {
        return map(
            OfferV2::getSkuType,
            "'skuType'",
            is(skuType),
            OfferMatcher::toStr
        );
    }

    public static Matcher<OfferV2> url(String url) {
        return map(
            OfferV2::getUrl,
            "'url'",
            is(url),
            OfferMatcher::toStr
        );
    }

    public static Matcher<OfferV2> supplier(Matcher<ShopInfoV2> shop) {
        return ApiMatchers.map(
            OfferV2::getSupplier,
            "'shop'",
            shop,
            OfferMatcher::toStr
        );
    }

    public static Matcher<OfferV2> offerId(Matcher<OfferId> ... matchers) {
        return map(
            OfferV2::getId,
            "'offerId'",
            allOf(matchers),
            OfferMatcher::toStr
        );
    }

    public static Matcher<OfferId> wareMd5(String wareMd5) {
        return map(
            OfferId::getWareMd5,
            "'wareMd5'",
            is(wareMd5),
            OfferMatcher::toStr
        );
    }

    public static Matcher<OfferId> feeShow(String feeShow) {
        return map(
            OfferId::getFeeShow,
            "'feeShow'",
            is(feeShow),
            OfferMatcher::toStr
        );
    }

    public static Matcher<OfferV2> price(Matcher<OfferPriceV2> price) {
        return map(
            x -> (OfferPriceV2) x.getPrice(),
            "'price'",
            price,
            OfferMatcher::toStr
        );
    }

    public static Matcher<OfferV2> delivery(Matcher<DeliveryV2> delivery) {
        return map(
            x -> (DeliveryV2) x.getDelivery(),
            "'delivery'",
            delivery,
            OfferMatcher::toStr
        );
    }

    public static Matcher<OfferV2> manufactCountries(Matcher<Iterable<? extends RegionV2>> manufactCountries) {
        return map(
            OfferV2::getManufactCountries,
            "'manufactCountries'",
            manufactCountries,
            OfferMatcher::toStr
        );
    }

    public static String toStr(OfferV2 offer) {
        if (null == offer) {
            return "null";
        }

        return MoreObjects.toStringHelper(offer)
            .add("id", toStr(offer.getId()))
            .add("sku", offer.getName())
            .add("skuType", offer.getSkuType())
            .add("url", offer.getUrl())
            .add("supplier", ShopInfoMatcher.toStr(offer.getSupplier()))
            .add(
                "manufactCountries",
                ApiMatchers.collectionToStr(offer.getManufactCountries(), RegionV2Matcher::toStr)
            )
            .toString();
    }

    private static String toStr(OfferId offerId) {
        if (null == offerId) {
            return "null";
        }
        return MoreObjects.toStringHelper(offerId)
            .add("wareMd5", offerId.getWareMd5())
            .add("feeShow", offerId.getFeeShow())
            .toString();
    }
}
