package ru.yandex.market.api.matchers;

import java.math.BigDecimal;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackThreshold;
import ru.yandex.market.loyalty.api.model.perk.PerkType;

public class CashbackThresholdMatcher {

    public static Matcher<CashbackThreshold> thresholds(Matcher<CashbackThreshold>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<CashbackThreshold> promoKey(String value) {
        return ApiMatchers.map(
                CashbackThreshold::getPromoKey,
                "'promoKey'",
                Matchers.is(value),
                CashbackThresholdMatcher::toStr
        );
    }

    public static Matcher<CashbackThreshold> requiredPerks(Matcher<Iterable<PerkType>> matcher) {
        return ApiMatchers.map(
                CashbackThreshold::getRequiredPerks,
                "'requiredPerks'",
                matcher,
                CashbackThresholdMatcher::toStr
        );
    }

    public static Matcher<CashbackThreshold> remainingMultiCartTotal(BigDecimal value) {
        return ApiMatchers.map(
                CashbackThreshold::getRemainingMultiCartTotal,
                "'remainingMultiCartTotal'",
                Matchers.is(value),
                CashbackThresholdMatcher::toStr
        );
    }

    public static Matcher<CashbackThreshold> minMultiCartTotal(BigDecimal value) {
        return ApiMatchers.map(
                CashbackThreshold::getMinMultiCartTotal,
                "'minMultiCartTotal'",
                Matchers.is(value),
                CashbackThresholdMatcher::toStr
        );
    }

    public static Matcher<CashbackThreshold> amount(BigDecimal value) {
        return ApiMatchers.map(
                CashbackThreshold::getAmount,
                "'amount'",
                Matchers.is(value),
                CashbackThresholdMatcher::toStr
        );
    }

    public static Matcher<CashbackThreshold> agitationPriority(Integer value) {
        return ApiMatchers.map(
                CashbackThreshold::getAgitationPriority,
                "'agitationPriority'",
                Matchers.is(value),
                CashbackThresholdMatcher::toStr
        );
    }

    public static String toStr(CashbackThreshold threshold) {
        if (null == threshold) {
            return "null";
        }

        return MoreObjects.toStringHelper(threshold)
                .add("promoKey", threshold.getPromoKey())
                .add("requiredPerks", threshold.getRequiredPerks())
                .add("remainingMultiCartTotal", threshold.getRemainingMultiCartTotal())
                .add("minMultiCartTotal", threshold.getMinMultiCartTotal())
                .add("amount", threshold.getAmount())
                .toString();
    }
}
