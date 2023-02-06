package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.user.order.promo.ApiOrderPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;

import java.math.BigDecimal;

public class ApiOrderPromoMatcher {
    public static Matcher<ApiOrderPromo> orderPromo(Matcher<ApiOrderPromo> ... matchers) {
        return Matchers.allOf(matchers);
    }


    public static Matcher<ApiOrderPromo> type(PromoType type) {
        return ApiMatchers.map(
            ApiOrderPromo::getType,
            "'type'",
            Matchers.is(type),
            ApiOrderPromoMatcher::toStr
        );
    }
    public static Matcher<ApiOrderPromo> marketPromoId(String marketPromoId) {
        return ApiMatchers.map(
            ApiOrderPromo::getMarketPromoId,
            "'marketPromoId'",
            Matchers.is(marketPromoId),
            ApiOrderPromoMatcher::toStr
        );
    }
    public static Matcher<ApiOrderPromo> promoCode(String promoCode) {
        return ApiMatchers.map(
            ApiOrderPromo::getPromoCode,
            "'promoCode'",
            Matchers.is(promoCode),
            ApiOrderPromoMatcher::toStr
        );
    }
    public static Matcher<ApiOrderPromo> coinId(Long coinId) {
        return ApiMatchers.map(
            ApiOrderPromo::getCoinId,
            "'coinId'",
            Matchers.is(coinId),
            ApiOrderPromoMatcher::toStr
        );
    }
    public static Matcher<ApiOrderPromo> buyerItemsDiscount(BigDecimal buyerItemsDiscount) {
        return ApiMatchers.map(
            ApiOrderPromo::getBuyerDiscount,
            "'buyerItemsDiscount'",
            Matchers.is(buyerItemsDiscount),
            ApiOrderPromoMatcher::toStr
        );
    }
    public static Matcher<ApiOrderPromo> deliveryDiscount(BigDecimal deliveryDiscount) {
        return ApiMatchers.map(
            ApiOrderPromo::getDeliveryDiscount,
            "'deliveryDiscount'",
            Matchers.is(deliveryDiscount),
            ApiOrderPromoMatcher::toStr
        );
    }
    public static Matcher<ApiOrderPromo> subsidy(BigDecimal subsidy) {
        return ApiMatchers.map(
            ApiOrderPromo::getSubsidy,
            "'subsidy'",
            Matchers.is(subsidy),
            ApiOrderPromoMatcher::toStr
        );
    }
    public static Matcher<ApiOrderPromo> buyerSubsidy(BigDecimal buyerSubsidy) {
        return ApiMatchers.map(
            ApiOrderPromo::getBuyerSubsidy,
            "'buyerSubsidy'",
            Matchers.is(buyerSubsidy),
            ApiOrderPromoMatcher::toStr
        );
    }

    public static String toStr(ApiOrderPromo promo) {
        if (null == promo) {
            return "null";
        }
        return MoreObjects.toStringHelper(ApiOrderPromo.class)
            .add("type", promo.getType().getCode())
            .add("marketPromoId", promo.getMarketPromoId())
            .add("promoCode", promo.getPromoCode())
            .add("coinId", promo.getCoinId())
            .add("buyerItemsDiscount", promo.getBuyerDiscount())
            .add("deliveryDiscount", promo.getDeliveryDiscount())
            .add("subsidy", promo.getSubsidy())
            .add("buyerSubsidy", promo.getBuyerSubsidy())
            .toString();
    }
}
