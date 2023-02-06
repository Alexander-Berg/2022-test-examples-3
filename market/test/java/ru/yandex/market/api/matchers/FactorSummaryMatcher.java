package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.opinion.fact.FactStat;
import ru.yandex.market.api.domain.v2.opinion.fact.FactsSummary;

public class FactorSummaryMatcher {
    public static Matcher<FactsSummary> summary(Matcher<FactsSummary> ... summary) {
        return Matchers.allOf(summary);
    }

    public static Matcher<FactsSummary> recommendedRatio(Matcher<Double> matcher) {
        return ApiMatchers.map(
            FactsSummary::getRecommendedRatio,
            "'rate'",
            matcher,
            FactorSummaryMatcher::toStr
        );
    }


    public static Matcher<FactsSummary> facts(Matcher<Iterable<FactStat>> matcher) {
        return ApiMatchers.map(
            FactsSummary::getFacts,
            "'facts'",
            matcher,
            FactorSummaryMatcher::toStr
        );
    }

    public static String toStr(FactsSummary summary) {
        if (null == summary) {
            return "null";
        }
        return MoreObjects.toStringHelper(FactsSummary.class)
            .add("recommendedRatio", summary.getRecommendedRatio())
            .add("facts", ApiMatchers.collectionToStr(summary.getFacts(), FactorStatMatcher::toStr))
            .toString();
    }

}
