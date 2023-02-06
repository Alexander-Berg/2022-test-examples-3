package ru.yandex.market.api.matchers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.user.order.cashback.CashbackOption;
import ru.yandex.market.api.user.order.cashback.CashbackPermission;
import ru.yandex.market.api.user.order.cashback.CashbackRestrictionReason;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackPromoResponse;
import ru.yandex.market.checkout.checkouter.cashback.model.CashbackThreshold;

public class CashbackOptionMatcher {

    public static Matcher<CashbackOption> cashbackOption(Matcher<CashbackOption>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<CashbackOption> type(CashbackPermission value) {
        return ApiMatchers.map(
                CashbackOption::getType,
                "'type'",
                Matchers.is(value),
                CashbackOptionMatcher::toStr
        );
    }

    public static Matcher<CashbackOption> amount(BigDecimal value) {
        return ApiMatchers.map(
                CashbackOption::getAmount,
                "'amount'",
                Matchers.is(value),
                CashbackOptionMatcher::toStr
        );
    }

    public static Matcher<CashbackOption> amountByPromoKey(Matcher<Map<String, BigDecimal>> matcher) {
        return ApiMatchers.map(
                CashbackOption::getAmountByPromoKey,
                "'amountByPromoKey'",
                matcher,
                CashbackOptionMatcher::toStr
        );
    }

    public static Matcher<CashbackOption> amountByPromoKey(Matcher<Map<String, BigDecimal>>... matchers) {
        return ApiMatchers.map(
                CashbackOption::getAmountByPromoKey,
                "'amountByPromoKey'",
                Matchers.allOf(matchers),
                CashbackOptionMatcher::toStr
        );
    }

    public static Matcher<CashbackOption> promos(Matcher<List<CashbackPromoResponse>> matcher) {
        return ApiMatchers.map(
                CashbackOption::getPromos,
                "'promos'",
                matcher,
                CashbackOptionMatcher::toStr
        );
    }

    public static Matcher<CashbackOption> thresholds(Matcher<List<CashbackThreshold>> matcher) {
        return ApiMatchers.map(
                CashbackOption::getThresholds,
                "'thresholds'",
                matcher,
                CashbackOptionMatcher::toStr
        );
    }

    public static Matcher<CashbackOption> restrictionReason(CashbackRestrictionReason value) {
        return ApiMatchers.map(
                CashbackOption::getRestrictionReason,
                "'restrictionReason'",
                Matchers.is(value),
                CashbackOptionMatcher::toStr
        );
    }

    public static Matcher<CashbackOption> version(Integer value) {
        return ApiMatchers.map(
                CashbackOption::getVersion,
                "'version'",
                Matchers.is(value),
                CashbackOptionMatcher::toStr
        );
    }

    public static Matcher<CashbackOption> promoKey(String value) {
        return ApiMatchers.map(
                CashbackOption::getPromoKey,
                "'promoKey'",
                Matchers.is(value),
                CashbackOptionMatcher::toStr
        );
    }

    public static Matcher<CashbackOption> uiPromoFlags(Matcher<Iterable<String>> matcher) {
        return ApiMatchers.map(
                CashbackOption::getUiPromoFlags,
                "'uiPromoFlags'",
                matcher,
                CashbackOptionMatcher::toStr
        );
    }

    public static String toStr(CashbackOption option) {
        if (null == option) {
            return "null";
        }

        return MoreObjects.toStringHelper(option)
                .add("type", option.getType())
                .add("amount", option.getAmount())
                .add("restrictionReason", option.getRestrictionReason())
                .add("version", option.getVersion())
                .add("promoKey", option.getPromoKey())
                .toString();
    }
}
