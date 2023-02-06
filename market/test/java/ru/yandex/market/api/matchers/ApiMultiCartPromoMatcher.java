package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.user.order.promo.ApiMultiCartPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;

import java.math.BigDecimal;

public class ApiMultiCartPromoMatcher {
    public static Matcher<ApiMultiCartPromo> apiMultiCartPromo(Matcher<ApiMultiCartPromo> ... matchers) {
        return Matchers.allOf(matchers);
    }

    public static String toStr(ApiMultiCartPromo promo) {
        if (null == promo) {
            return "null";
        }
        return MoreObjects.toStringHelper(ApiMultiCartPromo.class)
            .add("type", promo.getType())
            .add("marketPromoId", promo.getMarketPromoId())
            .add("promoCode", promo.getPromoCode())
            .add("coinId", promo.getCoinId())
            .add("buyerDiscount", promo.getBuyerDiscount())
            .add("deliveryDiscount", promo.getDeliveryDiscount())
            .toString();
    }

    public static Matcher<ApiMultiCartPromo> type(PromoType value) {
        return ApiMatchers.map(
            ApiMultiCartPromo::getType,
            "'type'",
            Matchers.is(value),
            ApiMultiCartPromoMatcher::toStr
        );
    }

    public static Matcher<ApiMultiCartPromo> marketPromoId(String value) {
        return ApiMatchers.map(
            ApiMultiCartPromo::getMarketPromoId,
            "'marketPromoId'",
            Matchers.is(value),
            ApiMultiCartPromoMatcher::toStr
        );
    }

    public static Matcher<ApiMultiCartPromo> promoCode(String value) {
        return ApiMatchers.map(
            ApiMultiCartPromo::getPromoCode,
            "'promoCode'",
            Matchers.is(value),
            ApiMultiCartPromoMatcher::toStr
        );
    }

    public static Matcher<ApiMultiCartPromo> coinId(long value) {
        return ApiMatchers.map(
            ApiMultiCartPromo::getCoinId,
            "'coinId'",
            Matchers.is(value),
            ApiMultiCartPromoMatcher::toStr
        );
    }

    public static Matcher<ApiMultiCartPromo> buyerDiscount(BigDecimal value) {
        return ApiMatchers.map(
            ApiMultiCartPromo::getBuyerDiscount,
            "'buyerDiscount'",
            Matchers.is(value),
            ApiMultiCartPromoMatcher::toStr
        );
    }

    public static Matcher<ApiMultiCartPromo> deliveryDiscount(BigDecimal value) {
        return ApiMatchers.map(
            ApiMultiCartPromo::getDeliveryDiscount,
            "'deliveryDiscount'",
            Matchers.is(value),
            ApiMultiCartPromoMatcher::toStr
        );
    }
}
