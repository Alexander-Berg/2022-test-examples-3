package ru.yandex.market.api.matchers;

import java.math.BigDecimal;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackPromoResponse;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;

public class CashbackPromoMatcher {

    public static Matcher<CashbackPromoResponse> promos(Matcher<CashbackPromoResponse>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<CashbackPromoResponse> amount(BigDecimal value) {
        return ApiMatchers.map(
                CashbackPromoResponse::getAmount,
                "'amount'",
                Matchers.is(value),
                CashbackPromoMatcher::toStr
        );
    }

    public static Matcher<CashbackPromoResponse> promoKey(String value) {
        return ApiMatchers.map(
                CashbackPromoResponse::getPromoKey,
                "'promoKey'",
                Matchers.is(value),
                CashbackPromoMatcher::toStr
        );
    }

    public static Matcher<CashbackPromoResponse> partnerId(Long value) {
        return ApiMatchers.map(
                CashbackPromoResponse::getPartnerId,
                "'partnerId'",
                Matchers.is(value),
                CashbackPromoMatcher::toStr
        );
    }

    public static Matcher<CashbackPromoResponse> nominal(BigDecimal value) {
        return ApiMatchers.map(
                CashbackPromoResponse::getNominal,
                "'nominal'",
                Matchers.is(value),
                CashbackPromoMatcher::toStr
        );
    }

    public static Matcher<CashbackPromoResponse> marketTariff(BigDecimal value) {
        return ApiMatchers.map(
                CashbackPromoResponse::getMarketTariff,
                "'marketTariff'",
                Matchers.is(value),
                CashbackPromoMatcher::toStr
        );
    }

    public static Matcher<CashbackPromoResponse> partnerTariff(BigDecimal value) {
        return ApiMatchers.map(
                CashbackPromoResponse::getPartnerTariff,
                "'partnerTariff'",
                Matchers.is(value),
                CashbackPromoMatcher::toStr
        );
    }

    public static Matcher<CashbackPromoResponse> error(MarketLoyaltyErrorCode value) {
        return ApiMatchers.map(
                CashbackPromoResponse::getError,
                "'MarketLoyaltyErrorCode'",
                Matchers.is(value),
                CashbackPromoMatcher::toStr
        );
    }

    public static Matcher<CashbackPromoResponse> revertToken(String value) {
        return ApiMatchers.map(
                CashbackPromoResponse::getRevertToken,
                "'revertToken'",
                Matchers.is(value),
                CashbackPromoMatcher::toStr
        );
    }

    public static Matcher<CashbackPromoResponse> uiPromoFlags(Matcher<Iterable<String>> matcher) {
        return ApiMatchers.map(
                CashbackPromoResponse::getUiPromoFlags,
                "'uiPromoFlags'",
                matcher,
                CashbackPromoMatcher::toStr
        );
    }

    public static String toStr(CashbackPromoResponse promo) {
        if (null == promo) {
            return "null";
        }

        return MoreObjects.toStringHelper(promo)
                .add("amount", promo.getAmount())
                .add("promoKey", promo.getPromoKey())
                .add("partnerId", promo.getPartnerId())
                .add("nominal", promo.getNominal())
                .add("marketTariff", promo.getMarketTariff())
                .add("partnerTariff", promo.getPartnerTariff())
                .add("error", promo.getError())
                .add("revertToken", promo.getRevertToken())
                .toString();
    }
}
