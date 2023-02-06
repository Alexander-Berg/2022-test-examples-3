package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.user.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;

import java.math.BigDecimal;

public class ItemPromoMatcher {
    public static Matcher<ItemPromo> itemPromo(Matcher<ItemPromo> ... matchers) {
        return Matchers.allOf(matchers);
    }

    public static String toStr(ItemPromo promo) {
        if (null == promo) {
            return "null";
        }
        return MoreObjects.toStringHelper(ItemPromo.class)
            .add("type", promo.getType())
            .add("marketPromoId", promo.getMarketPromoId())
            .add("promoCode", promo.getPromoCode())
            .add("coinId", promo.getCoinId())
            .add("buyerDiscount", promo.getBuyerDiscount())
            .add("subsidy", promo.getSubsidy())
            .add("buyerSubsidy", promo.getBuyerSubsidy())
            .toString();
    }

    public static Matcher<ItemPromo> type(PromoType value) {
        return ApiMatchers.map(
            ItemPromo::getType,
            "'type'",
            Matchers.is(value),
            ItemPromoMatcher::toStr
        );
    }

    public static Matcher<ItemPromo> marketPromoId(String value) {
        return ApiMatchers.map(
            ItemPromo::getMarketPromoId,
            "'marketPromoId'",
            Matchers.is(value),
            ItemPromoMatcher::toStr
        );
    }

    public static Matcher<ItemPromo> promoCode(String value) {
        return ApiMatchers.map(
            ItemPromo::getPromoCode,
            "'promoCode'",
            Matchers.is(value),
            ItemPromoMatcher::toStr
        );
    }

    public static Matcher<ItemPromo> coinId(long value) {
        return ApiMatchers.map(
            ItemPromo::getCoinId,
            "'coinId'",
            Matchers.is(value),
            ItemPromoMatcher::toStr
        );
    }

    public static Matcher<ItemPromo> buyerDiscount(BigDecimal value) {
        return ApiMatchers.map(
            ItemPromo::getBuyerDiscount,
            "'buyerDiscount'",
            Matchers.is(value),
            ItemPromoMatcher::toStr
        );
    }

    public static Matcher<ItemPromo> subsidy(BigDecimal value) {
        return ApiMatchers.map(
            ItemPromo::getSubsidy,
            "'subsidy'",
            Matchers.is(value),
            ItemPromoMatcher::toStr
        );
    }

    public static Matcher<ItemPromo> buyerSubsidy(BigDecimal value) {
        return ApiMatchers.map(
            ItemPromo::getBuyerSubsidy,
            "'buyerSubsidy'",
            Matchers.is(value),
            ItemPromoMatcher::toStr
        );
    }
}
