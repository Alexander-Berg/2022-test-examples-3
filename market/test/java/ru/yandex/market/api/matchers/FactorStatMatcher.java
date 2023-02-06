package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.opinion.fact.FactStat;

public class FactorStatMatcher {
    public static Matcher<FactStat> facts(Matcher<FactStat> ... facts) {
        return Matchers.allOf(facts);
    }

    public static Matcher<FactStat> id(int id) {
        return ApiMatchers.map(
            FactStat::getId,
            "'id'",
            Matchers.is(id),
            FactorStatMatcher::toStr
        );
    }

    public static Matcher<FactStat> title(String title) {
        return ApiMatchers.map(
            FactStat::getTitle,
            "'title'",
            Matchers.is(title),
            FactorStatMatcher::toStr
        );
    }
    public static Matcher<FactStat> value(double value) {
        return ApiMatchers.map(
            FactStat::getValue,
            "'value'",
            Matchers.is(value),
            FactorStatMatcher::toStr
        );
    }

    public static Matcher<FactStat> count(long count) {
        return ApiMatchers.map(
            FactStat::getCount,
            "'count'",
            Matchers.is(count),
            FactorStatMatcher::toStr
        );
    }

    public static String toStr(FactStat factor) {
        if (null == factor) {
            return "null";
        }
        return MoreObjects.toStringHelper(FactStat.class)
            .add("id", factor.getId())
            .add("title", factor.getTitle())
            .add("count", factor.getCount())
            .add("value", factor.getValue())
            .toString();
    }
}
