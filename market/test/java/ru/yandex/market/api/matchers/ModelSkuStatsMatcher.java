package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.domain.v2.ModelSkuStats;
import ru.yandex.market.api.sku.SkuService;

public class ModelSkuStatsMatcher {
    public static Matcher<ModelSkuStats> modelSkuStats(Matcher<ModelSkuStats> ... matchers) {
        return Matchers.allOf(matchers);
    }

    public static Matcher<ModelSkuStats> beforeFilters(int beforeFitlers) {
        return ApiMatchers.map(
            ModelSkuStats::getBeforeFilters,
            "'before'",
            Matchers.is(beforeFitlers),
            ModelSkuStatsMatcher::toStr
        );
    }

    public static Matcher<ModelSkuStats> afterFilters(int afterFilters) {
        return ApiMatchers.map(
            ModelSkuStats::getAfterFilters,
            "'after'",
            Matchers.is(afterFilters),
            ModelSkuStatsMatcher::toStr
        );
    }

    public static String toStr(ModelSkuStats stats) {
        if (null == stats) {
            return "null";
        }
        return MoreObjects.toStringHelper(ModelSkuStats.class)
            .add("beforeFilters", stats.getBeforeFilters())
            .add("afterFilters", stats.getAfterFilters())
            .toString();
    }
}
