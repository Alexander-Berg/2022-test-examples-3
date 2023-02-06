package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.option.Statistic;

import static org.hamcrest.Matchers.is;

public class StatisticMatcher {
    public static Matcher<Statistic> statistic(Matcher<Statistic>... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<Statistic> count(int count) {
        return ApiMatchers.map(
            Statistic::getCount,
            "'count'",
            is(count),
            StatisticMatcher::toStr
        );
    }

    public static Matcher<Statistic> modelCount(int count) {
        return ApiMatchers.map(
            Statistic::getModelCount,
            "'modelCount'",
            is(count),
            StatisticMatcher::toStr
        );
    }

    public static Matcher<Statistic> offerCount(int count) {
        return ApiMatchers.map(
            Statistic::getOfferCount,
            "'offerCount'",
            is(count),
            StatisticMatcher::toStr
        );
    }

    public static Matcher<Statistic> cpaCount(int count) {
        return ApiMatchers.map(
            Statistic::getCpaCount,
            "'cpaCount'",
            is(count),
            StatisticMatcher::toStr
        );
    }

    private static String toStr(Statistic statistic) {
        if (null == statistic) {
            return "null";
        }
        return MoreObjects.toStringHelper(Statistic.class)
            .add("count", statistic.getCount())
            .add("modelCount", statistic.getModelCount())
            .add("offerCount", statistic.getOfferCount())
            .add("cpaCount", statistic.getCpaCount())
            .toString();
    }
}
